package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.core.content.res.ResourcesCompat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guide and player for a computer sharing its screen.
 * Opened from the Second screen tile. The computer runs tools/second-screen.py
 * and announces "CYBER-SCREEN port name" on UDP 47865. No stored schema.
 * User asked for a visual guide: how to use it, what to do, and how it works.
 */
public class SecondScreenActivity extends Activity {

    private static final int PORT = 47865;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> hosts = new LinkedHashMap<>();
    private LinearLayout choices;
    private TextView stageTitle;
    private TextView stageBody;
    private View pulse;
    private View stage;
    private VideoView video;
    private View guide;
    private volatile boolean running = true;
    private Thread listen;
    private boolean reduced;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reduced = Config.load(this).reducedMotion;
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF05060C);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.HORIZONTAL);
        page.setPadding(dp(56), dp(36), dp(56), dp(32));
        page.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(32, 0xFFFFFFFF, true);
        title.setText(R.string.second_screen);
        steps.addView(title);
        TextView lead = text(16, 0xC7E8E8ED, false);
        lead.setText(R.string.second_lead);
        lead.setPadding(0, dp(6), 0, dp(22));
        steps.addView(lead);
        steps.addView(step("1", R.string.second_step_1, R.string.second_step_1_body, null));
        steps.addView(step("2", R.string.second_step_2, R.string.second_step_2_body, getString(R.string.second_command)));
        steps.addView(step("3", R.string.second_step_3, R.string.second_step_3_body, null));
        page.addView(steps, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.15f));

        stage = buildStage();
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(dp(420), dp(280));
        stageLp.leftMargin = dp(28);
        page.addView(stage, stageLp);

        guide = page;
        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        video = new VideoView(this);
        video.setVisibility(View.GONE);
        root.addView(video, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        if (!reduced) {
            stage.setCameraDistance(dp(4200));
            stage.setRotationY(-12f);
            stage.animate().rotationY(-7f).setDuration(Motion.NORMAL_MS).setInterpolator(Motion.SMOOTH).start();
            pulse.animate().alpha(0.25f).setDuration(900).setInterpolator(new LinearInterpolator())
                    .withEndAction(this::pulseAgain).start();
        }
        listen = new Thread(this::listenForComputers, "second-screen");
        listen.setDaemon(true);
        listen.start();
    }

    private void pulseAgain() {
        if (reduced || pulse == null || !hosts.isEmpty()) return;
        float next = pulse.getAlpha() < 0.5f ? 1f : 0.25f;
        pulse.animate().alpha(next).setDuration(900).setInterpolator(new LinearInterpolator())
                .withEndAction(this::pulseAgain).start();
    }

    private View buildStage() {
        FrameLayout frame = new FrameLayout(this);
        GradientDrawable shell = new GradientDrawable();
        shell.setColor(0xFF12161E);
        shell.setCornerRadius(dp(28));
        shell.setStroke(dp(1), 0x33FFFFFF);
        frame.setBackground(shell);
        frame.setPadding(dp(22), dp(22), dp(22), dp(22));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER_HORIZONTAL);

        pulse = new View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(0xFFFFFFFF);
        pulse.setBackground(dot);
        inner.addView(pulse, new LinearLayout.LayoutParams(dp(10), dp(10)));

        stageTitle = text(22, 0xFFFFFFFF, true);
        stageTitle.setGravity(Gravity.CENTER);
        stageTitle.setText(R.string.second_waiting);
        stageTitle.setPadding(0, dp(16), 0, dp(8));
        inner.addView(stageTitle);

        stageBody = text(14, 0xB3E8E8ED, false);
        stageBody.setGravity(Gravity.CENTER);
        stageBody.setText(R.string.second_step_2_body);
        inner.addView(stageBody);

        choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.VERTICAL);
        choices.setPadding(0, dp(18), 0, 0);
        inner.addView(choices);

        frame.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        return frame;
    }

    private View step(String number, int title, int body, String command) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, dp(24), dp(16));

        TextView badge = text(16, 0xFF05060C, true);
        badge.setGravity(Gravity.CENTER);
        badge.setText(number);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xFFFFFFFF);
        badge.setBackground(circle);
        row.addView(badge, new LinearLayout.LayoutParams(dp(32), dp(32)));

        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(14), 0, 0, 0);
        TextView head = text(18, 0xFFFFFFFF, true);
        head.setText(title);
        words.addView(head);
        TextView copy = text(14, 0xB3E8E8ED, false);
        copy.setText(body);
        copy.setPadding(0, dp(3), 0, 0);
        words.addView(copy);
        if (command != null) {
            TextView code = text(15, 0xFFFFFFFF, true);
            code.setText(command);
            code.setPadding(dp(12), dp(8), dp(12), dp(8));
            GradientDrawable chip = new GradientDrawable();
            chip.setColor(0xFF1C212B);
            chip.setCornerRadius(dp(10));
            code.setBackground(chip);
            LinearLayout.LayoutParams codeLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            codeLp.topMargin = dp(8);
            words.addView(code, codeLp);
        }
        row.addView(words);
        return row;
    }

    private void listenForComputers() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setSoTimeout(1000);
            byte[] buf = new byte[512];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                } catch (java.net.SocketTimeoutException again) {
                    continue;
                }
                String message = new String(packet.getData(), 0, packet.getLength()).trim();
                if (!message.startsWith("CYBER-SCREEN ")) continue;
                String[] parts = message.split(" ", 3);
                if (parts.length < 3) continue;
                String url = "http://" + packet.getAddress().getHostAddress() + ":" + parts[1] + "/index.m3u8";
                String name = parts[2];
                main.post(() -> addHost(name, url));
            }
        } catch (Exception ignored) {
            main.post(() -> {
                if (stageBody != null) stageBody.setText(R.string.second_listen_fail);
            });
        }
    }

    private void addHost(String name, String url) {
        if (hosts.containsKey(url) || video.getVisibility() == View.VISIBLE) return;
        boolean first = hosts.isEmpty();
        hosts.put(url, name);
        if (first) {
            pulse.animate().cancel();
            pulse.setAlpha(1f);
            stageTitle.setText(name);
            stageBody.setText(R.string.second_step_3_body);
            if (!reduced) {
                stage.animate().rotationY(0f).setDuration(Motion.NORMAL_MS).setInterpolator(Motion.SMOOTH).start();
            }
        }
        TextView row = text(18, 0xFF05060C, true);
        row.setText(getString(R.string.second_show) + "  ·  " + name);
        row.setGravity(Gravity.CENTER);
        row.setFocusable(true);
        row.setClickable(true);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        row.setBackground(bg);
        int p = dp(14);
        row.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        row.setOnClickListener(v -> play(url));
        choices.addView(row, lp);
        if (first) row.requestFocus();
    }

    private void play(String url) {
        guide.setVisibility(View.GONE);
        video.setVisibility(View.VISIBLE);
        video.setVideoURI(Uri.parse(url));
        video.setOnPreparedListener(mp -> video.start());
        video.setOnErrorListener((mp, what, extra) -> {
            video.setVisibility(View.GONE);
            guide.setVisibility(View.VISIBLE);
            stageBody.setText(R.string.second_fail);
            return true;
        });
        video.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (video.getVisibility() == View.VISIBLE) {
            video.stopPlayback();
            video.setVisibility(View.GONE);
            guide.setVisibility(View.VISIBLE);
            if (choices.getChildCount() > 0) choices.getChildAt(0).requestFocus();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (listen != null) listen.interrupt();
        if (pulse != null) pulse.animate().cancel();
        video.stopPlayback();
        super.onDestroy();
    }

    private TextView text(int sp, int color, boolean medium) {
        TextView view = new TextView(this);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setIncludeFontPadding(false);
        Typeface face = ResourcesCompat.getFont(this, medium ? R.font.sora_medium : R.font.sora_regular);
        if (face != null) view.setTypeface(face);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
