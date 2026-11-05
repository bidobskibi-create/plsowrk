package com.archdroid.swipenav;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private float d;

    private int dp(int v) { return (int) (v * d); }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xFF1A1C22); g.setCornerRadius(dp(16));
        l.setBackgroundDrawable(g);
        l.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        l.setLayoutParams(lp);
        l.setOnTouchListener(new android.view.View.OnTouchListener() {
            public boolean onTouch(android.view.View v, android.view.MotionEvent e) {
                int a = e.getActionMasked();
                if (a == android.view.MotionEvent.ACTION_DOWN)
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(120).start();
                else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL)
                    v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                            .setInterpolator(new android.view.animation.OvershootInterpolator(2.5f)).start();
                return false;
            }
        });
        return l;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private Switch toggle(String title, String sub, boolean on, CompoundButton.OnCheckedChangeListener l, LinearLayout parent) {
        Switch s = new Switch(this);
        s.setText(title); s.setTextColor(Color.WHITE); s.setTextSize(16);
        s.setChecked(on); s.setOnCheckedChangeListener(l);
        parent.addView(s);
        TextView t = text(sub, 12, 0xFF8A8F9C, false);
        t.setPadding(0, dp(4), 0, 0);
        parent.addView(t);
        return s;
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        d = getResources().getDisplayMetrics().density;
        startService(new Intent(this, SwipeNavService.class));
        final SharedPreferences sp = getSharedPreferences(SwipeNavService.PREFS, Context.MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(16));

        TextView title = text("Swipe navigation", 26, Color.WHITE, true);
        title.setPadding(dp(4), 0, 0, dp(4));
        TextView sub = text("Gesture navigation for your Lumia 520", 13, 0xFF8A8F9C, false);
        sub.setPadding(dp(4), 0, 0, dp(18));
        root.addView(title); root.addView(sub);

        LinearLayout c1 = card();
        toggle("Swipe gestures", "Hides the on-screen buttons. Turn off to bring them back.",
                Settings.System.getInt(getContentResolver(), "dev_force_show_navbar", 1) == 0,
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton v, boolean on) {
                        try { Settings.System.putInt(getContentResolver(), "dev_force_show_navbar", on ? 0 : 1); }
                        catch (Throwable t) {
                            Toast.makeText(SettingsActivity.this, "Could not change setting", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, c1);
        root.addView(c1);

        LinearLayout c2 = card();
        toggle("Show swipe bar", "The thin pill at the bottom edge. Gestures still work when hidden.",
                sp.getBoolean(SwipeNavService.KEY_SHOW_BAR, true),
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton v, boolean on) {
                        sp.edit().putBoolean(SwipeNavService.KEY_SHOW_BAR, on).commit();
                    }
                }, c2);
        root.addView(c2);

        LinearLayout cv = card();
        toggle("iOS-style volume HUD", "Centered rounded volume indicator with a dot meter.",
                sp.getBoolean(SwipeNavService.KEY_VOL_HUD, true),
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton v, boolean on) {
                        sp.edit().putBoolean(SwipeNavService.KEY_VOL_HUD, on).commit();
                    }
                }, cv);
        root.addView(cv);

        LinearLayout c3 = card();
        TextView h = text("Gestures", 13, 0xFF5AB4FF, true);
        h.setPadding(0, 0, 0, dp(8));
        c3.addView(h);
        String[][] rows = {{"\u2194", "Swipe left or right", "Back"}, {"\u2191", "Swipe up", "Home"},
                {"\u2191\u2022", "Swipe up and hold, or far", "Recent apps"}};
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            TextView ic = text(r[0], 18, 0xFF6EE7A8, true);
            ic.setMinWidth(dp(40));
            TextView a = text(r[1], 14, 0xFFC9CCD6, false);
            a.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(ic); row.addView(a); row.addView(text(r[2], 14, Color.WHITE, true));
            c3.addView(row);
        }
        root.addView(c3);

        root.setBackgroundColor(0xFF0E0F13);
        // entrance: everything fades and slides up in a stagger
        for (int i = 0; i < root.getChildCount(); i++) {
            android.view.View v = root.getChildAt(i);
            v.setAlpha(0f); v.setTranslationY(dp(28));
            v.animate().alpha(1f).translationY(0).setStartDelay(60 * i).setDuration(380)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
        }
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF0E0F13);
        sv.addView(root);
        setContentView(sv);
    }
}
