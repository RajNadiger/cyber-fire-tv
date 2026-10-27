package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Typeface;

import java.io.File;

/** A stable open animation for one app. Every title uses the same face. */
final class AppMotion {
    private static Typeface title;
    private static boolean titleResolved;

    private AppMotion() {}

    static int style(String pkg) {
        int hash = pkg == null ? 0 : pkg.hashCode();
        int mod = hash % 4;
        return mod < 0 ? mod + 4 : mod;
    }

    /** A clean technical sans when hero.ttf is on the TV, otherwise the plain system face. */
    static Typeface typeface(Context context, String pkg) {
        if (titleResolved) return title;
        titleResolved = true;
        File dir = context.getExternalFilesDir(null);
        File file = dir == null ? null : new File(dir, "hero.ttf");
        if (file != null && file.isFile()) {
            try {
                title = Typeface.createFromFile(file);
            } catch (RuntimeException ignored) {
                title = null;
            }
        }
        if (title == null) title = UiFont.get(context, R.string.font_medium);
        return title;
    }

    static float tracking(String pkg) {
        return 0.04f;
    }

    /** {enter, exit} for the window animation when this app opens. */
    static int[] open(String pkg) {
        switch (style(pkg)) {
            case 1: return new int[]{R.anim.app_open_rise_enter, R.anim.app_open_rise_exit};
            case 2: return new int[]{R.anim.app_open_zoom_enter, R.anim.app_open_zoom_exit};
            case 3: return new int[]{R.anim.app_open_slide_enter, R.anim.app_open_slide_exit};
            default: return new int[]{R.anim.app_open_enter, R.anim.app_open_exit};
        }
    }
}
