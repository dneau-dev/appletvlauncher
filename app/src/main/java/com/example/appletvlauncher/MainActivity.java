package com.example.appletvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextClock;
import android.widget.TextView;
import java.util.*;

public class MainActivity extends Activity {
    // Dock, left to right. Change these package names to change the dock.
    static final String[] DOCK = {"com.google.android.youtube.tv", "com.netflix.ninja",
            "com.spotify.tv.android", "com.amazon.amazonvideo.livingroom"};
    static final int COLS = 4;

    final Map<String, View> tiles = new HashMap<>();
    ScrollView scroll;
    View firstDock;
    TextView hi;
    SharedPreferences prefs;

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    GradientDrawable box(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    StateListDrawable glass(int r) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, box(0x55FFFFFF, r));
        s.addState(new int[]{}, box(0x00000000, r));
        return s;
    }

    TextView txt(TextView t, int sp, int color, String font) {
        t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(font, Typeface.NORMAL)); return t;
    }

    TextView pill(String label, View.OnClickListener c) {
        TextView t = txt(new TextView(this), 16, Color.WHITE, "sans-serif");
        t.setText(label); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(24), dp(10), dp(24), dp(10));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, box(0x66FFFFFF, 30));
        s.addState(new int[]{}, box(0x26FFFFFF, 30));
        t.setBackground(s); t.setFocusable(true); t.setOnClickListener(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(12); t.setLayoutParams(lp);
        return t;
    }

    View tile(ResolveInfo ri) {
        PackageManager pm = getPackageManager();
        final String pkg = ri.activityInfo.packageName;
        final Intent launch = new Intent(Intent.ACTION_MAIN)
                .setComponent(new ComponentName(pkg, ri.activityInfo.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL); t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(8), dp(8), dp(8), dp(8)); t.setBackground(glass(18));
        t.setFocusable(true);
        ImageView iv = new ImageView(this);
        Drawable d = ri.activityInfo.loadBanner(pm);
        if (d == null) d = ri.loadIcon(pm);
        iv.setImageDrawable(d); iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        t.addView(iv, new LinearLayout.LayoutParams(dp(176), dp(99)));
        final TextView l = txt(new TextView(this), 15, Color.WHITE, "sans-serif-light");
        l.setText(ri.loadLabel(pm)); l.setSingleLine(true); l.setGravity(Gravity.CENTER);
        l.setVisibility(View.INVISIBLE);
        t.addView(l, new LinearLayout.LayoutParams(dp(176), -2));
        t.setOnFocusChangeListener((v, f) -> {
            v.animate().scaleX(f ? 1.1f : 1f).scaleY(f ? 1.1f : 1f).setDuration(120).start();
            l.setVisibility(f ? View.VISIBLE : View.INVISIBLE);
            if (f) prefs.edit().putString("last", pkg).apply();
        });
        t.setOnClickListener(v -> { try { startActivity(launch); } catch (Exception e) { } });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6)); t.setLayoutParams(lp);
        tiles.put(pkg, t);
        return t;
    }

    void inputs() {
        TvInputManager m = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        final List<TvInputInfo> list = m.getTvInputList();
        String[] names = new String[list.size()];
        for (int i = 0; i < names.length; i++) names[i] = String.valueOf(list.get(i).loadLabel(this));
        new AlertDialog.Builder(this).setItems(names, (dlg, w) -> {
            TvInputInfo i = list.get(w);
            Intent in = new Intent(Intent.ACTION_VIEW, i.isPassthroughInput()
                    ? TvContract.buildChannelUriForPassthroughInput(i.getId())
                    : TvContract.Channels.CONTENT_URI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(in); } catch (Exception e) { }
        }).show();
    }

    // Deterministic D-pad: one row per press, ends of a row never fall through.
    void wire(List<List<View>> rows) {
        for (List<View> r : rows) for (View v : r) v.setId(View.generateViewId());
        for (int i = 0; i < rows.size(); i++) {
            List<View> r = rows.get(i);
            List<View> up = rows.get(Math.max(i - 1, 0)), dn = rows.get(Math.min(i + 1, rows.size() - 1));
            for (int j = 0; j < r.size(); j++) {
                View v = r.get(j);
                v.setNextFocusLeftId(r.get(Math.max(j - 1, 0)).getId());
                v.setNextFocusRightId(r.get(Math.min(j + 1, r.size() - 1)).getId());
                v.setNextFocusUpId(up.get(Math.min(j, up.size() - 1)).getId());
                v.setNextFocusDownId(dn.get(Math.min(j, dn.size() - 1)).getId());
            }
        }
    }

    String greeting() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 18 ? "Bonjour" : "Bonsoir";
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("p", 0);
        scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK); scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(56), dp(36), dp(56), dp(36));
        scroll.addView(col);
        List<List<View>> rows = new ArrayList<>();

        LinearLayout head = new LinearLayout(this);
        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        hi = txt(new TextView(this), 20, 0xAAFFFFFF, "sans-serif-light");
        TextClock clk = txt(new TextClock(this), 64, Color.WHITE, "sans-serif-thin");
        clk.setFormat24Hour("HH:mm"); clk.setFormat12Hour("HH:mm");
        TextClock date = txt(new TextClock(this), 20, 0xAAFFFFFF, "sans-serif-light");
        date.setFormat24Hour("EEEE d MMMM"); date.setFormat12Hour("EEEE d MMMM");
        left.addView(hi); left.addView(clk); left.addView(date);
        head.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        List<View> top = new ArrayList<>();
        top.add(pill("Sources", v -> inputs()));
        top.add(pill("Réglages", v -> startActivity(new Intent(Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));
        for (View v : top) head.addView(v);
        col.addView(head); rows.add(top);

        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> apps = new LinkedHashMap<>();
        for (String cat : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER})
            for (ResolveInfo ri : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat), 0))
                if (!apps.containsKey(ri.activityInfo.packageName)
                        && !ri.activityInfo.packageName.equals(getPackageName()))
                    apps.put(ri.activityInfo.packageName, ri);

        List<View> dockRow = new ArrayList<>();
        LinearLayout dock = new LinearLayout(this);
        dock.setBackground(box(0x26FFFFFF, 28)); dock.setPadding(dp(12), dp(12), dp(12), dp(12));
        for (String p : DOCK) {
            ResolveInfo ri = apps.remove(p);
            if (ri != null) { View t = tile(ri); dock.addView(t); dockRow.add(t); }
        }
        if (!dockRow.isEmpty()) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = dp(48); col.addView(dock, lp);
            rows.add(dockRow); firstDock = dockRow.get(0);
        }

        List<ResolveInfo> rest = new ArrayList<>(apps.values());
        final PackageManager fpm = pm;
        Collections.sort(rest, (x, y) -> String.valueOf(x.loadLabel(fpm))
                .compareToIgnoreCase(String.valueOf(y.loadLabel(fpm))));
        for (int i = 0; i < rest.size(); i += COLS) {
            LinearLayout row = new LinearLayout(this);
            List<View> r = new ArrayList<>();
            for (int j = i; j < Math.min(i + COLS, rest.size()); j++) {
                View t = tile(rest.get(j)); row.addView(t); r.add(t);
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = dp(i == 0 ? 36 : 6); lp.leftMargin = dp(12);
            col.addView(row, lp); rows.add(r);
        }
        wire(rows);
        setContentView(scroll);
    }

    @Override protected void onResume() {
        super.onResume();
        hi.setText(greeting());
        View v = tiles.get(prefs.getString("last", ""));
        if (v == null) v = firstDock;
        if (v != null) v.requestFocus();
    }

    @Override protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        scroll.fullScroll(View.FOCUS_UP);
        if (firstDock != null) firstDock.requestFocus();
    }

    @Override public void onBackPressed() { /* a launcher never exits */ }
}
