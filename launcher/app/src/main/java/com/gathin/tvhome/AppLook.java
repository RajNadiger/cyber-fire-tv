package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Per-app name, picture and colour.
 * Callers: AppAdapter (tile build) and AppStudioActivity (edit).
 * Preferences file app_look, one string per package: name + unit separator + colour int.
 * Picture: files/app-icons/&lt;package&gt;.png, longest side 480px.
 * User: "the setting is easy and do anything and has all freedom to do anything with launcher
 * even from changing the icons of any app with the image with their will the name of the app
 * and even colour fonts and everything freedom."
 */
final class AppLook {

    private static final String PREFS = "app_look";
    private static final char SEP = '\u001f';

    final String name;
    /** 0 means the app keeps the colour sampled from its artwork. */
    final int color;

    private AppLook(String name, int color) {
        this.name = name;
        this.color = color;
    }

    static AppLook get(Context context, String pkg) {
        String raw = prefs(context).getString(pkg, null);
        if (raw == null) return new AppLook(null, 0);
        int cut = raw.indexOf(SEP);
        if (cut < 0) return new AppLook(null, 0);
        String name = raw.substring(0, cut);
        int color = 0;
        try {
            color = Integer.parseInt(raw.substring(cut + 1));
        } catch (NumberFormatException ignored) {
            color = 0;
        }
        return new AppLook(name.isEmpty() ? null : name, color);
    }

    static void setName(Context context, String pkg, String name) {
        AppLook current = get(context, pkg);
        write(context, pkg, name, current.color);
    }

    static void setColor(Context context, String pkg, int color) {
        AppLook current = get(context, pkg);
        write(context, pkg, current.name, color);
    }

    static void clear(Context context, String pkg) {
        prefs(context).edit().remove(pkg).apply();
        File file = iconFile(context, pkg);
        if (file.isFile()) file.delete();
    }

    static Drawable icon(Context context, String pkg) {
        File file = iconFile(context, pkg);
        if (!file.isFile()) return null;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) return null;
        return new BitmapDrawable(context.getResources(), bitmap);
    }

    /** Shrinks the picture so a photo cannot sit in memory at full size. */
    static boolean saveIcon(Context context, String pkg, Bitmap source) {
        if (source == null) return false;
        int longest = Math.max(source.getWidth(), source.getHeight());
        Bitmap scaled = source;
        if (longest > 480) {
            float ratio = 480f / longest;
            scaled = Bitmap.createScaledBitmap(source,
                    Math.max(1, Math.round(source.getWidth() * ratio)),
                    Math.max(1, Math.round(source.getHeight() * ratio)), true);
        }
        File dir = new File(context.getFilesDir(), "app-icons");
        if (!dir.isDirectory() && !dir.mkdirs()) return false;
        try (FileOutputStream out = new FileOutputStream(iconFile(context, pkg))) {
            return scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (java.io.IOException e) {
            return false;
        } finally {
            if (scaled != source) scaled.recycle();
            if (!source.isRecycled()) source.recycle();
        }
    }

    static File iconFile(Context context, String pkg) {
        return new File(new File(context.getFilesDir(), "app-icons"), pkg.replace('/', '_') + ".png");
    }

    private static void write(Context context, String pkg, String name, int color) {
        String kept = name == null ? "" : name.replace(SEP, ' ').trim();
        if (kept.isEmpty() && color == 0 && !iconFile(context, pkg).isFile()) {
            prefs(context).edit().remove(pkg).apply();
            return;
        }
        prefs(context).edit().putString(pkg, kept + SEP + color).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
