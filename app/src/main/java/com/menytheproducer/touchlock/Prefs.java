package com.menytheproducer.touchlock;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String OPACITY = "opacity";
    static final String HOLD_MS = "hold_ms";
    static final String DELAY = "delay_sec";
    static final String VIBRATION = "vibration";
    static final String HINTS = "hints";
    static final String AWAKE = "awake";
    static final String BUBBLE_ACTIVE = "bubble_active";
    static final String IS_LOCKED = "is_locked";

    static SharedPreferences of(Context context) {
        return context.getSharedPreferences("meny_touch_lock_prefs", Context.MODE_PRIVATE);
    }

    static int opacity(Context context) { return of(context).getInt(OPACITY, 18); }
    static long hold(Context context) { return of(context).getInt(HOLD_MS, 2200); }
    static int delay(Context context) { return of(context).getInt(DELAY, 0); }
    static boolean vibrate(Context context) { return of(context).getBoolean(VIBRATION, true); }
    static boolean hints(Context context) { return of(context).getBoolean(HINTS, true); }
    static boolean awake(Context context) { return of(context).getBoolean(AWAKE, false); }

    private Prefs() { }
}
