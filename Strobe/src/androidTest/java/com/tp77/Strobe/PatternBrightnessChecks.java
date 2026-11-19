package com.tp77.Strobe;

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;

import com.tp77.StrobeLib.MainActivity;
import com.tp77.StrobeLib.PatPart;
import com.tp77.StrobeLib.PatternFragment;
import com.tp77.StrobeLib.PatternStorage;
import com.tp77.StrobeLib.SettingsFragment;
import com.tp77.StrobeLib.StrobeFragment;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

/** Migration fixtures, paired expansion, and isolated dialog edits; no rewrites of user rows. */
final class PatternBrightnessChecks {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static String versionOne(PatPart[] parts) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeInt(parts.length);
        out.writeInt(1);
        for (PatPart part : parts) {
            out.writeInt(part.mType);
            out.writeFloat(part.mFrequency);
            out.writeInt(part.mFlashes);
            out.writeInt(part.mDuty);
            out.writeInt(part.mTime);
            out.writeBoolean(part.mUseTime);
            out.writeUTF(part.mName);
        }
        out.close();
        return android.util.Base64.encodeToString(bytes.toByteArray(), 0);
    }

    private static int version(String payload) throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(android.util.Base64.decode(payload, 0)));
        try { in.readInt(); return in.readInt(); } finally { in.close(); }
    }

    private static PatPart pulse(MainActivity activity, int brightness, int repetitions) {
        PatPart part = new PatPart(activity);
        part.mBrightness = brightness;
        part.mFrequency = 10;
        part.mDuty = 50;
        part.mFlashes = repetitions;
        part.mUseTime = false;
        return part;
    }

    static void run(Instrumentation instrumentation, MainActivity activity, Consumer<String> passed) throws Exception {
        PatternStorage storage = PatternStorage.getInstance(activity);
        int oldRows = 0;
        try (Cursor rows = storage.mDb.rawQuery("select pattern from patterns", null)) {
            while (rows.moveToNext()) {
                String payload = rows.getString(0);
                PatPart[] parts = PatternStorage.stringToPattern(payload);
                check(parts != null, "Stored pattern failed to load");
                if (version(payload) == 1) {
                    oldRows++;
                    for (PatPart part : parts) check(part.mBrightness == -1, "Old part inherited custom brightness");
                }
            }
        }
        passed.accept("Existing version-1 database rows load with native brightness (" + oldRows + " rows)");

        PatPart first = pulse(activity, 0, 2);
        first.mName = "v1 multi-part fixture \u2603";
        PatPart second = pulse(activity, 100, 1);
        second.mFrequency = 7.25f;
        PatPart skip = pulse(activity, 75, 2);
        skip.mType = PatPart.T_FLASH_DELAY;
        PatPart delay = pulse(activity, 99, 1);
        delay.mType = PatPart.T_DELAY;
        delay.mTime = 123456;
        PatPart[] fixture = {first, second, skip, delay};
        PatPart[] old = PatternStorage.stringToPattern(versionOne(fixture));
        check(old != null && old.length == fixture.length, "Version-1 multi-part fixture did not load");
        for (int i = 0; i < fixture.length; i++) {
            check(old[i].mBrightness == -1, "Old brightness did not default to -1");
            check(old[i].mType == fixture[i].mType && old[i].mFrequency == fixture[i].mFrequency
                    && old[i].mFlashes == fixture[i].mFlashes && old[i].mTime == fixture[i].mTime
                    && old[i].mName.equals(fixture[i].mName), "Version-1 field alignment changed");
        }
        String upgraded = PatternStorage.patternToString(old);
        check(version(upgraded) == 2, "Resaving old pattern did not produce version 2");
        for (PatPart part : PatternStorage.stringToPattern(upgraded)) check(part.mBrightness == -1, "Resave changed old brightness");
        PatPart[] roundTrip = PatternStorage.stringToPattern(PatternStorage.patternToString(fixture));
        for (int i = 0; i < fixture.length; i++) {
            check(roundTrip[i].mBrightness == fixture[i].mBrightness, "Version-2 brightness round trip failed");
            check(new PatPart(roundTrip[i]).mBrightness == fixture[i].mBrightness, "Part copy lost brightness");
        }
        passed.accept("Version-1 multi-part fixtures migrate to -1; version-2 payloads and part copies retain brightness");

        String temporary = "__brightness_check_" + java.util.UUID.randomUUID();
        try {
            storage.addPattern(temporary, fixture);
            PatPart reference = pulse(activity, 42, 1);
            reference.mType = PatPart.T_PATTERN;
            reference.mName = temporary;
            PatPart.Playback nested = PatPart.buildPlayback(new PatPart[]{reference, pulse(activity, 50, 1)}, activity);
            check(Arrays.equals(nested.brightnesses, new int[]{0, 0, 100, -1, -1, -1, 50}),
                    "Nested brightness expansion lost pulse alignment");
            check(nested.durations.length == nested.brightnesses.length * 2, "Duration and brightness lengths differ");
            check(nested.durations[6] == -1 && nested.durations[10] == -1,
                    "Skip/delay events stopped being LED-free");
            check(storage.getPattern(temporary)[1].mBrightness == 100, "Database round trip lost brightness");
        } finally { storage.deletePattern(temporary); }
        passed.accept("Database round trip and nested patterns preserve per-flash alignment; skips/delays use -1");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        String draftKey = "pattern";
        // Use the actual key declared by PatternFragment, including its existing spelling.
        java.lang.reflect.Field key = PatternFragment.class.getDeclaredField("P_PATTERN");
        key.setAccessible(true);
        draftKey = (String)key.get(null);
        final String draftPreference = draftKey;
        final boolean hadDraft = prefs.contains(draftKey);
        final String draft = prefs.getString(draftKey, "");
        PatternFragment target = new PatternFragment();
        try {
            instrumentation.runOnMainSync(() -> {
                prefs.edit().putInt(StrobeFragment.P_BRIGHTNESS, 37)
                        .putBoolean(StrobeFragment.P_CUSTOM_BRIGHTNESS, true)
                        .putBoolean(SettingsFragment.P_SHOW_BRIGHTNESS, true).commit();
                check(new PatPart(activity).mBrightness == 37, "New part did not seed from main settings");
                activity.getSupportFragmentManager().beginTransaction().add(target, "pattern-test-target").commit();
                activity.getSupportFragmentManager().executePendingTransactions();
                target.mPattern = new PatPart[]{pulse(activity, -1, 1)};
            });
            PatternFragment.EditDialogFragment cancel = open(instrumentation, activity, target, "cancel-test");
            instrumentation.runOnMainSync(() -> {
                AlertDialog dialog = (AlertDialog)cancel.getDialog();
                check(!((CheckBox)dialog.findViewById(com.tp77.StrobeLib.R.id.custom_brightness_check)).isChecked(),
                        "Editing old part enabled custom brightness");
                ((CheckBox)dialog.findViewById(com.tp77.StrobeLib.R.id.custom_brightness_check)).performClick();
                changeSlider(dialog);
                dialog.findViewById(com.tp77.StrobeLib.R.id.test_button).performClick();
                check(activity.mService.mBrightnesses[0] == 25, "Part preview ignored local brightness");
                activity.stopFlashing();
                check(prefs.getInt(StrobeFragment.P_BRIGHTNESS, -1) == 37, "Dialog wrote global brightness");
                check(prefs.getBoolean(StrobeFragment.P_CUSTOM_BRIGHTNESS, false), "Dialog wrote global checkbox");
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            });
            instrumentation.waitForIdleSync();
            check(target.mPattern[0].mBrightness == -1, "Cancel changed the original part");

            PatternFragment.EditDialogFragment save = open(instrumentation, activity, target, "save-test");
            instrumentation.runOnMainSync(() -> {
                AlertDialog dialog = (AlertDialog)save.getDialog();
                ((CheckBox)dialog.findViewById(com.tp77.StrobeLib.R.id.custom_brightness_check)).performClick();
                changeSlider(dialog);
            });
            instrumentation.waitForIdleSync();
            android.graphics.Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            if (screenshot != null) {
                try (java.io.FileOutputStream output = new java.io.FileOutputStream(
                        new java.io.File(activity.getCacheDir(), "pattern-editor-brightness.png"))) {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
                } finally { screenshot.recycle(); }
            }
            instrumentation.runOnMainSync(() -> ((AlertDialog)save.getDialog())
                    .getButton(AlertDialog.BUTTON_POSITIVE).performClick());
            instrumentation.waitForIdleSync();
            check(target.mPattern[0].mBrightness == 25, "OK did not save the part's custom brightness");
            check(PatternStorage.stringToPattern(prefs.getString(draftPreference, ""))[0].mBrightness == 25,
                    "Current draft did not persist part brightness");
            instrumentation.runOnMainSync(() -> prefs.edit().putBoolean(SettingsFragment.P_SHOW_BRIGHTNESS, false).commit());
            PatternFragment.EditDialogFragment hidden = open(instrumentation, activity, target, "hidden-test");
            instrumentation.runOnMainSync(() -> {
                AlertDialog dialog = (AlertDialog)hidden.getDialog();
                check(dialog.findViewById(com.tp77.StrobeLib.R.id.brightness_section).getVisibility() == View.GONE,
                        "Show brightness setting did not hide dialog controls");
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            instrumentation.waitForIdleSync();
            check(target.mPattern[0].mBrightness == 25, "Hidden controls erased baked-in brightness");
            check(new PatPart(activity).mBrightness == -1, "New hidden-controls part inherited custom brightness");
            passed.accept("Dialog edits stay local; Cancel discards, OK saves, and hidden controls preserve baked-in brightness");
        } finally {
            instrumentation.runOnMainSync(() -> {
                for (android.support.v42.app.Fragment fragment : activity.getSupportFragmentManager().getFragments()) {
                    if (fragment instanceof PatternFragment.EditDialogFragment) ((PatternFragment.EditDialogFragment)fragment).dismissAllowingStateLoss();
                }
                activity.getSupportFragmentManager().executePendingTransactions();
                if (target.isAdded()) {
                    target.mPattern = PatternStorage.stringToPattern(draft);
                    if (target.mPattern == null) target.mPattern = new PatPart[0];
                    activity.getSupportFragmentManager().beginTransaction().remove(target).commit();
                    activity.getSupportFragmentManager().executePendingTransactions();
                }
                SharedPreferences.Editor edit = prefs.edit();
                if (hadDraft) edit.putString(draftPreference, draft); else edit.remove(draftPreference);
                edit.commit();
            });
        }
    }

    private static PatternFragment.EditDialogFragment open(Instrumentation instrumentation, MainActivity activity,
                                                           PatternFragment target, String tag) {
        PatternFragment.EditDialogFragment dialog = new PatternFragment.EditDialogFragment();
        instrumentation.runOnMainSync(() -> {
            dialog.setTargetFragment(target, 0);
            dialog.show(activity.getSupportFragmentManager(), tag);
            activity.getSupportFragmentManager().executePendingTransactions();
        });
        instrumentation.waitForIdleSync();
        return dialog;
    }

    private static void changeSlider(AlertDialog dialog) {
        SeekBar slider = (SeekBar)dialog.findViewById(com.tp77.StrobeLib.R.id.brightness_seek_bar);
        slider.setProgress(20);
        slider.setKeyProgressIncrement(5);
        slider.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
    }
}
