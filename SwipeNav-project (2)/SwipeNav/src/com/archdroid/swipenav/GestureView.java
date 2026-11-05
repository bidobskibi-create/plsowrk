package com.archdroid.swipenav;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/**
 * Bottom-edge strip.  Left/right = Back, up = Home, up+hold or far = Recents.
 * Spring-physics pill: squashes on touch, follows the finger, glows, ripples on
 * action, fades down when idle and springs back to life on touch.
 */
public class GestureView extends View {
    private static final int WHITE = 0xFFFFFFFF, ICE = 0xFFDDEEFF, BLUE = 0xFF5AB4FF, GREEN = 0xFF6EE7A8;
    private final float d = getResources().getDisplayMetrics().density;
    private final float upDist = 40 * d, farDist = 160 * d, sideDist = 56 * d;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Handler h = new Handler();

    private float downX, downY, curX, curY;
    private boolean tracking, fired, showBar = true, idle;
    private int armed = WHITE;

    // spring state: position + velocity
    private float sx, vx;       // horizontal shift
    private float sl, vl;       // lift
    private float sw, vw;       // width/squash factor (0 rest .. 1 pressed)
    private float flash;        // 0..1, decays
    private float ripple = 1f, rippleX; // 0..1 progress, 1 = finished
    private int rippleColor;
    private float alpha = 0.6f; // eased pill alpha 0..1

    private final Runnable holdCheck = new Runnable() {
        public void run() {
            if (tracking && !fired && downY - curY > upDist) fire(KeyEvent.KEYCODE_APP_SWITCH, BLUE);
        }
    };
    private final Runnable goIdle = new Runnable() {
        public void run() { idle = true; postInvalidateOnAnimation(); }
    };

    public GestureView(Context c) { super(c); wake(); }

    public void setShowBar(boolean b) { showBar = b; invalidate(); }

    private void wake() {
        idle = false;
        h.removeCallbacks(goIdle);
        h.postDelayed(goIdle, 2500);
    }

    private void fire(int code, int color) {
        fired = true; armed = color; flash = 1f;
        ripple = 0f; rippleX = getWidth() / 2f + sx; rippleColor = color;
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        Keys.press(getContext(), code);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        wake();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = curX = e.getRawX(); downY = curY = e.getRawY();
                tracking = true; fired = false; armed = WHITE;
                h.postDelayed(holdCheck, 450);
                break;
            case MotionEvent.ACTION_MOVE:
                curX = e.getRawX(); curY = e.getRawY();
                float up = downY - curY, side = Math.abs(curX - downX);
                if (!fired && up > farDist) fire(KeyEvent.KEYCODE_APP_SWITCH, BLUE);
                else if (!fired && side > sideDist && side > up) fire(KeyEvent.KEYCODE_BACK, GREEN);
                if (!fired && up > upDist) {
                    armed = ICE;
                    h.removeCallbacks(holdCheck); h.postDelayed(holdCheck, 450);
                }
                break;
            case MotionEvent.ACTION_UP:
                curX = e.getRawX(); curY = e.getRawY();
                if (!fired && downY - curY > upDist) fire(KeyEvent.KEYCODE_HOME, WHITE);
                // fall through
            case MotionEvent.ACTION_CANCEL:
                tracking = false; h.removeCallbacks(holdCheck);
                break;
        }
        postInvalidateOnAnimation();
        return true;
    }

    @Override protected void onDetachedFromWindow() {
        h.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }

    /** One spring step toward target. Returns new pos; velocity via out[0]. */
    private final float[] out = new float[1];
    private float spring(float pos, float vel, float target, float k, float damp) {
        vel = (vel + (target - pos) * k) * damp;
        out[0] = vel;
        return pos + vel;
    }

    @Override protected void onDraw(Canvas c) {
        if (!showBar) return;

        float tx = tracking ? Math.max(-34 * d, Math.min(34 * d, (curX - downX) * 0.5f)) : 0;
        float tl = tracking ? Math.min(11 * d, Math.max(0, downY - curY) * 0.28f) : 0;
        float tw = tracking ? 1f : 0f;
        sx = spring(sx, vx, tx, 0.22f, 0.72f); vx = out[0];
        sl = spring(sl, vl, tl, 0.22f, 0.72f); vl = out[0];
        sw = spring(sw, vw, tw, 0.20f, 0.70f); vw = out[0];
        flash *= 0.90f;
        if (ripple < 1f) ripple = Math.min(1f, ripple + 0.07f);
        float ta = tracking || flash > 0.05f ? 1f : (idle ? 0.28f : 0.6f);
        alpha += (ta - alpha) * 0.15f;

        float cx = getWidth() / 2f + sx, cy = getHeight() - 9 * d - sl;
        float half = (30 + 18 * sw + 12 * flash) * d;
        float t = (1.7f + 0.9f * sw + 0.7f * flash) * d;
        int base = tracking || flash > 0.05f ? armed : WHITE;
        float emph = Math.max(sw, flash);

        // ripple band (expands from the pill when an action fires)
        if (ripple < 1f) {
            float p = 1f - (1f - ripple) * (1f - ripple);       // ease-out
            paint.setColor((rippleColor & 0x00FFFFFF) | ((int) (70 * (1f - ripple)) << 24));
            c.drawCircle(rippleX, getHeight(), (20 + 150 * p) * d, paint);
        }
        // glow
        paint.setColor((base & 0x00FFFFFF) | ((int) (55 * emph) << 24));
        r.set(cx - half - 7 * d, cy - t - 5 * d, cx + half + 7 * d, cy + t + 5 * d);
        c.drawRoundRect(r, t + 5 * d, t + 5 * d, paint);
        // pill
        paint.setColor((base & 0x00FFFFFF) | ((int) (255 * Math.min(1f, alpha * (0.45f + 0.55f * Math.max(emph, 0.4f)))) << 24));
        r.set(cx - half, cy - t, cx + half, cy + t);
        c.drawRoundRect(r, t, t, paint);

        boolean moving = Math.abs(sx - tx) > 0.2f || Math.abs(sl - tl) > 0.2f || Math.abs(sw - tw) > 0.01f
                || Math.abs(vx) > 0.05f || Math.abs(vl) > 0.05f || Math.abs(vw) > 0.002f
                || flash > 0.02f || ripple < 1f || Math.abs(alpha - ta) > 0.01f;
        if (moving) postInvalidateOnAnimation();
    }
}
