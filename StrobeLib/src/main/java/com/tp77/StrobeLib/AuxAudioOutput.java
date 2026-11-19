package com.tp77.StrobeLib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/** Independent Java AUX player. Lifecycle/focus/volume live on main; only the writer touches PCM. */
final class AuxAudioOutput {
    interface Listener {
        void onStarted();
        void onFinished();
        void onInterrupted(String message);
    }

    private static final String TAG = "AuxAudioOutput";
    private final Context context;
    private final AudioManager audio;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private final AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private AuxWaveform.Plan plan;
    private AuxWaveform.Levels levels = new AuxWaveform.Levels(100, 0, 1);
    private boolean selected;
    private boolean playing;
    private boolean interrupted;
    private boolean closed;
    private boolean maxVolumeOnStart = true;
    private boolean requireHeadphones = true;
    private boolean headphonesBlocked;
    private volatile Session session;
    private int savedVolume;
    private int appliedVolume;
    private boolean changedVolume;
    private boolean userChangedVolume;

    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent intent) {
            interrupt("AUX stopped: audio output disconnected");
        }
    };
    private final ContentObserver volumeObserver = new ContentObserver(main) {
        @Override public void onChange(boolean selfChange) {
            if (changedVolume && audio.getStreamVolume(AudioManager.STREAM_MUSIC) != appliedVolume) {
                noteManualVolumeChange();
            }
        }
    };
    private AudioDeviceCallback deviceCallback;

    AuxAudioOutput(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
        audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(noisy, filter, Context.RECEIVER_EXPORTED);
        else context.registerReceiver(noisy, filter);
        context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver);
        if (Build.VERSION.SDK_INT >= 23) {
            deviceCallback = new AudioDeviceCallback() {
                @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
                    Session current = session;
                    if (current == null) return;
                    for (AudioDeviceInfo device : removed) {
                        if (device.isSink() && device.getId() == current.deviceId) {
                            current.restoreVolume = false;
                            interrupt("AUX stopped: audio output disconnected");
                            break;
                        }
                    }
                }
            };
            audio.registerAudioDeviceCallback(deviceCallback, main);
        }
    }

    boolean isPlaying() { return session != null; }

    void setSettings(boolean enabled, int high, int low, int stereo, boolean maxVolume, boolean headphones, int brightness) {
        onMain(() -> {
            if (closed) return;
            if ((enabled && !selected) || (!headphones && headphonesBlocked)) {
                interrupted = false;
                headphonesBlocked = false;
            }
            selected = enabled;
            maxVolumeOnStart = maxVolume;
            requireHeadphones = headphones;
            levels = new AuxWaveform.Levels(high, low, stereo, brightness);
            if (session != null) {
                session.levels = levels;
                session.requireHeadphones = headphones;
                if (enabled && headphones) {
                    if (Build.VERSION.SDK_INT >= 23) checkRoute(session);
                    else if (!audio.isWiredHeadsetOn()) blockForHeadphones();
                } else if (enabled) {
                    try {
                        prepareVolume(session);
                        session.routeReady = true;
                    } catch (RuntimeException e) {
                        Log.w(TAG, "Cannot prepare AUX volume", e);
                        interrupt("AUX stopped: media volume unavailable");
                    }
                }
            }
            reconcile();
        });
    }

    void setPlayback(int[] durations, boolean flashing, boolean torch, boolean loop) {
        AuxWaveform.Plan next = flashing || torch ? new AuxWaveform.Plan(durations, loop, torch) : null;
        onMain(() -> {
            if (closed) return;
            boolean active = flashing || torch;
            if (active && !playing) {
                interrupted = false;
                headphonesBlocked = false;
            }
            playing = active;
            plan = next;
            if (session != null && active) session.plan = next;
            reconcile();
        });
    }

    void noteManualVolumeChange() {
        onMain(() -> {
            if (session == null) return;
            userChangedVolume = true;
        });
    }

    private void onMain(Runnable action) {
        if (Looper.myLooper() == main.getLooper()) action.run();
        else main.post(action);
    }

    private void reconcile() {
        if (!selected || !playing || interrupted || closed) { stopSession(); return; }
        if (session != null || plan == null) return;
        AudioDeviceInfo headphones = null;
        if (requireHeadphones) {
            if (Build.VERSION.SDK_INT >= 23) headphones = findHeadphones();
            boolean detected = Build.VERSION.SDK_INT >= 23 ? headphones != null : audio.isWiredHeadsetOn();
            if (!detected) { blockForHeadphones(); return; }
        }
        Session next = null;
        boolean writerStarted = false;
        try {
            int rate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
            if (rate <= 0) rate = 48000;
            int minimum = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) throw new IllegalStateException("Stereo PCM output unavailable");
            // Prefer a reliable ~100 ms queue over minimal latency. Updates are written in ~20 ms chunks.
            int bufferBytes = Math.max(minimum, rate / 10 * 4);
            AudioTrack track = new AudioTrack(attributes, new AudioFormat.Builder().setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).build(),
                    bufferBytes, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            next = new Session(track, rate, bufferBytes / 4, plan, levels);
            session = next;
            if (track.getState() != AudioTrack.STATE_INITIALIZED) throw new IllegalStateException("AudioTrack initialization failed");
            next.requireHeadphones = requireHeadphones;
            next.routeReady = !requireHeadphones || Build.VERSION.SDK_INT < 23;
            if (Build.VERSION.SDK_INT >= 23 && headphones != null && !track.setPreferredDevice(headphones)) {
                throw new IllegalStateException("Headphone output unavailable");
            }
            if (!requestFocus(next)) throw new IllegalStateException("Audio focus unavailable");
            if (Build.VERSION.SDK_INT >= 23) {
                final Session routed = next;
                routed.routeListener = audioTrack -> checkRoute(routed);
                track.addOnRoutingChangedListener(routed.routeListener, main);
            }
            changedVolume = false;
            userChangedVolume = false;
            if (next.routeReady) prepareVolume(next);
            track.setVolume(1f);
            final Session writer = next;
            new Thread(() -> writeAudio(writer), "Strobily AUX audio").start();
            writerStarted = true;
            listener.onStarted();
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot start AUX", e);
            // A started writer releases its own track after it exits.
            stopSession();
            if (next != null && !writerStarted) next.track.release();
            interrupted = true;
            listener.onInterrupted("AUX could not start: " + e.getMessage());
        }
    }

    // AUX controls a wired/USB connection. Bluetooth speakers and built-in speakers do not qualify.
    static boolean isHeadphoneType(int type) {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                || type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_ACCESSORY
                || type == AudioDeviceInfo.TYPE_USB_HEADSET;
    }

    private AudioDeviceInfo findHeadphones() {
        for (AudioDeviceInfo device : audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isHeadphoneType(device.getType())) return device;
        }
        return null;
    }

    private void blockForHeadphones() {
        headphonesBlocked = true;
        interrupted = true;
        stopSession();
        listener.onInterrupted("AUX stopped: connect wired headphones or a USB audio output");
    }

    private void prepareVolume(Session current) {
        if (current.volumeHandled) return;
        current.volumeHandled = true;
        if (maxVolumeOnStart) raiseVolume();
    }

    private void checkRoute(Session current) {
        if (session != current || Build.VERSION.SDK_INT < 23) return;
        try {
            checkConfirmedRoute(current);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot confirm AUX route/volume", e);
            interrupt("AUX stopped: headphone route or media volume unavailable");
        }
    }

    private void checkConfirmedRoute(Session current) {
        AudioDeviceInfo route = current.track.getRoutedDevice();
        if (route == null) {
            if (current.requireHeadphones && current.routeReady) blockForHeadphones();
            return;
        }
        if (current.requireHeadphones && !isHeadphoneType(route.getType())) {
            current.restoreVolume = false;
            blockForHeadphones();
            return;
        }
        int previous = current.deviceId;
        current.deviceId = route.getId();
        if (previous != -1 && previous != current.deviceId) {
            current.restoreVolume = false;
            interrupt("AUX stopped: audio output changed");
            return;
        }
        prepareVolume(current);
        current.routeReady = true;
    }

    private boolean requestFocus(Session current) {
        current.focusListener = change -> onMain(() -> {
            if (session == current && change < AudioManager.AUDIOFOCUS_GAIN) {
                interrupt("AUX stopped: audio focus lost");
            }
        });
        if (Build.VERSION.SDK_INT >= 26) {
            current.focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(current.focusListener, main).build();
            return audio.requestAudioFocus(current.focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return audio.requestAudioFocus(current.focusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void raiseVolume() {
        userChangedVolume = false;
        changedVolume = false;
        if (audio.isVolumeFixed()) return;
        savedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        appliedVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        changedVolume = true;
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, appliedVolume, 0);
        appliedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    private void stopSession() {
        Session old = session;
        if (old == null) return;
        session = null;
        old.running = false;
        if (Build.VERSION.SDK_INT >= 23 && old.deviceId != -1) {
            try {
                AudioDeviceInfo route = old.track.getRoutedDevice();
                // Never apply the old headphone volume to a newly selected speaker/other output.
                if (route == null || route.getId() != old.deviceId) old.restoreVolume = false;
            } catch (IllegalStateException e) {
                old.restoreVolume = false;
                Log.w(TAG, "Audio route unavailable during stop", e);
            }
        }
        if (Build.VERSION.SDK_INT >= 23 && old.routeListener != null) {
            old.track.removeOnRoutingChangedListener(old.routeListener);
        }
        // pause/flush interrupts blocking writes and discards queued samples for explicit stops.
        try { old.track.pause(); old.track.flush(); }
        catch (IllegalStateException e) { Log.w(TAG, "Stopping AUX track", e); }
        try {
            if (old.restoreVolume && changedVolume && !userChangedVolume
                    && audio.getStreamVolume(AudioManager.STREAM_MUSIC) == appliedVolume) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0);
            }
        } catch (RuntimeException e) { Log.w(TAG, "Restoring media volume", e); }
        changedVolume = false;
        if (Build.VERSION.SDK_INT >= 26 && old.focusRequest != null) audio.abandonAudioFocusRequest(old.focusRequest);
        else if (old.focusListener != null) audio.abandonAudioFocus(old.focusListener);
    }

    private void interrupt(String message) {
        if (session == null) return;
        headphonesBlocked = false;
        interrupted = true; // Settings/music updates cannot silently restart after an interruption.
        stopSession();
        listener.onInterrupted(message);
    }

    void stop() { setPlayback(null, false, false, false); }

    void close() {
        onMain(() -> {
            closed = true;
            stopSession();
            context.unregisterReceiver(noisy);
            context.getContentResolver().unregisterContentObserver(volumeObserver);
            if (Build.VERSION.SDK_INT >= 23) audio.unregisterAudioDeviceCallback(deviceCallback);
        });
    }

    private void writeAudio(Session current) {
        boolean completed = false;
        String failure = null;
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);
            int chunkFrames = Math.max(1, current.sampleRate / 50);
            short[] buffer = new short[chunkFrames * 2];
            long prerollFrames = 0;
            long routeDeadline = SystemClock.elapsedRealtime() + 3000;
            while (current.running && !current.routeReady) {
                prerollFrames += writeSamples(current, buffer, buffer.length) / 2;
                if (SystemClock.elapsedRealtime() > routeDeadline) throw new IllegalStateException("Headphone route could not be confirmed");
            }
            AuxWaveform.Plan rendering = null;
            AuxWaveform renderer = null;
            long written = prerollFrames;
            long presented = 0;
            long lastHead = 0;
            while (current.running) {
                if (rendering != current.plan) {
                    rendering = current.plan;
                    renderer = new AuxWaveform(rendering, current.sampleRate);
                }
                int frames = renderer.render(buffer, chunkFrames, current.levels);
                if (frames == 0) {
                    // Short bursts may not reach AudioTrack's startup threshold. Queue trailing silence
                    // so even a sub-buffer burst starts on older Android versions. Drain only the waveform.
                    long target = written;
                    java.util.Arrays.fill(buffer, (short)0);
                    for (int padding = 0; current.running && rendering == current.plan
                            && padding < current.bufferFrames;) {
                        int count = writeSamples(current, buffer, Math.min(chunkFrames, current.bufferFrames - padding) * 2);
                        padding += count / 2;
                        written += count / 2;
                    }
                    // Native burst completion must not truncate queued AUX audio.
                    long progressTime = SystemClock.elapsedRealtime();
                    while (current.running && rendering == current.plan && presented < target) {
                        long head = current.track.getPlaybackHeadPosition() & 0xffffffffL;
                        long delta = (head - lastHead) & 0xffffffffL;
                        presented += delta;
                        lastHead = head;
                        if (delta > 0) progressTime = SystemClock.elapsedRealtime();
                        if (SystemClock.elapsedRealtime() - progressTime > 3000) {
                            throw new IllegalStateException("Audio output stalled");
                        }
                        if (presented < target) SystemClock.sleep(5);
                    }
                    if (rendering != current.plan) continue;
                    completed = current.running;
                    break;
                }
                written += writeSamples(current, buffer, frames * 2) / 2;
                // Keep the long frame count valid even after the 32-bit playback head wraps.
                if (current.running) {
                    long head = current.track.getPlaybackHeadPosition() & 0xffffffffL;
                    presented += (head - lastHead) & 0xffffffffL;
                    lastHead = head;
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "AUX writer stopped", e);
            if (current.running) failure = "AUX stopped: " + e.getMessage();
        } finally {
            final boolean finished = completed;
            final String error = failure;
            main.post(() -> {
                // A stale writer must never stop a replacement session or restore its volume.
                if (session == current) {
                    stopSession();
                    if (finished) {
                        playing = false;
                        listener.onFinished();
                    } else if (error != null) {
                        interrupted = true;
                        listener.onInterrupted(error);
                    }
                }
                current.track.release(); // Writer has exited before release; no join on the UI thread.
            });
        }
    }

    private int writeSamples(Session current, short[] buffer, int size) {
        int offset = 0;
        long progressTime = SystemClock.elapsedRealtime();
        while (current.running && offset < size) {
            int count = current.track.write(buffer, offset, size - offset);
            if (count < 0) {
                if (!current.running) break;
                throw new IllegalStateException("Audio write failed (" + count + ")");
            }
            if (count == 0) {
                if (SystemClock.elapsedRealtime() - progressTime > 3000) throw new IllegalStateException("Audio write stalled");
                SystemClock.sleep(1);
                continue;
            }
            progressTime = SystemClock.elapsedRealtime();
            offset += count;
            if (!current.started && current.running) { current.track.play(); current.started = true; }
        }
        return offset;
    }

    private static final class Session {
        final AudioTrack track;
        final int sampleRate;
        final int bufferFrames;
        boolean started; // Writer thread only.
        volatile AuxWaveform.Plan plan;
        volatile AuxWaveform.Levels levels;
        volatile boolean running = true;
        volatile int deviceId = -1;
        volatile boolean restoreVolume = true;
        volatile boolean requireHeadphones;
        volatile boolean routeReady;
        boolean volumeHandled; // Main thread only.
        AudioTrack.OnRoutingChangedListener routeListener;
        AudioManager.OnAudioFocusChangeListener focusListener;
        AudioFocusRequest focusRequest;

        Session(AudioTrack track, int rate, int bufferFrames, AuxWaveform.Plan plan, AuxWaveform.Levels levels) {
            this.track = track;
            sampleRate = rate;
            this.bufferFrames = bufferFrames;
            this.plan = plan;
            this.levels = levels;
        }
    }
}
