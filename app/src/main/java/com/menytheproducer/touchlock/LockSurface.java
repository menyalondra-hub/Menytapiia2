package com.menytheproducer.touchlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** Touch-consuming, full-window view with a deliberately long-press-only unlock. */
final class LockSurface extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable unlocked;
    private final long requiredMs;
    private boolean pressed;
    private boolean finished;
    private long downTime;
    private final Runnable complete;

    LockSurface(Context context, Runnable unlockCallback) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        unlocked = unlockCallback;
        requiredMs = Prefs.hold(context);
        complete = () -> {
            if (pressed && !finished) {
                finished = true;
                pressed = false;
                unlocked.run();
            }
        };
        setContentDescription("Pantalla táctil bloqueada. Mantén presionado el candado central para desbloquear.");
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private void fill(Canvas canvas, int color) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(color); paint.setStrokeWidth(1);
        canvas.drawColor(color);
    }
    private void write(Canvas c, String text, float x, float y, float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.BOLD) : Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(dp(size));
        paint.setColor(color);
        c.drawText(text, x, y, paint);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int opacity = Math.min(85, Math.max(0, Prefs.opacity(getContext())));
        fill(c, Color.argb((int)(255 * opacity / 100f), 3, 8, 22));
        float w = getWidth();
        float h = getHeight();
        float x = w / 2f, cy = h / 2f;
        final int white = Color.rgb(246, 249, 255);
        final int dim = Color.rgb(185, 205, 229);
        final int cyan = Color.rgb(66, 233, 226);

        if (Prefs.hints(getContext())) {
            write(c, "MENY TOUCH LOCK", x, Math.max(dp(50), h * .13f), 15, white, true);
            write(c, "Pantalla táctil bloqueada", x, Math.max(dp(77), h * .13f + dp(26)), 13, dim, false);
        }

        float radius = dp(63);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(222, 23, 30, 52));
        c.drawCircle(x, cy, radius + dp(7), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(5));
        paint.setColor(Color.argb(200, 145, 161, 185));
        c.drawCircle(x, cy, radius, paint);

        float progress = 0f;
        if (pressed) progress = Math.min(1f, (SystemClock.uptimeMillis() - downTime) / (float)requiredMs);
        if (pressed) {
            paint.setColor(cyan);
            paint.setStrokeCap(Paint.Cap.ROUND);
            RectF r = new RectF(x - radius, cy - radius, x + radius, cy + radius);
            c.drawArc(r, -90, 360f * progress, false, paint);
        }
        drawLock(c, x, cy - dp(12), white);
        write(c, pressed ? "DESBLOQUEANDO" : "DESBLOQUEAR", x, cy + dp(35), 9, white, true);
        write(c, pressed ? Math.round(progress * 100f) + "%" : "Mantén presionado", x,
                cy + radius + dp(41), 14, white, true);
        write(c, pressed ? "Sigue presionando..." : "La reproducción sigue activa", x,
                cy + radius + dp(66), 12, dim, false);
        if (pressed) postInvalidateDelayed(16);
    }

    private void drawLock(Canvas canvas, float x, float y, int color) {
        paint.setStrokeWidth(dp(5));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        RectF arch = new RectF(x - dp(15), y - dp(18), x + dp(15), y + dp(14));
        canvas.drawArc(arch, 180, 180, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(x - dp(20), y + dp(1), x + dp(20), y + dp(32)), dp(6), dp(6), paint);
        paint.setColor(Color.rgb(23, 30, 52));
        canvas.drawCircle(x, y + dp(15), dp(3.3f), paint);
    }

    private boolean inside(float x, float y) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= dp(110) * dp(110);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = inside(event.getX(), event.getY());
                if (pressed) {
                    downTime = SystemClock.uptimeMillis();
                    postDelayed(complete, requiredMs);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed && !inside(event.getX(), event.getY())) reset();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                reset();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                reset();
                return true;
            default:
                return true;
        }
    }

    private void reset() {
        pressed = false;
        removeCallbacks(complete);
        invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(complete);
        super.onDetachedFromWindow();
    }
}
