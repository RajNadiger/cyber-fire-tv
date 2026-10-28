package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small live weather chip under the top icon row. Focus opens the day's range. Press opens the hours. */
final class WeatherBoard {

    private static final String PREFS = "weather_board";
    private static final long FRESH_MS = 20 * 60 * 1000L;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService FETCH = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "weather");
        t.setDaemon(true);
        return t;
    });

    private static View chip;
    private static Glyph glyph;
    private static TextView tempView;
    private static TextView lineView;
    private static TextView detailView;
    private static FrameLayout panel;
    private static Snapshot shown;
    private static boolean busy;
    private static Runnable downToApps;

    private WeatherBoard() {}

    static void attach(Activity activity, LinearLayout slot, Runnable onDownToApps) {
        if (chip != null || slot == null) return;
        downToApps = onDownToApps;
        float d = activity.getResources().getDisplayMetrics().density;
        int padH = Math.round(10 * d);
        int padV = Math.round(4 * d);

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(padH, padV, padH, padV);
        row.setFocusable(true);
        row.setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x9912161E);
        bg.setCornerRadius(14 * d);
        bg.setStroke(Math.round(d), 0x22FFFFFF);
        row.setBackground(bg);

        glyph = new Glyph(activity);
        row.addView(glyph, new LinearLayout.LayoutParams(Math.round(16 * d), Math.round(16 * d)));

        LinearLayout words = new LinearLayout(activity);
        words.setOrientation(LinearLayout.HORIZONTAL);
        words.setGravity(Gravity.CENTER_VERTICAL);
        words.setPadding(Math.round(6 * d), 0, 0, 0);
        tempView = text(activity, 13, 0xFFFFFFFF, true);
        lineView = text(activity, 12, 0xD6E8E8ED, false);
        detailView = text(activity, 12, 0xB3E8E8ED, false);
        lineView.setVisibility(View.GONE);
        detailView.setVisibility(View.GONE);
        words.addView(tempView);
        words.addView(lineView);
        words.addView(detailView);
        row.addView(words);
        tempView.setText("—°");
        lineView.setText("Weather");

        slot.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        chip = row;
        View buttons = activity.findViewById(R.id.topButtons);
        View.OnLayoutChangeListener place = (v, l, t, r, b, ol, ot, or, ob) -> placeUnderTabs(activity, slot, buttons);
        buttons.addOnLayoutChangeListener(place);
        slot.addOnLayoutChangeListener(place);
        slot.post(() -> placeUnderTabs(activity, slot, buttons));
        row.setOnFocusChangeListener((v, focused) -> {
            v.animate().scaleX(focused ? 1.05f : 1f).scaleY(focused ? 1.05f : 1f).setDuration(180).start();
            bg.setStroke(Math.round(d), focused ? 0xEEFFFFFF : 0x33FFFFFF);
            if (shown == null) return;
            if (focused) {
                lineView.setVisibility(View.VISIBLE);
                lineView.setText("  " + phrase(shown.code));
                detailView.setVisibility(View.VISIBLE);
                detailView.setText("   " + shown.high + "° / " + shown.low + "°");
                lineView.animate().alpha(1f).setDuration(180).start();
                detailView.animate().alpha(1f).setDuration(180).start();
            } else {
                lineView.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                    if (!row.hasFocus()) lineView.setVisibility(View.GONE);
                }).start();
                detailView.animate().alpha(0f).setDuration(120).withEndAction(() -> {
                    if (!row.hasFocus()) detailView.setVisibility(View.GONE);
                }).start();
            }
        });
        row.setOnClickListener(v -> open(activity));
        row.setOnKeyListener((v, code, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_DOWN && downToApps != null) {
                downToApps.run();
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_UP) {
                View settings = activity.findViewById(R.id.btnSettings);
                if (settings != null) settings.requestFocus();
                return true;
            }
            return false;
        });

        Snapshot cached = read(activity);
        if (cached != null) apply(cached);
        refresh(activity, cached == null || System.currentTimeMillis() - cached.fetched > FRESH_MS);
        if (glyph != null) glyph.start();
    }

    private static void placeUnderTabs(Activity activity, View slot, View buttons) {
        if (slot.getHeight() == 0 || buttons.getHeight() == 0) return;
        View root = activity.findViewById(R.id.root);
        int[] buttonAt = new int[2];
        int[] rootAt = new int[2];
        buttons.getLocationOnScreen(buttonAt);
        root.getLocationOnScreen(rootAt);
        float gap = 8f * activity.getResources().getDisplayMetrics().density;
        slot.setY(buttonAt[1] - rootAt[1] + buttons.getHeight() + gap);
    }

    static boolean focusChip() {
        if (chip == null || chip.getVisibility() != View.VISIBLE) return false;
        chip.requestFocus();
        return true;
    }

    static void resume(Activity activity) {
        if (chip == null) return;
        if (glyph != null) glyph.start();
        Snapshot cached = shown != null ? shown : read(activity);
        refresh(activity, cached == null || System.currentTimeMillis() - cached.fetched > FRESH_MS);
    }

    static void pause() {
        if (glyph != null) glyph.stop();
        close();
    }

    private static void refresh(Activity activity, boolean force) {
        if (busy) return;
        if (!force && shown != null && System.currentTimeMillis() - shown.fetched < FRESH_MS) return;
        busy = true;
        FETCH.execute(() -> {
            Snapshot next = load();
            MAIN.post(() -> {
                busy = false;
                if (activity.isDestroyed()) return;
                if (next != null) {
                    write(activity, next);
                    apply(next);
                }
            });
        });
    }

    private static void apply(Snapshot s) {
        shown = s;
        tempView.setText(s.temp + "°");
        lineView.setText(phrase(s.code));
        detailView.setText("High " + s.high + "°   ·   Low " + s.low + "°");
        chip.setContentDescription(phrase(s.code) + ", " + s.temp + " degrees");
        if (glyph != null) glyph.setCode(s.code);
    }

    private static void open(Activity activity) {
        if (panel != null || shown == null) return;
        ViewGroup root = activity.findViewById(R.id.root);
        if (root == null) return;
        float d = activity.getResources().getDisplayMetrics().density;
        FrameLayout scrim = new FrameLayout(activity);
        scrim.setBackgroundColor(0xD005060C);
        scrim.setAlpha(0f);
        scrim.setFocusable(true);

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(28 * d);
        card.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF0141822);
        bg.setCornerRadius(28 * d);
        bg.setStroke(Math.round(d), 0x66FFFFFF);
        card.setBackground(bg);

        Glyph big = new Glyph(activity);
        big.setCode(shown.code);
        card.addView(big, new LinearLayout.LayoutParams(Math.round(72 * d), Math.round(72 * d)));
        TextView now = text(activity, 52, 0xFFFFFFFF, true);
        now.setText(shown.temp + "°");
        card.addView(now);
        TextView name = text(activity, 18, 0xFFF5F5F7, false);
        name.setText(phrase(shown.code));
        card.addView(name);
        TextView range = text(activity, 15, 0xC7E8E8ED, false);
        range.setText("High " + shown.high + "°     Low " + shown.low + "°");
        range.setPadding(0, Math.round(8 * d), 0, Math.round(20 * d));
        card.addView(range);

        LinearLayout hours = new LinearLayout(activity);
        hours.setOrientation(LinearLayout.HORIZONTAL);
        int count = Math.min(6, shown.hourTemps.length);
        for (int i = 0; i < count; i++) {
            LinearLayout col = new LinearLayout(activity);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_HORIZONTAL);
            col.setPadding(Math.round(12 * d), 0, Math.round(12 * d), 0);
            TextView hour = text(activity, 12, 0x99E8E8ED, false);
            hour.setText(shown.hourLabels[i]);
            TextView value = text(activity, 16, 0xFFFFFFFF, true);
            value.setText(shown.hourTemps[i] + "°");
            col.addView(hour);
            col.addView(value);
            col.setAlpha(0f);
            col.setTranslationY(12 * d);
            hours.addView(col);
            col.animate().alpha(1f).translationY(0f).setStartDelay(70L + i * 55L).setDuration(420).start();
        }
        card.addView(hours);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                Math.round(560 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.gravity = Gravity.CENTER;
        scrim.addView(card, cardLp);
        card.setScaleX(0.94f);
        card.setScaleY(0.94f);
        root.addView(scrim, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        panel = scrim;
        big.start();
        scrim.animate().alpha(1f).setDuration(200).start();
        card.animate().scaleX(1f).scaleY(1f).setDuration(340).start();
        scrim.requestFocus();
        scrim.setOnKeyListener((v, code, event) -> {
            if (event.getAction() == KeyEvent.ACTION_UP && (code == KeyEvent.KEYCODE_BACK
                    || code == KeyEvent.KEYCODE_ESCAPE || code == KeyEvent.KEYCODE_DPAD_CENTER
                    || code == KeyEvent.KEYCODE_ENTER)) {
                close();
                return true;
            }
            return true;
        });
    }

    private static void close() {
        if (panel == null) return;
        FrameLayout gone = panel;
        panel = null;
        gone.animate().alpha(0f).setDuration(160).withEndAction(() -> {
            ViewGroup parent = (ViewGroup) gone.getParent();
            if (parent != null) parent.removeView(gone);
            if (chip != null) chip.requestFocus();
        }).start();
    }

    private static TextView text(Context context, int sp, int color, boolean medium) {
        TextView view = new TextView(context);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setIncludeFontPadding(false);
        Typeface face = ResourcesCompat.getFont(context, medium ? R.font.sora_medium : R.font.sora_regular);
        if (face != null) view.setTypeface(face);
        return view;
    }

    private static Snapshot load() {
        try {
            JSONObject where = new JSONObject(http("https://ipwho.is/"));
            double lat = where.getDouble("latitude");
            double lon = where.getDouble("longitude");
            String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                    + "&longitude=" + lon
                    + "&current=temperature_2m,weather_code"
                    + "&hourly=temperature_2m"
                    + "&daily=temperature_2m_max,temperature_2m_min"
                    + "&forecast_hours=8&timezone=auto";
            JSONObject body = new JSONObject(http(url));
            JSONObject current = body.getJSONObject("current");
            JSONObject daily = body.getJSONObject("daily");
            JSONObject hourly = body.getJSONObject("hourly");
            JSONArray times = hourly.getJSONArray("time");
            JSONArray temps = hourly.getJSONArray("temperature_2m");
            String now = current.optString("time");
            int start = 0;
            for (int i = 0; i < times.length(); i++) {
                if (times.getString(i).compareTo(now) >= 0) {
                    start = i;
                    break;
                }
            }
            int n = Math.max(0, Math.min(6, temps.length() - start));
            int[] hourTemps = new int[n];
            String[] hourLabels = new String[n];
            for (int i = 0; i < n; i++) {
                hourTemps[i] = (int) Math.round(temps.getDouble(start + i));
                String stamp = times.getString(start + i);
                hourLabels[i] = stamp.length() >= 16 ? stamp.substring(11, 16) : stamp;
            }
            Snapshot s = new Snapshot();
            s.temp = (int) Math.round(current.getDouble("temperature_2m"));
            s.code = current.getInt("weather_code");
            s.high = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(0));
            s.low = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(0));
            s.hourTemps = hourTemps;
            s.hourLabels = hourLabels;
            s.fetched = System.currentTimeMillis();
            return s;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String http(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("Accept", "application/json");
        try (InputStream in = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String row;
            while ((row = reader.readLine()) != null) text.append(row);
            return text.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String phrase(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 71 && code <= 77) return "Snow";
        if (code >= 95) return "Storm";
        if (code >= 51) return "Rain";
        return "Cloudy";
    }

    private static void write(Context context, Snapshot s) {
        try {
            JSONObject json = new JSONObject();
            json.put("temp", s.temp);
            json.put("code", s.code);
            json.put("high", s.high);
            json.put("low", s.low);
            json.put("fetched", s.fetched);
            JSONArray temps = new JSONArray();
            JSONArray labels = new JSONArray();
            for (int i = 0; i < s.hourTemps.length; i++) {
                temps.put(s.hourTemps[i]);
                labels.put(s.hourLabels[i]);
            }
            json.put("hourTemps", temps);
            json.put("hourLabels", labels);
            prefs(context).edit().putString("snap", json.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static Snapshot read(Context context) {
        String raw = prefs(context).getString("snap", "");
        if (raw.isEmpty()) return null;
        try {
            JSONObject json = new JSONObject(raw);
            Snapshot s = new Snapshot();
            s.temp = json.getInt("temp");
            s.code = json.getInt("code");
            s.high = json.getInt("high");
            s.low = json.getInt("low");
            s.fetched = json.getLong("fetched");
            JSONArray temps = json.getJSONArray("hourTemps");
            JSONArray labels = json.getJSONArray("hourLabels");
            s.hourTemps = new int[temps.length()];
            s.hourLabels = new String[labels.length()];
            for (int i = 0; i < temps.length(); i++) {
                s.hourTemps[i] = temps.getInt(i);
                s.hourLabels[i] = labels.getString(i);
            }
            return s;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static final class Snapshot {
        int temp;
        int code;
        int high;
        int low;
        int[] hourTemps = new int[0];
        String[] hourLabels = new String[0];
        long fetched;
    }

    /** Sun, cloud, rain, snow, or storm. The mark keeps a slow motion while it is on screen. */
    static final class Glyph extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int code;
        private float phase;
        private android.animation.ValueAnimator spin;

        Glyph(Context context) {
            super(context);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }

        void setCode(int code) {
            this.code = code;
            invalidate();
        }

        void start() {
            if (spin != null) return;
            spin = android.animation.ValueAnimator.ofFloat(0f, 1f);
            spin.setDuration(2800);
            spin.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            spin.addUpdateListener(a -> {
                phase = (float) a.getAnimatedValue();
                invalidate();
            });
            spin.start();
        }

        void stop() {
            if (spin != null) spin.cancel();
            spin = null;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            float u = Math.min(w, h);
            if (u <= 0f) return;
            paint.setStrokeWidth(Math.max(2f, u * 0.07f));
            paint.setColor(0xFFF5F5F7);
            paint.setAlpha(255);
            float cx = w / 2f;
            float cy = h / 2f;
            if (code >= 95) {
                cloud(canvas, cx, cy - u * 0.05f, u * 0.32f);
                paint.setStyle(Paint.Style.FILL);
                Path bolt = new Path();
                bolt.moveTo(cx, cy);
                bolt.lineTo(cx - u * 0.08f, cy + u * 0.16f);
                bolt.lineTo(cx + u * 0.02f, cy + u * 0.16f);
                bolt.lineTo(cx - u * 0.05f, cy + u * 0.34f);
                canvas.drawPath(bolt, paint);
                paint.setStyle(Paint.Style.STROKE);
                return;
            }
            if ((code >= 51 && code < 71) || (code >= 80 && code < 95)) {
                cloud(canvas, cx, cy - u * 0.08f, u * 0.3f);
                for (int i = 0; i < 3; i++) {
                    float drop = (phase + i * 0.22f) % 1f;
                    float x = cx - u * 0.12f + i * u * 0.12f;
                    canvas.drawLine(x, cy + u * 0.06f + drop * u * 0.2f, x - u * 0.03f,
                            cy + u * 0.18f + drop * u * 0.2f, paint);
                }
                return;
            }
            if (code >= 71 && code <= 77) {
                cloud(canvas, cx, cy - u * 0.08f, u * 0.3f);
                paint.setStyle(Paint.Style.FILL);
                for (int i = 0; i < 3; i++) {
                    float drop = (phase + i * 0.25f) % 1f;
                    canvas.drawCircle(cx - u * 0.12f + i * u * 0.12f, cy + u * 0.1f + drop * u * 0.18f, u * 0.04f, paint);
                }
                paint.setStyle(Paint.Style.STROKE);
                return;
            }
            if (code >= 2) {
                cloud(canvas, cx, cy, u * 0.34f);
                return;
            }
            float r = u * 0.16f;
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.save();
            canvas.rotate(phase * 36f, cx, cy);
            for (int i = 0; i < 8; i++) {
                double a = Math.toRadians(i * 45.0);
                canvas.drawLine(
                        cx + (float) Math.cos(a) * (r + u * 0.08f),
                        cy + (float) Math.sin(a) * (r + u * 0.08f),
                        cx + (float) Math.cos(a) * (r + u * 0.22f),
                        cy + (float) Math.sin(a) * (r + u * 0.22f),
                        paint);
            }
            canvas.restore();
        }

        private void cloud(Canvas canvas, float cx, float cy, float r) {
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx - r * 0.45f, cy + r * 0.1f, r * 0.55f, paint);
            canvas.drawCircle(cx + r * 0.15f, cy - r * 0.15f, r * 0.62f, paint);
            canvas.drawCircle(cx + r * 0.55f, cy + r * 0.12f, r * 0.46f, paint);
            paint.setStyle(Paint.Style.STROKE);
        }
    }
}
