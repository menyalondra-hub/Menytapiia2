package com.menytheproducer.touchlock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(9, 13, 26);
    private static final int PANEL = Color.rgb(21, 27, 45);
    private static final int WHITE = Color.rgb(244, 247, 255);
    private static final int MUTED = Color.rgb(166, 180, 204);
    private static final int CYAN = Color.rgb(46, 220, 220);
    private static final int PURPLE = Color.rgb(117, 91, 248);

    private TextView status;
    private TextView permissionStatus;
    private Button mainButton;
    private Button permissionButton;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildScreen();
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        tv.setFontFeatureSettings("kern");
        tv.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(top);
        parent.addView(child, p);
    }

    private LinearLayout panel() {
        LinearLayout l = vertical();
        l.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = shape(PANEL, 23);
        bg.setStroke(dp(1), Color.rgb(43, 54, 79));
        l.setBackground(bg);
        return l;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setTextColor(primary ? BG : WHITE);
        b.setMinHeight(dp(54));
        b.setPadding(dp(14), 0, dp(14), 0);
        if (primary) {
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{CYAN, Color.rgb(85, 187, 238)});
            g.setCornerRadius(dp(16));
            b.setBackground(g);
        } else {
            GradientDrawable g = shape(Color.rgb(41, 51, 79), 14);
            g.setStroke(dp(1), Color.rgb(73, 90, 125));
            b.setBackground(g);
        }
        return b;
    }

    private void settingTitle(LinearLayout parent, String title, String subtitle) {
        add(parent, text(title, 15, WHITE, true), 0);
        add(parent, text(subtitle, 12, MUTED, false), 5);
    }

    private void buildScreen() {
        ScrollView scroller = new ScrollView(this);
        scroller.setBackgroundColor(BG);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        LinearLayout root = vertical();
        root.setPadding(dp(20), dp(30), dp(20), dp(36));
        scroller.addView(root);
        setContentView(scroller);

        TextView eyebrow = text("MENYTHEPRODUCER  /  UTILIDADES", 11, CYAN, true);
        eyebrow.setLetterSpacing(.13f);
        add(root, eyebrow, 0);
        add(root, text("Meny Touch Lock", 30, WHITE, true), 9);
        add(root, text("Tu pantalla, bajo control.", 15, MUTED, false), 3);

        LinearLayout hero = vertical();
        hero.setPadding(dp(21), dp(26), dp(21), dp(24));
        GradientDrawable heroGradient = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{Color.rgb(55, 49, 129), Color.rgb(20, 78, 105), Color.rgb(21, 36, 59)});
        heroGradient.setCornerRadius(dp(26));
        heroGradient.setStroke(dp(1), Color.rgb(65, 108, 149));
        hero.setBackground(heroGradient);
        add(hero, text("◉   BLOQUEO INTELIGENTE", 12, CYAN, true), 0);
        add(hero, text("Videos sin toques\naccidentales", 26, WHITE, true), 15);
        add(hero, text("Activa el botón flotante, abre tu video y tócalo para bloquear la pantalla sin pausar la reproducción.", 14, WHITE, false), 13);
        status = text("Comprobando estado...", 13, CYAN, true);
        add(hero, status, 20);
        add(root, hero, 24);

        LinearLayout control = panel();
        add(control, text("CONTROL PRINCIPAL", 11, CYAN, true), 0);
        mainButton = button("Activar botón flotante", true);
        add(control, mainButton, 14);
        mainButton.setOnClickListener(v -> onMainButton());
        add(control, text("El icono se puede mover y aparece sobre otras aplicaciones. Mantén presionado el candado central para desbloquear.", 12, MUTED, false), 13);
        add(root, control, 16);

        LinearLayout permissions = panel();
        settingTitle(permissions, "Permiso de superposición", "Necesario para bloquear los toques encima de otras apps.");
        permissionStatus = text("", 13, CYAN, true);
        add(permissions, permissionStatus, 12);
        permissionButton = button("Conceder permiso", false);
        permissionButton.setOnClickListener(v -> requestOverlay());
        add(permissions, permissionButton, 12);
        add(root, permissions, 14);

        add(root, text("PERSONALIZACIÓN", 12, CYAN, true), 29);
        LinearLayout settings = panel();

        TextView opacityValue = text("", 13, CYAN, true);
        settingTitle(settings, "Oscurecimiento", "Ajusta cuánto se oscurece la pantalla bloqueada.");
        add(settings, opacityValue, 10);
        SeekBar opacity = new SeekBar(this);
        opacity.setMax(60);
        opacity.setProgress(Prefs.opacity(this));
        opacity.setProgressTintList(ColorStateList.valueOf(CYAN));
        opacity.setThumbTintList(ColorStateList.valueOf(CYAN));
        opacityValue.setText(opacity.getProgress() + "%");
        opacity.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                opacityValue.setText(value + "%");
                if (fromUser) Prefs.of(MainActivity.this).edit().putInt(Prefs.OPACITY, value).apply();
            }
        });
        add(settings, opacity, 4);
        divider(settings);

        TextView holdValue = text("", 13, CYAN, true);
        settingTitle(settings, "Tiempo para desbloquear", "Mantén presionado el candado el tiempo elegido.");
        add(settings, holdValue, 10);
        SeekBar hold = new SeekBar(this);
        hold.setMax(8);
        hold.setProgress(Math.max(0, Math.min(8, (Prefs.of(this).getInt(Prefs.HOLD_MS, 2200) - 1000) / 500)));
        hold.setProgressTintList(ColorStateList.valueOf(CYAN));
        hold.setThumbTintList(ColorStateList.valueOf(CYAN));
        holdValue.setText(String.format(java.util.Locale.getDefault(), "%.1f segundos", (1000 + hold.getProgress() * 500) / 1000.0f));
        hold.setOnSeekBarChangeListener(new SimpleSeekListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean fromUser) {
                int ms = 1000 + 500 * value;
                holdValue.setText(String.format(java.util.Locale.getDefault(), "%.1f segundos", ms / 1000.0f));
                if (fromUser) Prefs.of(MainActivity.this).edit().putInt(Prefs.HOLD_MS, ms).apply();
            }
        });
        add(settings, hold, 4);
        divider(settings);

        settingTitle(settings, "Retardo al bloquear", "Después de tocar el botón flotante.");
        Spinner delays = new Spinner(this);
        String[] delayLabels = {"Inmediato", "3 segundos", "5 segundos", "10 segundos"};
        int[] delayTimes = {0, 3, 5, 10};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, delayLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        delays.setAdapter(adapter);
        delays.setBackground(shape(Color.rgb(38, 48, 72), 12));
        delays.setPadding(dp(12), dp(7), dp(12), dp(7));
        delays.setSelection(indexOf(delayTimes, Prefs.delay(this)));
        delays.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                Prefs.of(MainActivity.this).edit().putInt(Prefs.DELAY, delayTimes[pos]).apply();
                if (v instanceof TextView) ((TextView) v).setTextColor(WHITE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        add(settings, delays, 12);
        divider(settings);

        addToggle(settings, "Vibración al bloquear", "Confirma el bloqueo con una vibración corta.", Prefs.VIBRATION, true);
        divider(settings);
        addToggle(settings, "Mostrar instrucciones", "Presenta una ayuda breve en la pantalla bloqueada.", Prefs.HINTS, true);
        divider(settings);
        addToggle(settings, "Mantener pantalla encendida", "Impide que el dispositivo entre en reposo mientras está bloqueado.", Prefs.AWAKE, false);
        add(root, settings, 13);

        LinearLayout tips = panel();
        add(tips, text("¿CÓMO SE USA?", 12, CYAN, true), 0);
        add(tips, text("1  Autoriza el permiso de superposición.\n\n2  Activa el botón flotante y abre tu video.\n\n3  Toca el botón para bloquear los toques.\n\n4  Mantén presionado el círculo para desbloquear.", 14, WHITE, false), 16);
        add(tips, text("También puedes detener el servicio desde la notificación persistente. Los botones físicos y algunos gestos del sistema no pueden bloquearse.", 12, MUTED, false), 18);
        add(root, tips, 17);
        TextView footer = text("MENY TOUCH LOCK  ·  100% GRATIS  ·  SIN ANUNCIOS", 11, MUTED, true);
        footer.setGravity(Gravity.CENTER);
        add(root, footer, 30);
        refreshStatus();
    }

    private void divider(LinearLayout panel) {
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(48, 59, 81));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.topMargin = dp(20);
        params.bottomMargin = dp(20);
        panel.addView(line, params);
    }

    private void addToggle(LinearLayout parent, String title, String subtitle, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout texts = vertical();
        add(texts, text(title, 15, WHITE, true), 0);
        add(texts, text(subtitle, 12, MUTED, false), 5);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        Switch sw = new Switch(this);
        sw.setChecked(Prefs.of(this).getBoolean(key, defaultValue));
        sw.setThumbTintList(new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{CYAN, Color.rgb(165, 174, 198)}));
        sw.setOnCheckedChangeListener((b, checked) -> Prefs.of(this).edit().putBoolean(key, checked).apply());
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.leftMargin = dp(10);
        row.addView(sw, sp);
        add(parent, row, 0);
    }

    private int indexOf(int[] options, int value) {
        for (int i = 0; i < options.length; i++) if (options[i] == value) return i;
        return 0;
    }

    private void refreshStatus() {
        if (status == null) return;
        boolean allowed = Settings.canDrawOverlays(this);
        boolean active = Prefs.of(this).getBoolean(Prefs.BUBBLE_ACTIVE, false);
        boolean locked = Prefs.of(this).getBoolean(Prefs.IS_LOCKED, false);
        status.setText(!allowed ? "●  Permiso pendiente" : locked ? "●  Pantalla bloqueada" : active ? "●  Botón flotante activado" : "●  Listo para activar");
        status.setTextColor(allowed ? CYAN : Color.rgb(255, 191, 108));
        if (permissionStatus != null) permissionStatus.setText(allowed ? "✓ Permiso concedido" : "!  Permiso necesario");
        if (permissionButton != null) permissionButton.setVisibility(allowed ? View.GONE : View.VISIBLE);
        if (mainButton != null) mainButton.setText(active ? "Desactivar botón flotante" : "Activar botón flotante");
    }

    private void requestOverlay() {
        Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try { startActivity(permission); }
        catch (Exception ex) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
        }
    }

    private void onMainButton() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlay();
            return;
        }
        boolean active = Prefs.of(this).getBoolean(Prefs.BUBBLE_ACTIVE, false);
        Intent service = new Intent(this, TouchLockService.class);
        service.setAction(active ? TouchLockService.STOP : TouchLockService.SHOW);
        if (active) startService(service);
        else {
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
                else startService(service);
            } catch (Exception e) {
                Toast.makeText(this, "No se pudo activar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
            Toast.makeText(this, "¡Listo! Abre el video y toca el candado flotante.", Toast.LENGTH_LONG).show();
        }
        new android.os.Handler(getMainLooper()).postDelayed(this::refreshStatus, 350);
    }

    private abstract static class SimpleSeekListener implements SeekBar.OnSeekBarChangeListener {
        @Override public void onStartTrackingTouch(SeekBar bar) { }
        @Override public void onStopTrackingTouch(SeekBar bar) { }
    }
}
