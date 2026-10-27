package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.widget.TextView;

/** The typeface chosen in Launcher settings. Outfit is the built-in face; the others are system faces. */
final class HomeFont {
    static final String KEY = "font";
    private static final String[] IDS = {"outfit", "condensed", "mono", "serif"};

    private HomeFont() {}

    static String current(Context context) {
        String saved = prefs(context).getString(KEY, "outfit");
        for (String id : IDS) if (id.equals(saved)) return id;
        return "outfit";
    }

    static String[] ids() {
        return IDS.clone();
    }

    static int label(String id) {
        switch (id) {
            case "condensed": return R.string.font_condensed;
            case "mono": return R.string.font_mono;
            case "serif": return R.string.font_serif;
            default: return R.string.font_outfit;
        }
    }

    static Typeface typeface(Context context) {
        switch (current(context)) {
            case "condensed": return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            case "mono": return Typeface.create("monospace", Typeface.NORMAL);
            case "serif": return Typeface.create("serif", Typeface.NORMAL);
            default: return UiFont.get(context, R.string.font_medium);
        }
    }

    static void apply(TextView view) {
        if (view != null) view.setTypeface(typeface(view.getContext()));
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
    }
}
