package com.lanxx.gamebooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class FloatingService extends Service {
    private static final String CHANNEL = "lanxx_overlay";
    private WindowManager wm;
    private View bubble, panel;
    private boolean expanded = false;
    private int red = Color.rgb(255, 48, 79);
    private Handler handler = new Handler();
    private long timerStart = 0;
    private TextView timerText;
    private Runnable timerRunnable;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(21, buildNotification());
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "LanxX Floating Window",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Kontrol floating window LanxX Game Booster");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_manage)
                .setContentTitle("LanxX Game Booster aktif")
                .setContentText("Floating window siap digunakan")
                .setContentIntent(pi).setOngoing(true).build();
    }

    private WindowManager.LayoutParams params(int w, int h, int gravity) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                w, h, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = gravity;
        p.x = 24;
        p.y = 180;
        return p;
    }

    private void showBubble() {
        if (bubble != null) return;
        TextView b = new TextView(this);
        b.setText("LX");
        b.setTextColor(Color.WHITE);
        b.setTextSize(16);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(makeBubbleBackground());
        bubble = b;
        WindowManager.LayoutParams p = params(dp(58), dp(58), Gravity.TOP | Gravity.START);
        b.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY;
            float downX, downY;
            boolean moved;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = p.x; startY = p.y;
                    downX = e.getRawX(); downY = e.getRawY();
                    moved = false; return true;
                } else if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    int dx = (int)(e.getRawX() - downX);
                    int dy = (int)(e.getRawY() - downY);
                    if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) moved = true;
                    p.x = startX + dx; p.y = startY + dy;
                    try { wm.updateViewLayout(bubble, p); } catch (Exception ignored) {}
                    return true;
                } else if (e.getAction() == MotionEvent.ACTION_UP) {
                    if (!moved) togglePanel();
                    return true;
                }
                return true;
            }
        });
        try { wm.addView(bubble, p); } catch (Exception e) { stopSelf(); }
    }

    private android.graphics.drawable.GradientDrawable makeBubbleBackground() {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        d.setColor(0xFF17131A);
        d.setStroke(dp(3), red);
        return d;
    }

    private void togglePanel() {
        if (expanded) hidePanel(); else showPanel();
    }

    private void showPanel() {
        if (panel != null) return;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.setBackground(roundBg(0xF21A1720, red, dp(18)));
        TextView title = new TextView(this);
        title.setText("LANXX  /  GAME TOOLS");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(title);

        TextView hint = new TextView(this);
        hint.setText("QUICK LAUNCH");
        hint.setTextColor(red);
        hint.setTextSize(10);
        hint.setPadding(0, dp(10), 0, dp(4));
        box.addView(hint);

        JSONArray apps = savedApps();
        if (apps.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Belum ada shortcut. Tambahkan dari aplikasi LanxX.");
            empty.setTextColor(0xFFD0CBD3);
            empty.setTextSize(11);
            box.addView(empty);
        } else {
            for (int i = 0; i < apps.length(); i++) {
                JSONObject app = apps.optJSONObject(i);
                if (app == null) continue;
                String pkg = app.optString("package");
                String label = app.optString("label", pkg);
                Button launch = actionButton("▶  " + label);
                launch.setOnClickListener(v -> launchApp(pkg, label));
                box.addView(launch);
            }
        }

        Button timer = actionButton("⏱  MULAI / RESET TIMER");
        timer.setOnClickListener(v -> toggleTimer());
        box.addView(timer);
        timerText = new TextView(this);
        timerText.setText("Timer: 00:00");
        timerText.setTextColor(Color.WHITE);
        timerText.setTextSize(12);
        timerText.setPadding(0, dp(5), 0, dp(5));
        box.addView(timerText);

        Button settings = actionButton("⚙  PENGATURAN GAME");
        settings.setOnClickListener(v -> {
            try {
                Intent i = new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception ignored) {}
        });
        box.addView(settings);

        Button close = actionButton("TUTUP PANEL");
        close.setOnClickListener(v -> hidePanel());
        box.addView(close);

        panel = box;
        WindowManager.LayoutParams p = params(dp(250), -2, Gravity.TOP | Gravity.START);
        p.x = dp(16); p.y = dp(110);
        try { wm.addView(panel, p); expanded = true; }
        catch (Exception e) { panel = null; }
    }

    private void toggleTimer() {
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
            timerRunnable = null;
            timerStart = 0;
            if (timerText != null) timerText.setText("Timer: 00:00");
            return;
        }
        timerStart = SystemClock.elapsedRealtime();
        timerRunnable = new Runnable() {
            @Override public void run() {
                long sec = (SystemClock.elapsedRealtime() - timerStart) / 1000;
                if (timerText != null) timerText.setText(String.format(java.util.Locale.US,
                        "Timer: %02d:%02d", sec / 60, sec % 60));
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(timerRunnable);
    }

    private void launchApp(String pkg, String label) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i == null) {
                Toast.makeText(this, "Aplikasi tidak bisa dibuka", Toast.LENGTH_SHORT).show();
                return;
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            hidePanel();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal membuka " + label, Toast.LENGTH_SHORT).show();
        }
    }

    private JSONArray savedApps() {
        try {
            return new JSONArray(getSharedPreferences("lanxx_booster", MODE_PRIVATE)
                    .getString("favorite_apps", "[]"));
        } catch (Exception e) { return new JSONArray(); }
    }

    private Button actionButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF33212B));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(42));
        p.topMargin = dp(5);
        b.setLayoutParams(p);
        return b;
    }

    private android.graphics.drawable.GradientDrawable roundBg(int color, int stroke, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void hidePanel() {
        if (panel != null) {
            try { wm.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
        expanded = false;
        if (timerRunnable != null) {
            handler.removeCallbacks(timerRunnable);
            timerRunnable = null;
            timerStart = 0;
        }
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onDestroy() {
        hidePanel();
        if (bubble != null) {
            try { wm.removeView(bubble); } catch (Exception ignored) {}
            bubble = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
                    }
                         
