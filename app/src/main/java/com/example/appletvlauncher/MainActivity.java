package com.example.appletvlauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Outline;
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
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
    ImageView bg;

    // Wallpapers: files res/drawable-nodpi/bg_<name>.jpg. Drop a new bg_xxx.jpg (or .png)
    // there and it shows up in the picker by itself. "noir" = pure black.
    static final String[] BG_ORDER = {"aurore", "cote_amalfitaine", "nuit_bleue", "crepuscule", "ocean", "braise"};
    static final String BG_DEFAULT = "aurore";

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    GradientDrawable box(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    StateListDrawable glass(int r) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, strongGlass(r));
        s.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return s;
    }

    // ---- Liquid Glass look (no blur: Android 11 has no cheap one) ----
    static int w(int a) { return (a << 24) | 0xFFFFFF; }

    // Resting glass: light translucent body, bright rim at top-left, soft rim at bottom-right.
    Drawable lightGlass(int r) {
        return new Glass(dp(r), Math.max(1.5f, dp(1) * 1.2f), w(0x3A), w(0x12), w(0xB4), w(0x10), w(0x5A), w(0x34));
    }
    // Focused glass: brighter, thicker highlight.
    Drawable strongGlass(int r) {
        return new Glass(dp(r), Math.max(2f, dp(1) * 1.6f), w(0x70), w(0x34), w(0xF0), w(0x22), w(0x9A), w(0x48));
    }
    // Panels over a busy picture: dark tinted glass.
    Drawable darkGlass(int r) {
        return new Glass(dp(r), Math.max(1.5f, dp(1) * 1.2f), 0xEE16171C, 0xEE0B0C10, w(0x8C), w(0x10), w(0x40), w(0x1C));
    }

    static class Glass extends Drawable {
        final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), sheen = new Paint(Paint.ANTI_ALIAS_FLAG),
                rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF box = new RectF();
        final float r, stroke;
        final int top, bot, rimA, rimB, rimC, sheenA;

        Glass(float r, float stroke, int top, int bot, int rimA, int rimB, int rimC, int sheenA) {
            this.r = r; this.stroke = stroke; this.top = top; this.bot = bot;
            this.rimA = rimA; this.rimB = rimB; this.rimC = rimC; this.sheenA = sheenA;
            rim.setStyle(Paint.Style.STROKE); rim.setStrokeWidth(stroke);
        }

        @Override protected void onBoundsChange(android.graphics.Rect b) {
            float h = stroke / 2f;
            box.set(b.left + h, b.top + h, b.right - h, b.bottom - h);
            fill.setShader(new LinearGradient(0, b.top, 0, b.bottom, top, bot, Shader.TileMode.CLAMP));
            // Specular sheen on the upper half only.
            sheen.setShader(new LinearGradient(0, b.top, 0, b.top + b.height() * 0.55f,
                    sheenA, sheenA & 0xFFFFFF, Shader.TileMode.CLAMP));
            // Rim light: strong top-left, faint middle, medium bottom-right (like light bending at the edge).
            rim.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                    new int[]{rimA, rimB, rimB, rimC}, new float[]{0f, 0.35f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        }

        @Override public void draw(Canvas c) {
            c.drawRoundRect(box, r, r, fill);
            c.drawRoundRect(box, r, r, sheen);
            c.drawRoundRect(box, r, r, rim);
        }
        @Override public void setAlpha(int a) { }
        @Override public void setColorFilter(ColorFilter f) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

      <T extends TextView> T txt(T t, int sp, int color, String font) {
        t.setTextSize(sp); t.setTextColor(color);
        t.setTypeface(Typeface.create(font, Typeface.NORMAL)); return t;
    }

    TextView pill(String label, View.OnClickListener c) {
        TextView t = txt(new TextView(this), 16, Color.WHITE, "sans-serif");
        t.setText(label); t.setGravity(Gravity.CENTER);
        t.setPadding(dp(24), dp(10), dp(24), dp(10));
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, strongGlass(30));
        s.addState(new int[]{}, lightGlass(30));
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

    String bgLabel(String n) {
        switch (n) {
            case "aurore": return "Aurore";
            case "cote_amalfitaine": return "Côte amalfitaine";
            case "nuit_bleue": return "Nuit bleue";
            case "crepuscule": return "Crépuscule";
            case "ocean": return "Océan";
            case "braise": return "Braise";
            case "noir": return "Noir pur";
        }
        String t = n.replace('_', ' ');
        return t.substring(0, 1).toUpperCase() + t.substring(1);
    }

    List<String> wallpapers() {
        List<String> l = new ArrayList<>(Arrays.asList(BG_ORDER));
        List<String> extra = new ArrayList<>();
        for (java.lang.reflect.Field f : R.drawable.class.getFields()) {
            String n = f.getName();
            if (n.startsWith("bg_") && !l.contains(n.substring(3))) extra.add(n.substring(3));
        }
        Collections.sort(extra); l.addAll(extra); l.add("noir");
        return l;
    }

    Bitmap loadBg(String n, int sample) {
        int id = getResources().getIdentifier("bg_" + n, "drawable", getPackageName());
        if (id == 0) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        try { return BitmapFactory.decodeResource(getResources(), id, o); }
        catch (Throwable e) { return null; }
    }

    void applyWallpaper(String n) {
        bg.setImageBitmap(n.equals("noir") ? null : loadBg(n, 1));
    }

    View wallThumb(final String n, final Dialog d) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL); t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setPadding(dp(8), dp(8), dp(8), dp(8)); t.setBackground(glass(18));
        t.setFocusable(true);
        ImageView iv = new ImageView(this);
        iv.setBackgroundColor(Color.BLACK); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (!n.equals("noir")) iv.setImageBitmap(loadBg(n, 4));
        iv.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(12));
            }
        });
        iv.setClipToOutline(true);
        t.addView(iv, new LinearLayout.LayoutParams(dp(224), dp(126)));
        TextView l = txt(new TextView(this), 15, Color.WHITE, "sans-serif-light");
        l.setText(bgLabel(n)); l.setGravity(Gravity.CENTER); l.setPadding(0, dp(6), 0, 0);
        t.addView(l, new LinearLayout.LayoutParams(dp(224), -2));
        // Live preview while moving, saved only on OK.
        t.setOnFocusChangeListener((v, f) -> {
            v.animate().scaleX(f ? 1.06f : 1f).scaleY(f ? 1.06f : 1f).setDuration(120).start();
            if (f) applyWallpaper(n);
        });
        t.setOnClickListener(v -> { prefs.edit().putString("bg", n).apply(); d.dismiss(); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(6), dp(6), dp(6), dp(6)); t.setLayoutParams(lp);
        return t;
    }

    void wallpaperPicker() {
        final Dialog d = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        FrameLayout host = new FrameLayout(this);
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackground(darkGlass(32)); panel.setPadding(dp(28), dp(22), dp(28), dp(22));
        TextView title = txt(new TextView(this), 22, Color.WHITE, "sans-serif-light");
        title.setText("Fond d’écran"); panel.addView(title);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = new LinearLayout(this);
        strip.setPadding(dp(8), dp(14), dp(8), dp(4));
        String saved = prefs.getString("bg", BG_DEFAULT);
        View current = null;
        for (String n : wallpapers()) {
            View t = wallThumb(n, d); strip.addView(t);
            if (n.equals(saved)) current = t;
        }
        hs.addView(strip); panel.addView(hs);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.setMargins(dp(40), 0, dp(40), dp(32));
        host.addView(panel, lp);
        d.setContentView(host);
        // Back or OK: always end on the saved wallpaper.
        d.setOnDismissListener(x -> applyWallpaper(prefs.getString("bg", BG_DEFAULT)));
        d.show();
        if (current == null) current = strip.getChildAt(0);
        if (current != null) current.requestFocus();
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
        scroll.setBackgroundColor(Color.TRANSPARENT); scroll.setVerticalScrollBarEnabled(false);
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
        top.add(pill("Réglages", v -> new AlertDialog.Builder(this)
                .setItems(new String[]{"Fond d’écran", "Réglages Android"}, (dlg, w) -> {
                    if (w == 0) wallpaperPicker();
                    else startActivity(new Intent(Settings.ACTION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }).show()));
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
        dock.setBackground(lightGlass(34)); dock.setPadding(dp(12), dp(12), dp(12), dp(12));
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
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        bg = new ImageView(this); bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        // Dark scrim keeps the clock, dock and labels readable on any wallpaper.
        View scrim = new View(this);
        scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0x66000000, 0x22000000, 0xAA000000}));
        root.addView(scrim, new FrameLayout.LayoutParams(-1, -1));
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        applyWallpaper(prefs.getString("bg", BG_DEFAULT));
        setContentView(root);
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
