package com.archdroid.swipenav;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;

public class SwipeNavService extends Service {
    static final String PREFS = "swipenav", KEY_SHOW_BAR = "show_bar", KEY_VOL_HUD = "vol_hud";
    private VolumeHud hud;
    private final BroadcastReceiver volRx = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            if (hud == null || !sp.getBoolean(KEY_VOL_HUD, true)) return;
            int st = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", 3);
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            hud.show(am.getStreamVolume(st), am.getStreamMaxVolume(st));
        }
    };
    private WindowManager wm;
    private GestureView view;
    private SharedPreferences sp;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences p, String k) { apply(); }
    };
    private final ContentObserver obs = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) { apply(); }
    };

    @Override public void onCreate() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(prefListener);
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor("dev_force_show_navbar"), false, obs);
        // iOS 9 style volume HUD (centered, touch-through)
        hud = new VolumeHud(this);
        float d = getResources().getDisplayMetrics().density;
        WindowManager.LayoutParams hp = new WindowManager.LayoutParams((int) (150 * d), (int) (150 * d), 2010,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        hp.gravity = Gravity.CENTER;
        hp.setTitle("SwipeNavVolume");
        wm.addView(hud, hp);
        registerReceiver(volRx, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
        apply();
    }

    /** Gestures on (nav bar hidden) -> show strip. Off -> remove strip. */
    private void apply() {
        boolean on = Settings.System.getInt(getContentResolver(), "dev_force_show_navbar", 1) == 0;
        if (on && view == null) {
            view = new GestureView(this);
            float d = getResources().getDisplayMetrics().density;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT, (int) (24 * d),
                    2010 /* TYPE_SYSTEM_ERROR */,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.BOTTOM;
            lp.setTitle("SwipeNav");
            wm.addView(view, lp);
        } else if (!on && view != null) {
            wm.removeView(view);
            view = null;
        }
        if (view != null) view.setShowBar(sp.getBoolean(KEY_SHOW_BAR, true));
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onDestroy() {
        unregisterReceiver(volRx);
        if (hud != null) { wm.removeView(hud); hud = null; }
        sp.unregisterOnSharedPreferenceChangeListener(prefListener);
        getContentResolver().unregisterContentObserver(obs);
        if (view != null) { wm.removeView(view); view = null; }
    }
}
