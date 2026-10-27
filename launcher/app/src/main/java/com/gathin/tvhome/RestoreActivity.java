package com.gathin.tvhome;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Puts the launcher back to the build from before the custom changes, and clears its settings. */
public final class RestoreActivity extends Activity {
    private static final String ORIGINAL = "cleanyourtv-original.apk";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_restore);
        findViewById(R.id.restoreGo).setOnClickListener(v -> restore());
        findViewById(R.id.restoreCancel).setOnClickListener(v -> finish());
        findViewById(R.id.restoreGo).requestFocus();
    }

    private void restore() {
        View go = findViewById(R.id.restoreGo);
        go.setEnabled(false);
        clearSettings();
        File dir = getExternalFilesDir(null);
        File apk = dir == null ? null : new File(dir, "original.apk");
        if (apk == null || !apk.isFile()) {
            apk = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ORIGINAL);
        }
        if (!apk.isFile()) {
            Toast.makeText(this, R.string.restore_missing, Toast.LENGTH_LONG).show();
            go.setEnabled(true);
            return;
        }
        try {
            install(apk);
            clearLauncherFiles(apk);
        } catch (IOException e) {
            Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_LONG).show();
            go.setEnabled(true);
        }
    }

    private void clearSettings() {
        String[] names = {Config.SETTINGS_NAME, "wallpaper_rotation"};
        for (String name : names) {
            SharedPreferences prefs = getSharedPreferences(name, MODE_PRIVATE);
            prefs.edit().clear().commit();
        }
    }

    private void clearLauncherFiles(File keep) {
        File dir = getExternalFilesDir(null);
        if (dir == null) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (keep != null && child.getAbsolutePath().equals(keep.getAbsolutePath())) continue;
            deleteTree(child);
        }
    }

    private static void deleteTree(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        file.delete();
    }

    private void install(File apk) throws IOException {
        PackageInstaller installer = getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = installer.createSession(params);
        PackageInstaller.Session session = installer.openSession(sessionId);
        try (InputStream in = new FileInputStream(apk);
             OutputStream out = session.openWrite("base.apk", 0, apk.length())) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
            session.fsync(out);
        }
        Intent done = new Intent(this, RestoreActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, sessionId, done,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        session.commit(pending.getIntentSender());
        session.close();
        ((TextView) findViewById(R.id.restoreBody)).setText(R.string.restore_confirm_install);
    }
}
