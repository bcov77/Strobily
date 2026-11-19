package com.tp77.Strobe;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import com.tp77.StrobeLib.AdvancedFragment;
import com.tp77.StrobeLib.MainActivity;
import com.tp77.StrobeLib.R;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Output selection regression checks; restores the user's AUX and output preferences. */
final class AuxOutputChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static void run(Instrumentation runner, MainActivity activity, Consumer<String> passed) throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String[] keys = {AdvancedFragment.P_SHOW_AUX, AdvancedFragment.P_AUX_HIGH,
                AdvancedFragment.P_AUX_LOW, AdvancedFragment.P_AUX_STEREO,
                MainActivity.P_USE_AUX, MainActivity.P_USE_LED, MainActivity.P_USE_SCREEN,
                AdvancedFragment.P_AUX_MAX_VOLUME, AdvancedFragment.P_AUX_VOLUME_KEYS, AdvancedFragment.P_AUX_HEADPHONES};
        Map<String, Object> saved = new HashMap<>();
        for (String key : keys) saved.put(key, prefs.getAll().get(key));
        try {
            runner.runOnMainSync(() -> {
                for (String key : keys) prefs.edit().remove(key).commit();
                activity.setPage(MainActivity.F_ADVANCED);
            });
            runner.waitForIdleSync();
            runner.runOnMainSync(() -> {
                AdvancedFragment fragment = (AdvancedFragment)activity.getCurrentFragment();
                fragment.updateSliders(true);
                View view = fragment.getView();
                check(view.findViewById(R.id.aux_section).getVisibility() == View.GONE, "AUX options initially visible");
                view.findViewById(R.id.aux_mode_check).performClick();
                check(view.findViewById(R.id.aux_section).getVisibility() == View.VISIBLE, "AUX options did not open");
                check(activity.findViewById(R.id.aux_check).getVisibility() == View.VISIBLE, "AUX output did not appear");
                check(((CheckBox)view.findViewById(R.id.aux_max_volume_check)).isChecked(), "Max volume default not enabled");
                check(!((CheckBox)view.findViewById(R.id.aux_volume_keys_check)).isChecked(), "Volume keys default should adjust frequency");
                check(((CheckBox)view.findViewById(R.id.aux_headphones_check)).isChecked(), "Headphones default not enabled");
                view.findViewById(R.id.aux_max_volume_check).performClick();
                view.findViewById(R.id.aux_volume_keys_check).performClick();
                view.findViewById(R.id.aux_headphones_check).performClick();
                fragment.updateSliders(true);
                check(!prefs.getBoolean(AdvancedFragment.P_AUX_MAX_VOLUME, true), "Max volume checkbox not saved");
                check(prefs.getBoolean(AdvancedFragment.P_AUX_VOLUME_KEYS, false), "Volume keys checkbox not saved");
                check(!prefs.getBoolean(AdvancedFragment.P_AUX_HEADPHONES, true), "Headphones checkbox not saved");
                check(((SeekBar)view.findViewById(R.id.aux_high_seek_bar)).getProgress() == 200, "High default != +1");
                check(((SeekBar)view.findViewById(R.id.aux_low_seek_bar)).getProgress() == 100, "Low default != 0");
                check(((SeekBar)view.findViewById(R.id.aux_stereo_seek_bar)).getMax() == 2, "Stereo is not three positions");
                change(view, R.id.aux_high_seek_bar, 0, KeyEvent.KEYCODE_DPAD_LEFT);
                check(prefs.getInt(AdvancedFragment.P_AUX_HIGH, 999) == -100, "High minimum not saved");
                change(view, R.id.aux_low_seek_bar, 200, KeyEvent.KEYCODE_DPAD_RIGHT);
                check(prefs.getInt(AdvancedFragment.P_AUX_LOW, 999) == 100, "Low maximum not saved");
                change(view, R.id.aux_stereo_seek_bar, 2, KeyEvent.KEYCODE_DPAD_RIGHT);
                check(prefs.getInt(AdvancedFragment.P_AUX_STEREO, -1) == 2, "Stereo not saved");
                fragment.updateSliders(true);
                check(((SeekBar)view.findViewById(R.id.aux_high_seek_bar)).getProgress() == 0, "Voltage refresh lost saved value");
            });
            runner.waitForIdleSync();
            passed.accept("AUX controls open, defaults match, voltage endpoints and stereo persist");
            runner.runOnMainSync(() -> {
                int[] ids = {R.id.led_check, R.id.screen_check, R.id.aux_check};
                for (int mask = 1; mask < 8; mask++) {
                    for (int toggle = 0; toggle < 3; toggle++) {
                        prefs.edit().putBoolean(MainActivity.P_USE_LED, (mask & 1) != 0)
                                .putBoolean(MainActivity.P_USE_SCREEN, (mask & 2) != 0)
                                .putBoolean(MainActivity.P_USE_AUX, (mask & 4) != 0).commit();
                        activity.setLedScreenCheck();
                        activity.findViewById(ids[toggle]).performClick();
                        int expected = mask ^ (1 << toggle);
                        if (expected == 0) expected = 1;
                        int actual = (activity.useLed() ? 1 : 0) | (activity.useScreen() ? 2 : 0) | (activity.useAux() ? 4 : 0);
                        check(actual == expected, "Output selection mismatch: " + mask + " toggle " + toggle);
                        for (int i = 0; i < 3; i++) check(((CheckBox)activity.findViewById(ids[i])).isChecked()
                                == ((expected & (1 << i)) != 0), "Output checkbox mismatch");
                    }
                }
                prefs.edit().putBoolean(MainActivity.P_USE_LED, false)
                        .putBoolean(MainActivity.P_USE_SCREEN, false).putBoolean(MainActivity.P_USE_AUX, true).commit();
                View advanced = activity.getCurrentFragment().getView();
                advanced.findViewById(R.id.aux_mode_check).performClick();
                check(activity.useLed() && !activity.useAux(), "Hiding AUX-only mode did not restore LED");
                check(activity.findViewById(R.id.aux_check).getVisibility() == View.GONE, "Hidden AUX output still visible");
                // Existing two-output switching still works with AUX hidden.
                activity.handleLedScreenPress(true, false);
                check(!activity.useLed() && activity.useScreen(), "Legacy LED-to-screen selection changed");
                activity.handleLedScreenPress(false, false);
                check(activity.useLed() && !activity.useScreen(), "Legacy screen-to-LED selection changed");
            });
            passed.accept("All 21 AUX output transitions, AUX-only playback selection, LED fallback, and hidden-mode behavior");
        } finally {
            runner.runOnMainSync(() -> {
                SharedPreferences.Editor editor = prefs.edit();
                for (String key : keys) {
                    Object value = saved.get(key);
                    if (value == null) editor.remove(key);
                    else if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
                    else editor.putInt(key, (Integer)value);
                }
                editor.commit();
                activity.setLedScreenCheck();
                activity.mService.settingsUpdate();
                activity.updateSliders();
            });
        }
    }

    private static void change(View view, int id, int target, int direction) {
        SeekBar slider = (SeekBar)view.findViewById(id);
        slider.setKeyProgressIncrement(1);
        slider.setProgress(target + (direction == KeyEvent.KEYCODE_DPAD_LEFT ? 1 : -1));
        slider.onKeyDown(direction, new KeyEvent(KeyEvent.ACTION_DOWN, direction));
    }
}
