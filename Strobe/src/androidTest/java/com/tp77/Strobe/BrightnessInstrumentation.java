package com.tp77.Strobe;

import android.app.Instrumentation;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.tp77.StrobeLib.MainActivity;
import com.tp77.StrobeLib.SettingsFragment;
import com.tp77.StrobeLib.StrobeFragment;
import com.tp77.StrobeLib.StrobeService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/** Device test, no external runner dependencies. Requires a strength-capable camera (API 33+). */
@android.annotation.SuppressLint("NewApi")
public class BrightnessInstrumentation extends Instrumentation {
    private MainActivity activity;
    private SharedPreferences preferences;
    private CameraManager manager;
    private String camera;
    private int maximum;
    private int normal;
    private volatile boolean torchLit;
    private final java.util.concurrent.atomic.AtomicInteger torchOffTransitions = new java.util.concurrent.atomic.AtomicInteger();
    private final CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {
        @Override public void onTorchModeChanged(String id, boolean enabled) {
            if (id.equals(camera)) {
                if (torchLit && !enabled) torchOffTransitions.incrementAndGet();
                torchLit = enabled;
            }
        }
    };
    private final StringBuilder results = new StringBuilder();

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void waitFor(BooleanSupplier condition, String message) {
        long end = SystemClock.uptimeMillis() + 5000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < end) SystemClock.sleep(25);
        check(condition.getAsBoolean(), message + (manager == null ? "" : " (strength=" + strength()
                + ", lit=" + torchLit + ", torch=" + activity.mService.mTorch + ")"));
    }

    private int strength() {
        try { return manager.getTorchStrengthLevel(camera); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    private boolean litAt(int level) { return torchLit && strength() == level; }

    private void brightness(int percent, boolean custom, boolean shown) {
        runOnMainSync(() -> preferences.edit()
                .putInt(StrobeFragment.P_BRIGHTNESS, percent)
                .putBoolean(StrobeFragment.P_CUSTOM_BRIGHTNESS, custom)
                .putBoolean(SettingsFragment.P_SHOW_BRIGHTNESS, shown).commit());
    }

    private void stopLed() {
        runOnMainSync(() -> activity.stopFlashing());
        SystemClock.sleep(200);
    }

    private void start(int[] timing, int[] brightness, int who) {
        runOnMainSync(() -> activity.startFlashing(timing, brightness, true, who));
    }

    private void passed(String message) {
        results.append("PASS: ").append(message).append('\n');
        Bundle status = new Bundle();
        status.putString("stream", "PASS: " + message + "\n");
        sendStatus(0, status);
    }

    @Override public void onStart() {
        Bundle output = new Bundle();
        int code = -1;
        Map<String, Object> saved = new HashMap<>();
        try {
            preferences = PreferenceManager.getDefaultSharedPreferences(getTargetContext());
            String[] keys = {StrobeFragment.P_BRIGHTNESS, StrobeFragment.P_CUSTOM_BRIGHTNESS,
                    SettingsFragment.P_SHOW_BRIGHTNESS, MainActivity.P_NEW_CAMERA,
                    MainActivity.P_USE_LED, MainActivity.P_USE_SCREEN, MainActivity.P_USE_AUX};
            Map<String, ?> all = preferences.getAll();
            for (String key : keys) saved.put(key, all.get(key));
            preferences.edit().putBoolean(MainActivity.P_NEW_CAMERA, true)
                    .putBoolean(MainActivity.P_USE_LED, true)
                    .putBoolean(MainActivity.P_USE_SCREEN, false).putBoolean(MainActivity.P_USE_AUX, false).commit();
            brightness(100, true, true);
            Intent launch = new Intent(getTargetContext(), MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = (MainActivity)startActivitySync(launch);
            waitFor(() -> activity.mService != null && activity.mService.mWhichCamera != null
                    && activity.mService.isFlashReady(), "Camera/service did not become ready");
            AuxOutputChecks.run(this, activity, this::passed);
            com.tp77.StrobeLib.AuxAudioChecks.run(this, activity, this::passed);
            PatternBrightnessChecks.run(this, activity, this::passed);
            camera = activity.mService.mWhichCamera;
            manager = (CameraManager)getTargetContext().getSystemService("camera");
            CameraCharacteristics info = manager.getCameraCharacteristics(camera);
            maximum = info.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
            normal = info.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL);
            check(maximum > 1, "This device does not support torch strength control");
            manager.registerTorchCallback(torchCallback, new android.os.Handler(android.os.Looper.getMainLooper()));

            brightness(100, true, true);
            // Strength changes must bypass the ON guard without restarting continuous torch mode.
            runOnMainSync(() -> activity.torchOn());
            waitFor(() -> litAt(maximum), "Torch did not start at custom maximum");
            passed("Torch starts at scaled custom strength " + maximum);
            // Let the existing counter exceed maxOn before changing brightness.
            SystemClock.sleep(450);
            int offBeforeStrengthChanges = torchOffTransitions.get();
            brightness(0, true, true);
            waitFor(() -> litAt(1), "Already-lit torch did not pick up minimum strength");
            brightness(50, true, true);
            int middle = Math.max(1, Math.round(maximum * 0.5f));
            waitFor(() -> litAt(middle), "Already-lit torch did not pick up middle strength");
            brightness(100, true, true);
            waitFor(() -> litAt(maximum), "Already-lit torch did not return to maximum strength");
            check(torchOffTransitions.get() == offBeforeStrengthChanges,
                    "Changing custom torch strength introduced an OFF transition");
            passed("Live torch updates bypass the counter without turning the light off");
            brightness(100, false, true);
            waitFor(() -> litAt(normal), "Unchecking custom brightness did not restore native strength");
            brightness(0, true, true);
            waitFor(() -> litAt(1), "Rechecking custom brightness did not update lit torch");
            brightness(0, true, false);
            waitFor(() -> litAt(normal), "Hiding brightness controls did not restore native strength");
            passed("Unchecked or hidden custom brightness restores native torch strength");
            stopLed();

            // Long ON phases make strength observable without depending on flash timing precision.
            brightness(0, true, true);
            start(new int[]{600000, 150000}, null, MainActivity.S_STROBE);
            waitFor(() -> litAt(1), "Strobe did not use global minimum");
            brightness(100, true, true);
            waitFor(() -> litAt(maximum), "Running strobe did not pick up global change");
            passed("Global strobe brightness changes during playback");
            brightness(0, false, true);
            waitFor(() -> litAt(normal), "Unchecked custom brightness did not use native strength");
            brightness(0, true, false);
            waitFor(() -> litAt(normal), "Hidden controls still applied custom strength");
            brightness(0, true, true);
            waitFor(() -> litAt(1), "Reenabled controls did not restore saved brightness");
            passed("Both checkboxes gate live global brightness");
            stopLed();

            for (int source : new int[]{MainActivity.S_TACHO, MainActivity.S_OTHER}) {
                brightness(0, true, true);
                start(new int[]{600000, 150000}, null, source);
                waitFor(() -> litAt(1), "Global brightness not applied to source " + source);
                stopLed();
            }
            passed("Tacho and shared music/turntable playback use global brightness");

            brightness(0, true, true);
            runOnMainSync(() -> activity.startPatternFlashing(new int[]{600000, 150000}, true, MainActivity.S_OTHER));
            waitFor(() -> litAt(normal), "Pattern inherited global brightness");
            check(activity.mService.mBrightnesses[0] == -1, "Pattern lost native sentinel");
            passed("Existing patterns explicitly use native brightness");
            stopLed();

            brightness(0, false, false);
            runOnMainSync(() -> {
                com.tp77.StrobeLib.PatPart[] parts = new com.tp77.StrobeLib.PatPart[3];
                int[] percentages = {0, 100, -1};
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = new com.tp77.StrobeLib.PatPart(activity);
                    parts[i].mFrequency = 1000000f / 750000f;
                    parts[i].mUseTime = true;
                    parts[i].mTime = 600000;
                    parts[i].mBrightness = percentages[i];
                }
                activity.startPatternFlashing(parts, true, MainActivity.S_OTHER);
            });
            waitFor(() -> litAt(1), "Per-flash minimum missing");
            waitFor(() -> litAt(maximum), "Per-flash maximum missing");
            waitFor(() -> litAt(normal), "Per-flash native sentinel missing");
            brightness(50, true, true);
            waitFor(() -> litAt(1), "Global settings overrode explicit array");
            waitFor(() -> litAt(maximum), "Array changed after global update");
            passed("Per-flash array overrides global settings, including hidden/unchecked controls");
            brightness(0, true, true);
            runOnMainSync(() -> activity.updateIfRunning(new int[]{600000, 150000}, true));
            waitFor(() -> litAt(1), "Normal timing update retained a stale pattern array");
            check(activity.mService.mBrightnesses == null, "Normal controls retained pattern brightness");
            passed("Normal timing controls switch back to global brightness without stale array lengths");
            stopLed();

            // Music's beat worker calls the service directly, rather than MainActivity.
            brightness(0, true, true);
            runOnMainSync(() -> activity.mService.controlUpdate(new int[]{600000, 150000},
                    true, false, true, MainActivity.S_OTHER));
            waitFor(() -> litAt(1), "Direct music service path lost global brightness");
            passed("Direct service updates use live global brightness");
            stopLed();

            java.util.concurrent.atomic.AtomicInteger screenOn = new java.util.concurrent.atomic.AtomicInteger();
            java.util.concurrent.atomic.AtomicInteger screenOff = new java.util.concurrent.atomic.AtomicInteger();
            android.os.Handler original = activity.mService.mScreenPost;
            runOnMainSync(() -> {
                activity.mService.mScreenPost = new android.os.Handler(android.os.Looper.getMainLooper()) {
                    @Override public void handleMessage(android.os.Message message) {
                        if (message.what == com.tp77.StrobeLib.StrobeLibActivity.SCREEN_ON) screenOn.incrementAndGet();
                        if (message.what == com.tp77.StrobeLib.StrobeLibActivity.SCREEN_OFF) screenOff.incrementAndGet();
                        original.sendEmptyMessage(message.what);
                    }
                };
                preferences.edit().putBoolean(MainActivity.P_USE_LED, false)
                        .putBoolean(MainActivity.P_USE_SCREEN, true).commit();
                activity.mService.settingsUpdate();
            });
            brightness(0, true, true);
            start(new int[]{150000, 150000}, new int[]{0}, MainActivity.S_OTHER);
            waitFor(() -> screenOn.get() >= 3 && screenOff.get() >= 3,
                    "LED brightness affected screen flashing");
            stopLed();
            runOnMainSync(() -> {
                activity.mService.mScreenPost = original;
                preferences.edit().putBoolean(MainActivity.P_USE_LED, true)
                        .putBoolean(MainActivity.P_USE_SCREEN, false).commit();
                activity.mService.settingsUpdate();
            });
            passed("Screen pulses continue independently of LED brightness");

            boolean invalid = false;
            try {
                activity.mService.controlUpdate(new int[]{1000, 1000}, new int[]{1, 2},
                        true, false, true, MainActivity.S_OTHER);
            } catch (IllegalArgumentException expected) { invalid = true; }
            check(invalid, "Mismatched arrays accepted");
            passed("Mismatched timing/brightness arrays are rejected");
        } catch (Throwable e) {
            code = 0;
            results.append("FAIL: ").append(android.util.Log.getStackTraceString(e));
        } finally {
            if (activity != null) {
                try { stopLed(); } catch (Throwable ignored) { }
            }
            if (preferences != null) {
                runOnMainSync(() -> {
                    SharedPreferences.Editor editor = preferences.edit();
                    for (Map.Entry<String, Object> entry : saved.entrySet()) {
                        Object value = entry.getValue();
                        if (value == null) editor.remove(entry.getKey());
                        else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean)value);
                        else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer)value);
                    }
                    editor.commit();
                });
            }
            if (manager != null) manager.unregisterTorchCallback(torchCallback);
            if (activity != null) runOnMainSync(() -> activity.finish());
            output.putString("stream", results.toString());
            finish(code, output);
        }
    }
}
