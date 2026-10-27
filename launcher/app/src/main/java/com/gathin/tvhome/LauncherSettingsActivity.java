package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView fontChoice;
    private static final String[] LOOK_KEYS = {
            "ambient_glow", "app_glow", "dock_title", "dock_glass", "cinematic_home",
            "focus_overshoot", "reduced_motion", "fade_rows", "show_greeting",
            "header_white", "icon_only_buttons", "always_show_names"};
    private static final int[] LOOK_IDS = {
            R.id.lookColourWash, R.id.lookTileGlow, R.id.lookBigName, R.id.lookGlassDock, R.id.lookShelf,
            R.id.lookSpring, R.id.lookCalm, R.id.lookFade, R.id.lookGreeting, R.id.lookBrightClock,
            R.id.lookRoundButtons, R.id.lookAllNames};
    private static final int[] LOOK_LABELS = {
            R.string.look_colour_wash, R.string.look_tile_glow, R.string.look_big_name, R.string.look_glass_dock,
            R.string.look_shelf, R.string.look_spring, R.string.look_calm, R.string.look_fade, R.string.look_greeting,
            R.string.look_bright_clock, R.string.look_round_buttons, R.string.look_all_names};
    private static final int[] LOOK_HINTS = {
            R.string.hint_colour_wash, R.string.hint_tile_glow, R.string.hint_big_name, R.string.hint_glass_dock,
            R.string.hint_shelf, R.string.hint_spring, R.string.hint_calm, R.string.hint_fade, R.string.hint_greeting,
            R.string.hint_bright_clock, R.string.hint_round_buttons, R.string.hint_all_names};
    private final TextView[] lookRows = new TextView[LOOK_KEYS.length];
    private TextView glassStyleChoice;
    private TextView glowStyleChoice;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperMode());
        fontChoice = findViewById(R.id.fontChoice);
        fontChoice.setOnClickListener(v -> showFontPicker());
        for (int i = 0; i < LOOK_KEYS.length; i++) {
            final String key = LOOK_KEYS[i];
            lookRows[i] = findViewById(LOOK_IDS[i]);
            lookRows[i].setOnClickListener(v -> {
                boolean now = lookOn(Config.load(this), key);
                choices.edit().putBoolean(key, !now).apply();
                changed();
                refresh();
            });
        }
        glassStyleChoice = findViewById(R.id.lookGlassStyle);
        glassStyleChoice.setOnClickListener(v -> showStylePicker("glass_style", R.string.look_glass_style,
                new String[]{"glass", "rim", "halo"},
                new int[]{R.string.glass_glass, R.string.glass_rim, R.string.glass_halo}));
        glowStyleChoice = findViewById(R.id.lookGlowStyle);
        glowStyleChoice.setOnClickListener(v -> showStylePicker("glow_style", R.string.look_glow_style,
                new String[]{"radial", "horizon"},
                new int[]{R.string.glow_radial, R.string.glow_horizon}));
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY).remove(Config.WALLPAPER_MODE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY).remove(HomeFont.KEY)
                    .remove("glass_style").remove("glow_style").commit();
            for (String key : LOOK_KEYS) choices.edit().remove(key).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperMode() {
        String[] items = {
                getString(R.string.wallpaper_auto),
                getString(R.string.wallpaper_choose),
                getString(R.string.wallpaper_theme_default)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        File dir = Config.dir(this);
                        if (dir != null) new File(dir, Config.USER_WALLPAPERS).mkdirs();
                        choices.edit().putString(Config.WALLPAPER_MODE_KEY, Config.WALLPAPER_MODE_AUTO)
                                .remove(Config.WALLPAPER_CHOICE_KEY).apply();
                        changed();
                        Toast.makeText(this, R.string.wallpaper_auto_help, Toast.LENGTH_LONG).show();
                        refresh();
                    } else if (which == 1) {
                        choices.edit().remove(Config.WALLPAPER_MODE_KEY).apply();
                        showWallpaperPicker();
                    } else {
                        choices.edit().remove(Config.WALLPAPER_MODE_KEY)
                                .remove(Config.WALLPAPER_CHOICE_KEY).apply();
                        changed();
                        refresh();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>();
        if (dir != null) {
            images.addAll(WallpaperRotation.images(new File(dir, Config.USER_WALLPAPERS)));
            images.addAll(WallpaperRotation.images(new File(dir, "wallpapers")));
        }
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().remove(Config.WALLPAPER_MODE_KEY)
                                .putString(Config.WALLPAPER_CHOICE_KEY, images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).remove(Config.WALLPAPER_MODE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean anyLookChoice() {
        for (String key : LOOK_KEYS) if (choices.contains(key)) return true;
        return false;
    }

    private static boolean lookOn(Config config, String key) {
        switch (key) {
            case "ambient_glow": return config.ambientGlow;
            case "app_glow": return config.appGlow;
            case "dock_title": return config.dockTitle;
            case "dock_glass": return config.dockGlass;
            case "cinematic_home": return config.cinematicHome;
            case "focus_overshoot": return config.focusOvershoot;
            case "reduced_motion": return config.reducedMotion;
            case "fade_rows": return config.fadeRows;
            case "show_greeting": return config.showGreeting;
            case "header_white": return config.headerWhite;
            case "icon_only_buttons": return config.iconOnlyButtons;
            default: return config.alwaysShowNames;
        }
    }

    private void showStylePicker(String key, int title, String[] ids, int[] labels) {
        String current = choices.getString(key, "");
        String[] names = new String[ids.length];
        int checked = 0;
        for (int i = 0; i < ids.length; i++) {
            names[i] = getString(labels[i]);
            if (ids[i].equals(current)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(names, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putString(key, ids[which]).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String glassLabel(String style) {
        if ("rim".equals(style)) return getString(R.string.glass_rim);
        if ("halo".equals(style)) return getString(R.string.glass_halo);
        return getString(R.string.glass_glass);
    }

    private String glowLabel(String style) {
        return getString("horizon".equals(style) ? R.string.glow_horizon : R.string.glow_radial);
    }

    private void showFontPicker() {
        String[] ids = HomeFont.ids();
        String[] labels = new String[ids.length];
        String current = HomeFont.current(this);
        int checked = 0;
        for (int i = 0; i < ids.length; i++) {
            labels[i] = getString(HomeFont.label(ids[i]));
            if (ids[i].equals(current)) checked = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.font_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    choices.edit().putString(HomeFont.KEY, ids[which]).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void paintPreview(Config look) {
        TextView greeting = findViewById(R.id.previewGreeting);
        TextView name = findViewById(R.id.previewName);
        View band = findViewById(R.id.previewBand);
        LinearLayout dock = findViewById(R.id.previewDock);
        LinearLayout grid = findViewById(R.id.previewGrid);
        greeting.setVisibility(look.showGreeting ? View.VISIBLE : View.GONE);
        greeting.setAlpha(look.headerWhite ? 1f : 0.45f);
        name.setVisibility(look.dockTitle ? View.VISIBLE : View.GONE);
        name.setTypeface(HomeFont.typeface(this));
        name.setTextColor(look.ambientGlow ? 0xFFE50914 : Color.WHITE);
        if (band.getBackground() != null) band.getBackground().mutate().setAlpha(look.dockOpacity * 255 / 100);
        fillChips(dock, columns, Color.WHITE, 28);
        grid.setVisibility(rowsBelowDock > 0 ? View.VISIBLE : View.GONE);
        fillChips(grid, columns, 0x66FFFFFF, 16);
    }

    private void fillChips(LinearLayout row, int count, int color, int heightDp) {
        row.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int gap = Math.round(6 * density);
        int height = Math.round(heightDp * density);
        for (int i = 0; i < count; i++) {
            View chip = new View(this);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(8 * density);
            bg.setColor(color);
            chip.setBackground(bg);
            chip.setFocusable(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, 1f);
            if (i > 0) lp.leftMargin = gap;
            row.addView(chip, lp);
        }
    }

    private void refresh() {
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        String wallpaperLabel = Config.WALLPAPER_MODE_AUTO.equals(choices.getString(Config.WALLPAPER_MODE_KEY, null))
                ? getString(R.string.wallpaper_auto)
                : "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                : wallpaperCurrent != null ? wallpaperCurrent
                : getString(R.string.wallpaper_theme_default);
        wallpaperChoice.setText(getString(R.string.wallpaper_state, wallpaperLabel));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        fontChoice.setText(getString(R.string.font_state, getString(HomeFont.label(HomeFont.current(this)))));
        Config look = Config.load(this);
        for (int i = 0; i < lookRows.length; i++) {
            boolean on = lookOn(look, LOOK_KEYS[i]);
            lookRows[i].setText(getString(LOOK_LABELS[i], getString(on ? R.string.on : R.string.off))
                    + "\n" + getString(LOOK_HINTS[i]));
        }
        paintPreview(look);
        glassStyleChoice.setText(getString(R.string.look_glass_style, glassLabel(look.glassStyle)));
        glowStyleChoice.setText(getString(R.string.look_glow_style, glowLabel(look.glowStyle)));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.WALLPAPER_MODE_KEY)
                || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains(HomeFont.KEY)
                || choices.contains("glass_style") || choices.contains("glow_style") || anyLookChoice();
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
