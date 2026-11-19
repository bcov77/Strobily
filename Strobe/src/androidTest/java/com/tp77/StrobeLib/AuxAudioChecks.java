package com.tp77.StrobeLib;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Real AudioTrack lifecycle checks. All hardware playback uses zero-valued PCM (silence). */
public final class AuxAudioChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void await(BooleanSupplier predicate, String message) {
        long end = SystemClock.elapsedRealtime() + 6000;
        while (!predicate.getAsBoolean() && SystemClock.elapsedRealtime() < end) SystemClock.sleep(10);
        check(predicate.getAsBoolean(), message);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    private static AudioTrack track(AuxAudioOutput output) {
        Object session = field(output, "session");
        return session == null ? null : (AudioTrack)field(session, "track");
    }

    public static void run(Instrumentation runner, MainActivity activity, Consumer<String> passed) {
        AudioManager audio = (AudioManager)activity.getSystemService("audio");
        int originalVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int testVolume = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 3);
        AtomicInteger finished = new AtomicInteger();
        AtomicInteger interruptions = new AtomicInteger();
        AuxAudioOutput[] holder = new AuxAudioOutput[1];
        try {
            runner.runOnMainSync(() -> {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, testVolume, 0);
                holder[0] = new AuxAudioOutput(activity, new AuxAudioOutput.Listener() {
                    @Override public void onStarted() { }
                    @Override public void onFinished() { finished.incrementAndGet(); }
                    @Override public void onInterrupted(String message) { interruptions.incrementAndGet(); }
                });
                holder[0].setSettings(true, 0, 0, 1, true, false, 100);
                holder[0].setPlayback(new int[]{10000, 15000}, true, false, true);
            });
            AuxAudioOutput output = holder[0];
            check(output.isPlaying(), "AUX did not create a track");
            AudioTrack first = track(output);
            await(() -> first.getPlaybackHeadPosition() > 2000, "AudioTrack playback clock did not advance");
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    "AUX did not raise media volume");
            int head = first.getPlaybackHeadPosition();
            runner.runOnMainSync(() -> {
                output.setSettings(true, 0, 0, 2, true, false, 100);
                output.setPlayback(new int[]{20000, 30000}, true, false, true);
            });
            check(track(output) == first, "Live timing/stereo update replaced the AudioTrack");
            await(() -> first.getPlaybackHeadPosition() > head + 2000, "Live update stopped the audio clock");
            int underruns = first.getUnderrunCount();
            SystemClock.sleep(1200);
            check(first.getUnderrunCount() == underruns, "Steady AUX playback underruns");
            runner.runOnMainSync(output::stop);
            await(() -> first.getState() == AudioTrack.STATE_UNINITIALIZED, "Stop did not release track");
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume, "Stop did not restore original media volume");
            passed.accept("AUX hardware clock advances without steady-state underruns; live updates reuse track; Stop releases and restores volume");

            runner.runOnMainSync(() -> {
                output.setSettings(true, 0, 0, 1, false, false, 100);
                output.setPlayback(new int[]{10000, 10000}, true, false, true);
            });
            AudioTrack unboosted = track(output);
            await(() -> unboosted.getPlaybackHeadPosition() > 2000, "Unboosted audio did not play");
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume, "Unchecked max-volume setting raised media volume");
            runner.runOnMainSync(() -> {
                output.stop();
                output.setSettings(true, 0, 0, 1, true, false, 100);
            });
            passed.accept("Unchecked max-volume option leaves media volume unchanged");

            // Much shorter than the output buffer: must start, drain, and finish rather than hang.
            runner.runOnMainSync(() -> output.setPlayback(new int[]{1000, 1000}, true, false, false));
            await(() -> finished.get() == 1, "Sub-buffer finite burst did not drain");
            check(!output.isPlaying(), "Finite burst still active");
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume, "Finite burst did not restore volume");
            passed.accept("Two-millisecond finite AUX burst drains and releases instead of waiting forever for buffer startup");

            runner.runOnMainSync(() -> output.setPlayback(new int[]{50000, 50000}, false, true, false));
            AudioTrack torch = track(output);
            await(() -> torch.getPlaybackHeadPosition() > 2000, "AUX Torch did not play");
            runner.runOnMainSync(() -> {
                output.noteManualVolumeChange();
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, testVolume + 1, 0);
                output.stop();
            });
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume + 1, "Stop overwrote user's volume adjustment");
            runner.runOnMainSync(() -> {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, testVolume, 0);
                output.setPlayback(new int[]{10000, 10000}, true, false, true);
            });
            runner.runOnMainSync(() -> {
                Object session = field(output, "session");
                ((AudioManager.OnAudioFocusChangeListener)field(session, "focusListener"))
                        .onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
            });
            check(!output.isPlaying() && interruptions.get() == 1, "Focus loss did not interrupt AUX");
            runner.runOnMainSync(() -> {
                output.setSettings(true, 0, 0, 0, true, false, 100);
                output.setPlayback(new int[]{20000, 20000}, true, false, true);
            });
            check(!output.isPlaying(), "Settings/automatic timing update restarted interrupted AUX");
            runner.runOnMainSync(() -> {
                output.stop();
                output.setPlayback(new int[]{10000, 10000}, true, false, true);
            });
            check(output.isPlaying(), "Explicit restart did not resume AUX");
            runner.runOnMainSync(() -> {
                output.stop();
                output.setPlayback(new int[]{10000, 10000}, true, false, true);
                output.stop();
                output.setPlayback(new int[]{10000, 10000}, true, false, true);
            });
            AudioTrack replacement = track(output);
            await(() -> replacement.getPlaybackHeadPosition() > 2000, "Stale writer cleanup stopped replacement track");
            runner.runOnMainSync(output::stop);
            check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume, "Rapid restart corrupted saved volume");
            passed.accept("AUX Torch, manual volume preservation, focus-loss latch, explicit restart, and rapid replacement cleanup");
            boolean headphonesPresent = false;
            for (android.media.AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                headphonesPresent |= AuxAudioOutput.isHeadphoneType(device.getType());
            }
            if (!headphonesPresent) {
                runner.runOnMainSync(() -> {
                    output.setSettings(true, 0, 0, 1, true, true, 100);
                    output.setPlayback(new int[]{10000, 10000}, true, false, true);
                });
                check(!output.isPlaying(), "Headphone guard allowed speaker playback");
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) == testVolume,
                        "Blocked headphone playback changed media volume");
                runner.runOnMainSync(() -> output.setSettings(true, 0, 0, 1, false, false, 100));
                AudioTrack override = track(output);
                check(override != null, "Disabling headphone guard did not permit explicit speaker playback");
                await(() -> override.getPlaybackHeadPosition() > 2000, "Speaker override did not play");
                runner.runOnMainSync(output::stop);
                passed.accept("Headphone guard prevents track creation/volume boost without headphones; unchecked guard permits playback");
            }

        } finally {
            runner.runOnMainSync(() -> {
                if (holder[0] != null) holder[0].close();
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
            });
        }
        serviceChecks(runner, activity, passed);
    }

    private static void serviceChecks(Instrumentation runner, MainActivity activity, Consumer<String> passed) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        AudioManager audio = (AudioManager)activity.getSystemService("audio");
        int volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        String[] keys = {AdvancedFragment.P_SHOW_AUX, AdvancedFragment.P_AUX_HIGH, AdvancedFragment.P_AUX_LOW,
                MainActivity.P_USE_AUX, MainActivity.P_USE_LED, MainActivity.P_USE_SCREEN, SettingsFragment.P_PERSIST,
                AdvancedFragment.P_AUX_HEADPHONES, AdvancedFragment.P_AUX_MAX_VOLUME, AdvancedFragment.P_AUX_VOLUME_KEYS};
        Map<String, Object> saved = new HashMap<>();
        for (String key : keys) saved.put(key, prefs.getAll().get(key));
        StrobeService service = activity.mService;
        try {
            runner.runOnMainSync(() -> {
                activity.stopFlashing();
                prefs.edit().putInt(AdvancedFragment.P_AUX_HIGH, 0).putInt(AdvancedFragment.P_AUX_LOW, 0)
                        .putBoolean(AdvancedFragment.P_SHOW_AUX, true).putBoolean(MainActivity.P_USE_AUX, true)
                        .putBoolean(MainActivity.P_USE_LED, false).putBoolean(MainActivity.P_USE_SCREEN, false)
                        .putBoolean(SettingsFragment.P_PERSIST, true)
                        .putBoolean(AdvancedFragment.P_AUX_HEADPHONES, false)
                        .putBoolean(AdvancedFragment.P_AUX_MAX_VOLUME, true)
                        .putBoolean(AdvancedFragment.P_AUX_VOLUME_KEYS, true).commit();
                // Same direct service path used by music's worker.
                service.controlUpdate(new int[]{10000, 10000}, true, false, true, MainActivity.S_OTHER);
            });
            await(service::isAuxPlaying, "Service did not start AUX-only audio");
            runner.runOnMainSync(() -> {
                int before = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
                int[] durations = service.mFlashes.clone();
                activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN));
                activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_DOWN));
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC) < before, "Volume button did not adjust media volume");
                check(java.util.Arrays.equals(durations, service.mFlashes), "Volume button changed flash frequency during AUX");
                activity.stopFlashing();
            });
            await(() -> !service.isAuxPlaying(), "Service Stop left AUX running");
            runner.runOnMainSync(() -> service.controlUpdate(new int[]{1000, 1000}, true, false, false, MainActivity.S_STROBE));
            await(() -> !service.mFlashing && !service.isAuxPlaying(), "Service/native short burst completion did not finish AUX");
            passed.accept("AUX-only service/music path, media volume buttons, and native/audio finite-burst completion cooperate");

            runner.runOnMainSync(() -> service.controlUpdate(new int[]{10000, 10000}, true, false, true, MainActivity.S_OTHER));
            runner.getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME);
            await(() -> service.mForeground, "AUX-only playback did not enter foreground in background");
            check(service.isAuxPlaying(), "AUX-only playback stopped when activity paused");
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                check((service.getForegroundServiceType() & android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0,
                        "AUX foreground service missing mediaPlayback type");
            }
            // Shell launch mirrors opening the launcher icon; an app launch from the background is restricted.
            try (java.io.InputStream command = new android.os.ParcelFileDescriptor.AutoCloseInputStream(
                    runner.getUiAutomation().executeShellCommand("am start -W -n " + activity.getPackageName()
                            + "/com.tp77.StrobeLib.MainActivity"))) {
                byte[] bytes = new byte[1024];
                while (command.read(bytes) != -1) { }
            } catch (java.io.IOException e) { throw new AssertionError(e); }
            await(() -> service.mBound, "Activity did not rebind to background AUX service");
            passed.accept("AUX-only playback survives backgrounding with mediaPlayback foreground-service type");
        } finally {
            runner.runOnMainSync(() -> {
                service.controlUpdate(null, false, false, false, MainActivity.S_OTHER);
                SharedPreferences.Editor editor = prefs.edit();
                for (String key : keys) {
                    Object value = saved.get(key);
                    if (value == null) editor.remove(key);
                    else if (value instanceof Boolean) editor.putBoolean(key, (Boolean)value);
                    else editor.putInt(key, (Integer)value);
                }
                editor.commit();
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
                activity.setLedScreenCheck();
                service.settingsUpdate();
            });
        }
    }
}
