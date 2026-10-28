package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

/**
 * Callers: AndroidManifest receiver, MainActivity.armWakeRepair.
 * AlarmManager ELAPSED_REALTIME, no files. ServiceRepair stores the helper name only.
 * User: "why this problem keeps happening why cannot you fix it permanently and everytime
 * in all situation and evenwhen tv shuts and open and at all time pressing home must open cyber."
 */
public final class HomeWatch extends BroadcastReceiver {

    static final String TICK = "com.gathin.tvhome.HOME_WATCH";
    private static final long EVERY_MS = 15_000L;
    private static boolean screenArmed;

    @Override
    public void onReceive(Context context, Intent intent) {
        arm(context);
        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
            context.startActivity(new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    static void arm(Context context) {
        Context app = context.getApplicationContext();
        ServiceRepair.ensure(app);
        if (!screenArmed) {
            screenArmed = true;
            app.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    ServiceRepair.ensure(c);
                    schedule(c);
                }
            }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }
        schedule(app);
    }

    private static void schedule(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        Intent tick = new Intent(context, HomeWatch.class).setAction(TICK);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, tick,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long when = SystemClock.elapsedRealtime() + EVERY_MS;
        try {
            alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME, when, pending);
        } catch (SecurityException e) {
            alarms.set(AlarmManager.ELAPSED_REALTIME, when, pending);
        }
    }
}
