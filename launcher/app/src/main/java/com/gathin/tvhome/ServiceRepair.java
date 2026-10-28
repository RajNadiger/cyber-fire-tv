package com.gathin.tvhome;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Android takes an app's accessibility services off the enabled list when the app is force-stopped (Settings, a
 * cleaner app, or am force-stop), and the remote's Home, Input and app buttons then stop working until the helper
 * is turned on again over ADB. With restore_key_services on, a running helper records itself here and drops the
 * record when Android unbinds it (the user turned it off); a force stop kills the process before that, so the
 * record stays. Home starting again puts recorded helpers back on the list, when the installer granted
 * WRITE_SECURE_SETTINGS. With the flag off nothing is recorded and nothing is restored.
 */
final class ServiceRepair {
    private static final String PREFS = "accessibility_helpers";
    private static final AtomicBoolean repairing = new AtomicBoolean();

    private ServiceRepair() {
    }

    static void connected(Context context, Class<?> service) {
        prefs(context).edit().putBoolean(service.getName(), true).apply();
    }

    static void disconnected(Context context, Class<?> service) {
        prefs(context).edit().remove(service.getName()).apply();
    }

    /**
     * Fire OS drops this helper when the stick sleeps. Callers: MainActivity.onResume,
     * the screen-on receiver, and GuardKeep.ensureHomeKey. Prefs key is the service class name,
     * value true. No dates.
     */
    static void ensure(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) return;
        Config config = Config.load(context);
        if (!config.restoreKeyServices && config.stockHomeRedirect.isEmpty()) return;
        prefs(context).edit().putBoolean(HomeKeyService.class.getName(), true).commit();
        restore(context);
    }

    static void restore(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) return;
        try {
            ContentResolver cr = context.getContentResolver();
            String list = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            List<ComponentName> enabled = new ArrayList<>();
            if (list != null) {
                for (String entry : list.split(":")) {
                    ComponentName name = ComponentName.unflattenFromString(entry);
                    if (name != null) enabled.add(name);
                }
            }
            List<ComponentName> recorded = new ArrayList<>();
            for (String service : prefs(context).getAll().keySet()) {
                recorded.add(new ComponentName(context.getPackageName(), service));
            }
            if (recorded.isEmpty()) return;
            // The list without our recorded helpers, then with all of them.
            StringBuilder others = new StringBuilder();
            for (ComponentName name : enabled) {
                if (recorded.contains(name)) continue;
                if (others.length() > 0) others.append(':');
                others.append(name.flattenToShortString());
            }
            StringBuilder updated = new StringBuilder(others);
            boolean missing = false;
            for (ComponentName name : recorded) {
                if (!enabled.contains(name)) missing = true;
                if (updated.length() > 0) updated.append(':');
                updated.append(name.flattenToShortString());
            }
            if (!missing) return;
            final String without = others.toString();
            final String with = updated.toString();
            if (!repairing.compareAndSet(false, true)) return;
            final String seen = list == null ? "" : list;
            new Thread(() -> {
                try {
                    // On immediately. The remove-and-add below is only for a helper Android has marked crashed.
                    if (!replaceList(cr, seen, with)) return;
                    Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                    Thread.sleep(700);
                    String now = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                    if (now != null && now.contains("HomeKeyService")) return;
                    if (!replaceList(cr, with, without)) return;
                    Thread.sleep(700);
                    if (!replaceList(cr, without, with)) return;
                    Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
                } catch (RuntimeException | InterruptedException ignored) {
                    // The setting is the system's; a refusal leaves the helper off, as before.
                } finally {
                    repairing.set(false);
                }
            }, "ServiceRepair").start();
        } catch (RuntimeException ignored) {
            // The setting is the system's; a refusal leaves the helper off, as before.
        }
    }

    /** Writes the enabled list only while it still reads {@code expected}; an absent list counts as empty. */
    private static boolean replaceList(ContentResolver cr, String expected, String updated) {
        String now = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return Objects.equals(now == null ? "" : now, expected == null ? "" : expected)
                && Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
