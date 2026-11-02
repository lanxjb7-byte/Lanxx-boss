package com.lanxx.gamebooster;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS = "lanxx_booster";
    private static final String APPS = "favorite_apps";
    private LinearLayout appList;
    private TextView overlayStatus, appCount;
    private int red = Color.rgb(255, 48, 79);
    private int bg = Color.rgb(9, 9, 13);
    private int panel = Color.rgb(25, 22, 30);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        requestNotificationPermission();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(bg);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(28));
        scroll.addView(root);

        TextView brand = text("LANXX", 30, Color.WHITE, true);
        brand.setLetterSpacing(0.14f);
        root.addView(brand);
        TextView sub = text("GAME BOOSTER  /  CONTROL CENTER", 11, red, true);
        sub.setPadding(0, dp(2), 0, dp(20));
        root.addView(sub);

        LinearLayout hero = card();
        TextView heroTitle = text("GAMING CONTROL", 18, Color.WHITE, true);
        hero.addView(heroTitle);
        TextView heroDesc = text("Akses shortcut aplikasi melalui jendela mengambang saat bermain.", 13, 0xFFBDB7C2, false);
        heroDesc.setPadding(0, dp(8), 0, dp(16));
        hero.addView(heroDesc);

        overlayStatus = text("FLOATING WINDOW: NONAKTIF", 12, red, true);
        hero.addView(overlayStatus);
        Button start = button("AKTIFKAN FLOATING WINDOW");
        start.setOnClickListener(v -> enableOverlay());
        hero.addView(start);
        Button stop = button("MATIKAN FLOATING WINDOW");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            updateStatus();
            Toast.makeText(this, "Floating window dimatikan", Toast.LENGTH_SHORT).show();
        });
        hero.addView(stop);
        root.addView(hero, cardParams());

        LinearLayout appsCard = card();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setOrientation(LinearLayout.HORIZONTAL);
        TextView appsTitle = text("APP SHORTCUTS", 17, Color.WHITE, true);
        head.addView(appsTitle, new LinearLayout.LayoutParams(0, -2, 1));
        appCount = text("0 APP", 11, red, true);
        head.addView(appCount);
        appsCard.addView(head);

        TextView info = text("Pilih aplikasi yang ingin ditampilkan di floating window.", 12, 0xFFBDB7C2, false);
        info.setPadding(0, dp(7), 0, dp(12));
        appsCard.addView(info);

        Button add = button("+ TAMBAH APLIKASI");
        add.setOnClickListener(v -> showAppPicker());
        appsCard.addView(add);
        appList = new LinearLayout(this);
        appList.setOrientation(LinearLayout.VERTICAL);
        appsCard.addView(appList);
        root.addView(appsCard, cardParams());

        LinearLayout tools = card();
        tools.addView(text("GAMING TOOLS", 17, Color.WHITE, true));
        addTool(tools, "QUICK LAUNCH", "Buka game atau aplikasi favorit dari panel mengambang.");
        addTool(tools, "GAME TIMER", "Timer sederhana tersedia langsung di floating panel.");
        addTool(tools, "RAM & STORAGE", "Lihat ringkasan penggunaan memori perangkat.");
        addTool(tools, "QUICK SETTINGS", "Akses pengaturan Android yang relevan dengan game.");
        root.addView(tools, cardParams());

        TextView foot = text("LanxX • Utility tools for your gaming session", 10, 0xFF77717D, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, dp(18), 0, 0);
        root.addView(foot);

        setContentView(scroll);
        refreshApps();
        updateStatus();
    }

    private void enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
            Toast.makeText(this, "Izinkan tampil di atas aplikasi lain, lalu kembali ke LanxX.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent service = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
        else startService(service);
        updateStatus();
        Toast.makeText(this, "Floating window aktif", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (appList != null) {
            refreshApps();
            updateStatus();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 77);
        }
    }

    private void showAppPicker() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> found = pm.queryIntentActivities(query, 0);
        Collections.sort(found, (a, b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        List<android.content.pm.ResolveInfo> filtered = new ArrayList<>();
        for (android.content.pm.ResolveInfo r : found) {
            if (r.activityInfo == null || getPackageName().equals(r.activityInfo.packageName)) continue;
            filtered.add(r);
        }
        String[] labels = new String[filtered.size()];
        for (int i = 0; i < filtered.size(); i++) labels[i] = String.valueOf(filtered.get(i).loadLabel(pm));
        new AlertDialog.Builder(this)
                .setTitle("Pilih aplikasi")
                .setItems(labels, (dialog, which) -> {
                    android.content.pm.ResolveInfo r = filtered.get(which);
                    addFavorite(r.activityInfo.packageName, String.valueOf(r.loadLabel(pm)));
                })
                .setNegativeButton("BATAL", null)
                .show();
    }

    private void addFavorite(String pkg, String label) {
        JSONArray arr = getApps();
        try {
            for (int i = 0; i < arr.length(); i++) {
                if (pkg.equals(arr.getJSONObject(i).optString("package"))) {
                    Toast.makeText(this, "Aplikasi sudah ada", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            JSONObject item = new JSONObject();
            item.put("package", pkg);
            item.put("label", label);
            arr.put(item);
            getPreferences().edit().putString(APPS, arr.toString()).apply();
            refreshApps();
            Toast.makeText(this, label + " ditambahkan", Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) { }
    }

    private void removeFavorite(String pkg) {
        JSONArray old = getApps();
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject o = old.optJSONObject(i);
            if (o != null && !pkg.equals(o.optString("package"))) next.put(o);
        }
        getPreferences().edit().putString(APPS, next.toString()).apply();
        refreshApps();
    }

    private void refreshApps() {
        if (appList == null) return;
        appList.removeAllViews();
        JSONArray arr = getApps();
        appCount.setText(arr.length() + " APP");
        if (arr.length() == 0) {
            TextView empty = text("Belum ada aplikasi. Tekan TAMBAH APLIKASI.", 12, 0xFF918B96, false);
            empty.setPadding(0, dp(12), 0, dp(8));
            appList.addView(empty);
        }
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String pkg = o.optString("package");
            String label = o.optString("label", pkg);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(9), dp(6), dp(9));
            row.setBackgroundColor(0xFF17141B);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = dp(6);
            appList.addView(row, rp);
            TextView name = text(label, 13, Color.WHITE, true);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button remove = smallButton("HAPUS");
            remove.setOnClickListener(v -> removeFavorite(pkg));
            row.addView(remove);
        }
    }

    private JSONArray getApps() {
        try { return new JSONArray(getPreferences().getString(APPS, "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private android.content.SharedPreferences getPreferences() {
        return getSharedPreferences(PREFS, MODE_PRIVATE);
    }

    private void updateStatus() {
        if (overlayStatus == null) return;
        boolean allowed = Settings.canDrawOverlays(this);
        overlayStatus.setText(allowed ? "FLOATING WINDOW: IZIN DIBERIKAN" : "FLOATING WINDOW: IZIN BELUM ADA");
        overlayStatus.setTextColor(allowed ? 0xFF65E6A5 : red);
    }

    private void addTool(LinearLayout parent, String title, String desc) {
        TextView t = text(title, 13, red, true);
        t.setPadding(0, dp(14), 0, dp(3));
        parent.addView(t);
        parent.addView(text(desc, 12, 0xFFBDB7C2, false));
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(16), dp(16), dp(16));
        l.setBackgroundColor(panel);
        return l;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(14);
        return p;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(red));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(48));
        p.topMargin = dp(8);
        b.setLayoutParams(p);
        return b;
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(10);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF6B2636));
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
          }
      
