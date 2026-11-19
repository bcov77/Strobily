package com.tp77.StrobeLib;

/** Sample-clock renderer. No Android dependencies, camera calls, sleeps, or per-buffer allocation. */
final class AuxWaveform {
    static final class Plan {
        final int[] durations;
        final boolean loop;
        final boolean torch;

        Plan(int[] durations, boolean loop, boolean torch) {
            if (durations == null || durations.length == 0 || durations.length % 2 != 0) {
                throw new IllegalArgumentException("Expected ON/OFF pairs");
            }
            this.durations = durations.clone();
            this.loop = loop;
            this.torch = torch;
            long total = 0;
            for (int duration : durations) total += Math.max(0, duration);
            if (!torch && total == 0) throw new IllegalArgumentException("Empty AUX timeline");
        }
    }

    static final class Levels {
        final short high;
        final short low;
        final int stereo; // 0 = left, 1 = both, 2 = right

        Levels(int high, int low, int stereo) {
            this(high, low, stereo, 100);
        }

        Levels(int high, int low, int stereo, int brightness) {
            int gain = Math.max(0, Math.min(100, brightness));
            this.high = sample(high, gain);
            this.low = sample(low, gain);
            this.stereo = Math.max(0, Math.min(2, stereo));
        }

        private static short sample(int hundredths, int gain) {
            int value = Math.max(-100, Math.min(100, hundredths));
            // Scale before quantization so low voltage settings retain their precision.
            return (short)Math.round(value * (value < 0 ? 32768f : 32767f) * gain / 10000f);
        }
    }

    private final Plan plan;
    private final int sampleRate;
    private int nextSegment;
    private long remainingFrames;
    private long remainder;
    private boolean high;
    private boolean finished;

    AuxWaveform(Plan plan, int sampleRate) {
        this.plan = plan;
        this.sampleRate = sampleRate;
    }

    /** Returns the number of stereo frames rendered; a short buffer marks a finite sequence's end. */
    int render(short[] samples, int frames, Levels levels) {
        int rendered = 0;
        while (rendered < frames) {
            if (!plan.torch) {
                while (remainingFrames == 0 && !finished) {
                    if (nextSegment == plan.durations.length) {
                        if (plan.loop) nextSegment = 0;
                        else { finished = true; break; }
                    }
                    high = nextSegment % 2 == 0;
                    // A negative ON is a skipped flash/delay in Strobily's existing pattern format.
                    long scaled = Math.max(0, plan.durations[nextSegment++]) * (long)sampleRate + remainder;
                    remainingFrames = scaled / 1000000;
                    remainder = scaled % 1000000;
                }
                if (finished) break;
            }
            short value = plan.torch || high ? levels.high : levels.low;
            samples[rendered * 2] = levels.stereo == 2 ? 0 : value;
            samples[rendered * 2 + 1] = levels.stereo == 0 ? 0 : value;
            if (!plan.torch) remainingFrames--;
            rendered++;
        }
        return rendered;
    }
}
