package com.menytheproducer.touchlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class TouchLockService extends Service {
    public static final String SHOW = "com.menytheproducer.touchlock.SHOW";
    public static final String STOP = "com.menytheproducer.touchlock.STOP";
    public static final String LOCK = "com.menytheproducer.touchlock.LOCK";
    public static final String UNLOCK = "com.menytheproducer.touchlock.UNLOCK";

    private static final String CHANNEL = "meny_touch_lock_controls";
    private static final int NOTIFICATION_ID = 1744;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager manager;
    private View bubble;
    private LockSurface cover;
    private WindowManager.LayoutParams bubbleParams;
    private Runnable pendingLock;
    private boolean foregroundStarted;

    @Override public void onCreate() {
        super.onCreate();
        manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Controles de bloqueo táctil",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Desbloqueo seguro y control del botón flotante");
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? SHOW : intent.getAction();
        if (STOP.equals(action)) {
            shutDown();
            return START_NOT_STICKY;
        }
        if (UNLOCK.equals(action)) {
            unlock();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Autoriza Mostrar sobre otras apps para usar Meny Touch Lock", Toast.LENGTH_LONG).show();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!foregroundStarted) {
            try {
                startForeground(NOTIFICATION_ID, notification(false));
                foregroundStarted = true;
            } catch (Exception e) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        Prefs.of(this).edit().putBoolean(Prefs.BUBBLE_ACTIVE, true).apply();
        if (LOCK.equals(action)) lock();
        else if (cover == null) showBubble();
        updateNotification(cover != null);
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private int dp(float x) { return Math.round(x * getResources().getDisplayMetrics().density); }

    private PendingIntent serviceAction(String action, int id) {
        Intent i = new Intent(this, TouchLockService.class);
        i.setAction(action);
        return PendingIntent.getService(this, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Notification notification(boolean locked) {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap = PendingIntent.getActivity(this, 9, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(com.menytheproducer.touchlock.R.drawable.ic_notification)
                .setContentTitle(locked ? "Pantalla bloqueada" : "Meny Touch Lock activo")
                .setContentText(locked ? "Mantén pulsado el candado para desbloquear" : "Toca el candado flotante para bloquear los toques")
                .setContentIntent(tap)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (locked) builder.addAction(android.R.drawable.ic_lock_lock, "Desbloquear", serviceAction(UNLOCK, 11));
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", serviceAction(STOP, 12));
        return builder.build();
    }

    private void updateNotification(boolean locked) {
        if (!foregroundStarted) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification(locked));
    }

    private WindowManager.LayoutParams overlayParams(int width, int height) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private void showBubble() {
        if (bubble != null || cover != null) return;
        int side = dp(61);
        FrameLayout floating = new FrameLayout(this);
        GradientDrawable glow = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(71, 225, 232), Color.rgb(113, 83, 242)});
        glow.setShape(GradientDrawable.OVAL);
        glow.setStroke(dp(2), Color.WHITE);
        floating.setBackground(glow);
        floating.setElevation(dp(12));
        floating.setContentDescription("Meny Touch Lock, toca para bloquear. Arrastra para mover.");
        TextView glyph = new TextView(this);
        glyph.setText("🔒");
        glyph.setTextSize(25);
        glyph.setGravity(Gravity.CENTER);
        floating.addView(glyph, new FrameLayout.LayoutParams(-1, -1));
        bubbleParams = overlayParams(side, side);
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        bubbleParams.x = metrics.widthPixels - side - dp(14);
        bubbleParams.y = Math.max(dp(120), metrics.heightPixels / 2 - side / 2);
        floating.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int originX, originY;
            boolean moved;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX(); downY = event.getRawY();
                        originX = bubbleParams.x; originY = bubbleParams.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = Math.round(event.getRawX() - downX);
                        int deltaY = Math.round(event.getRawY() - downY);
                        if (Math.abs(deltaX) > dp(8) || Math.abs(deltaY) > dp(8)) moved = true;
                        if (moved) {
                            bubbleParams.x = Math.max(0, Math.min(metrics.widthPixels - side, originX + deltaX));
                            bubbleParams.y = Math.max(0, Math.min(metrics.heightPixels - side, originY + deltaY));
                            if (bubble != null) {
                                try { manager.updateViewLayout(bubble, bubbleParams); }
                                catch (IllegalArgumentException ignored) { }
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) scheduleLock();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        return true;
                    default:
                        return true;
                }
            }
        });
        try {
            manager.addView(floating, bubbleParams);
            bubble = floating;
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo mostrar el botón flotante", Toast.LENGTH_LONG).show();
            shutDown();
        }
    }

    private void scheduleLock() {
        if (pendingLock != null) {
            handler.removeCallbacks(pendingLock);
            pendingLock = null;
            Toast.makeText(this, "Bloqueo cancelado", Toast.LENGTH_SHORT).show();
            return;
        }
        int seconds = Prefs.delay(this);
        if (seconds == 0) { lock(); return; }
        Toast.makeText(this, "Se bloqueará en " + seconds + " segundos. Toca otra vez para cancelar.", Toast.LENGTH_LONG).show();
        pendingLock = () -> { pendingLock = null; lock(); };
        handler.postDelayed(pendingLock, seconds * 1000L);
    }

    private void removeBubble() {
        if (bubble != null) {
            try { manager.removeView(bubble); } catch (Exception ignored) { }
            bubble = null;
        }
    }

    private void lock() {
        cancelPending();
        if (cover != null) return;
        removeBubble();
        cover = new LockSurface(this, this::unlock);
        WindowManager.LayoutParams p = overlayParams(-1, -1);
        if (Prefs.awake(this)) p.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        try {
            manager.addView(cover, p);
            Prefs.of(this).edit().putBoolean(Prefs.IS_LOCKED, true).apply();
            vibrate(35);
            updateNotification(true);
        } catch (Exception e) {
            cover = null;
            Prefs.of(this).edit().putBoolean(Prefs.IS_LOCKED, false).apply();
            showBubble();
        }
    }

    private void unlock() {
        cancelPending();
        if (cover != null) {
            try { manager.removeView(cover); } catch (Exception ignored) { }
            cover = null;
            vibrate(65);
        }
        Prefs.of(this).edit().putBoolean(Prefs.IS_LOCKED, false).apply();
        if (Prefs.of(this).getBoolean(Prefs.BUBBLE_ACTIVE, false)) showBubble();
        updateNotification(false);
    }

    private void vibrate(int durationMs) {
        if (!Prefs.vibrate(this)) return;
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= 26)
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            else vibrator.vibrate(durationMs);
        }
    }

    private void cancelPending() {
        if (pendingLock != null) { handler.removeCallbacks(pendingLock); pendingLock = null; }
    }

    private void shutDown() {
        cancelPending();
        if (cover != null) {
            try { manager.removeView(cover); } catch (Exception ignored) { }
            cover = null;
        }
        removeBubble();
        Prefs.of(this).edit().putBoolean(Prefs.IS_LOCKED, false).putBoolean(Prefs.BUBBLE_ACTIVE, false).apply();
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        stopSelf();
    }

    @Override public void onDestroy() {
        cancelPending();
        if (cover != null) { try { manager.removeView(cover); } catch (Exception ignored) {} cover = null; }
        removeBubble();
        Prefs.of(this).edit().putBoolean(Prefs.IS_LOCKED, false).putBoolean(Prefs.BUBBLE_ACTIVE, false).apply();
        super.onDestroy();
    }
}
