package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
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
 * Shows a computer that is sharing its screen on this Wi-Fi.
 * The computer runs tools/second-screen.py and this screen lists it.
 */
public class SecondScreenActivity extends Activity {

    private static final int PORT = 47865;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, String> hosts = new LinkedHashMap<>();
    private LinearLayout list;
    private TextView status;
    private VideoView video;
    private volatile boolean running = true;
    private Thread listen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF05060C);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(48);
        page.setPadding(pad, dp(36), pad, dp(36));

        TextView title = text(28, 0xFFFFFFFF, true);
        title.setText(R.string.second_screen);
        page.addView(title);

        status = text(16, 0xD6E8E8ED, false);
        status.setText("On the same Wi-Fi, run second-screen.py on your Mac, Linux, or Windows computer.");
        status.setPadding(0, dp(8), 0, dp(18));
        page.addView(status);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        page.addView(list);
        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        video = new VideoView(this);
        video.setVisibility(View.GONE);
        root.addView(video, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        listen = new Thread(this::listenForComputers, "second-screen");
        listen.setDaemon(true);
        listen.start();
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
            main.post(() -> status.setText("Could not listen for a computer on this Wi-Fi."));
        }
    }

    private void addHost(String name, String url) {
        if (hosts.containsKey(url) || video.getVisibility() == View.VISIBLE) return;
        hosts.put(url, name);
        status.setText("Choose a computer.");
        TextView row = text(20, 0xFFFFFFFF, true);
        row.setText(name);
        row.setFocusable(true);
        row.setClickable(true);
        row.setBackgroundColor(0xFF1C212B);
        int p = dp(16);
        row.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(520), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(10);
        row.setOnClickListener(v -> play(url));
        list.addView(row, lp);
        if (list.getChildCount() == 1) row.requestFocus();
    }

    private void play(String url) {
        list.setVisibility(View.GONE);
        status.setVisibility(View.GONE);
        video.setVisibility(View.VISIBLE);
        video.setVideoURI(Uri.parse(url));
        video.setOnPreparedListener(mp -> video.start());
        video.setOnErrorListener((mp, what, extra) -> {
            video.setVisibility(View.GONE);
            status.setVisibility(View.VISIBLE);
            status.setText("The picture did not start. Leave second-screen.py running and try again.");
            list.setVisibility(View.VISIBLE);
            return true;
        });
        video.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (video.getVisibility() == View.VISIBLE) {
            video.stopPlayback();
            video.setVisibility(View.GONE);
            status.setVisibility(View.VISIBLE);
            list.setVisibility(View.VISIBLE);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        running = false;
        if (listen != null) listen.interrupt();
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
