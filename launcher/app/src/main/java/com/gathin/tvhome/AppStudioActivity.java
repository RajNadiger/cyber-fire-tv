package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caller: LauncherSettingsActivity opens this. It writes AppLook and bumps Config.SETTINGS_REVISION_KEY.
 * Layout: activity_app_studio.xml. No network API.
 * User asked to change any app's icon, name and colour from launcher settings.
 */
public final class AppStudioActivity extends Activity {

    private static final int PICK_IMAGE = 7;
    private static final int[] COLOURS = {
            0, 0xFFFFFFFF, 0xFFE50914, 0xFFFF0033, 0xFF1DB954, 0xFF3D5AFE, 0xFFFFB300, 0xFFB388FF
    };

    private final List<String> packages = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<TextView> rows = new ArrayList<>();
    private String selected;
    private boolean reduced;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_app_studio);
        reduced = Config.load(this).reducedMotion;
        View stage = findViewById(R.id.stage);
        float density = getResources().getDisplayMetrics().density;
        stage.setCameraDistance(12000f * density);
        stage.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 18f * density);
            }
        });
        stage.setClipToOutline(true);
        loadApps();
        findViewById(R.id.renameButton).setOnClickListener(v -> rename());
        findViewById(R.id.pictureButton).setOnClickListener(v -> picture());
        findViewById(R.id.resetButton).setOnClickListener(v -> {
            if (selected == null) return;
            AppLook.clear(this, selected);
            saved();
            show(selected, true);
        });
        LinearLayout colors = findViewById(R.id.colorRow);
        for (int color : COLOURS) {
            View chip = new View(this);
            int size = Math.round(36 * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.rightMargin = Math.round(8 * density);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(color == 0 ? 0xFF2A3140 : color);
            if (color == 0) dot.setStroke(Math.round(2 * density), 0xFFB9E7C7);
            chip.setBackground(dot);
            chip.setFocusable(true);
            chip.setContentDescription(color == 0
                    ? getString(R.string.your_apps_own_colour) : getString(R.string.your_apps_colour));
            final int chosen = color;
            chip.setOnClickListener(v -> {
                if (selected == null) return;
                AppLook.setColor(this, selected, chosen);
                saved();
                show(selected, true);
            });
            colors.addView(chip, lp);
        }
        if (!rows.isEmpty()) rows.get(0).requestFocus();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(tv, 0)) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] one : apps) seen |= one[1].equals(pkg);
            if (seen) continue;
            apps.add(new String[]{info.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        LinearLayout list = findViewById(R.id.appList);
        float density = getResources().getDisplayMetrics().density;
        for (String[] app : apps) {
            packages.add(app[1]);
            labels.add(app[0]);
            TextView row = new TextView(this);
            row.setText(displayName(app[1], app[0]));
            row.setTextColor(0xFFFFFFFF);
            row.setTextSize(18f);
            row.setFocusable(true);
            row.setClickable(true);
            row.setMinHeight(Math.round(48 * density));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(Math.round(16 * density), 0, Math.round(16 * density), 0);
            row.setBackgroundResource(R.drawable.settings_choice_bg);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = Math.round(6 * density);
            final String pkg = app[1];
            row.setOnFocusChangeListener((v, focused) -> {
                if (focused) show(pkg, false);
            });
            list.addView(row, lp);
            rows.add(row);
        }
    }

    private String displayName(String pkg, String original) {
        AppLook look = AppLook.get(this, pkg);
        return look.name != null ? look.name : original;
    }

    private void show(String pkg, boolean turn) {
        selected = pkg;
        int index = packages.indexOf(pkg);
        if (index < 0) return;
        for (int i = 0; i < rows.size(); i++) rows.get(i).setSelected(i == index);
        AppLook look = AppLook.get(this, pkg);
        String name = look.name != null ? look.name : labels.get(index);
        rows.get(index).setText(name);
        Runnable apply = () -> {
            TextView title = findViewById(R.id.stageName);
            title.setText(name);
            title.setTextColor(look.color != 0 ? look.color : 0xFFFFFFFF);
            title.setTypeface(HomeFont.typeface(this));
            ImageView art = findViewById(R.id.stageArt);
            Drawable custom = AppLook.icon(this, pkg);
            if (custom != null) {
                art.setImageDrawable(custom);
            } else {
                PackageManager pm = getPackageManager();
                Drawable banner = null;
                try {
                    banner = pm.getApplicationBanner(pkg);
                } catch (PackageManager.NameNotFoundException ignored) {
                    banner = null;
                }
                if (banner == null) {
                    try {
                        banner = pm.getApplicationIcon(pkg);
                    } catch (PackageManager.NameNotFoundException ignored) {
                        banner = null;
                    }
                }
                art.setImageDrawable(banner);
            }
        };
        View stage = findViewById(R.id.stage);
        if (!turn || reduced) {
            apply.run();
            return;
        }
        stage.animate().cancel();
        stage.animate().rotationY(12f).alpha(0.35f).setDuration(100).withEndAction(() -> {
            if (isDestroyed()) return;
            apply.run();
            stage.animate().rotationY(0f).alpha(1f)
                    .setDuration(Motion.FAST_MS).setInterpolator(Motion.SMOOTH).start();
        }).start();
    }

    private void rename() {
        if (selected == null) return;
        int index = packages.indexOf(selected);
        AppLook look = AppLook.get(this, selected);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(look.name != null ? look.name : labels.get(index));
        input.setSelection(input.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.your_apps_rename)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String typed = input.getText().toString().trim();
                    AppLook.setName(this, selected, typed.equals(labels.get(index)) ? null : typed);
                    saved();
                    show(selected, true);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void picture() {
        if (selected == null) return;
        List<File> files = iconSources();
        if (files.isEmpty()) {
            openPicker();
            return;
        }
        String[] names = new String[files.size() + 1];
        for (int i = 0; i < files.size(); i++) names[i] = files.get(i).getName();
        names[files.size()] = getString(R.string.your_apps_pick);
        new AlertDialog.Builder(this)
                .setTitle(R.string.your_apps_picture)
                .setItems(names, (dialog, which) -> {
                    if (which == files.size()) openPicker();
                    else saveFile(files.get(which));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private List<File> iconSources() {
        List<File> files = new ArrayList<>();
        addImages(files, new File(getExternalFilesDir(null), "icons"));
        File dir = Config.dir(this);
        if (dir != null) addImages(files, new File(dir, Config.USER_WALLPAPERS));
        return files;
    }

    private static void addImages(List<File> into, File folder) {
        File[] found = folder.isDirectory() ? folder.listFiles() : null;
        if (found == null) return;
        for (File file : found) {
            String name = file.getName().toLowerCase();
            if (file.isFile() && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
                    || name.endsWith(".webp"))) into.add(file);
        }
    }

    private void openPicker() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.setType("image/*");
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(pick, getString(R.string.your_apps_picture)), PICK_IMAGE);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, R.string.your_apps_no_picker, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_IMAGE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            if (bitmap != null && AppLook.saveIcon(this, selected, bitmap)) {
                saved();
                show(selected, true);
            }
        } catch (java.io.IOException e) {
            Toast.makeText(this, R.string.your_apps_no_picker, Toast.LENGTH_LONG).show();
        }
    }

    private void saveFile(File file) {
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap != null && AppLook.saveIcon(this, selected, bitmap)) {
            saved();
            show(selected, true);
        }
    }

    private void saved() {
        android.content.SharedPreferences choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
        Toast.makeText(this, R.string.your_apps_saved, Toast.LENGTH_SHORT).show();
    }
}
