package com.tp77.StrobeLib;

import org.junit.Test;
import static org.junit.Assert.*;

public class AuxWaveformTest {
    @Test public void finitePulseHasExactLevelsAndChannels() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{2000, 3000}, false, false), 1000);
        short[] samples = new short[20];
        assertEquals(5, wave.render(samples, 10, new AuxWaveform.Levels(100, -100, 1)));
        assertArrayEquals(new short[]{32767, 32767, 32767, 32767, -32768, -32768,
                -32768, -32768, -32768, -32768}, java.util.Arrays.copyOf(samples, 10));
        assertEquals(0, wave.render(samples, 10, new AuxWaveform.Levels(100, 0, 1)));
    }

    @Test public void channelAndVoltageChangesPreservePulsePosition() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{2000, 2000}, true, false), 1000);
        short[] samples = new short[2];
        wave.render(samples, 1, new AuxWaveform.Levels(100, 0, 0));
        assertArrayEquals(new short[]{32767, 0}, samples);
        wave.render(samples, 1, new AuxWaveform.Levels(50, -100, 2));
        assertArrayEquals(new short[]{0, 16384}, samples);
        wave.render(samples, 1, new AuxWaveform.Levels(100, -100, 2));
        assertArrayEquals(new short[]{0, -32768}, samples);
    }

    @Test public void skippedFlashesAndZeroDurationSegmentsDoNotEmitHigh() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{-1, 2000, 0, 1000}, false, false), 1000);
        short[] samples = new short[20];
        assertEquals(3, wave.render(samples, 10, new AuxWaveform.Levels(100, 0, 1)));
        assertArrayEquals(new short[20], samples);
    }

    @Test public void torchHoldsHighRegardlessOfSchedule() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{0, 0}, false, true), 48000);
        short[] samples = new short[200];
        assertEquals(100, wave.render(samples, 100, new AuxWaveform.Levels(-100, 100, 0)));
        for (int i = 0; i < 100; i++) {
            assertEquals(-32768, samples[i * 2]);
            assertEquals(0, samples[i * 2 + 1]);
        }
    }

    @Test public void fractionalSamplesDoNotAccumulateFrequencyError() {
        int[] durations = new int[10000];
        for (int i = 0; i < durations.length; i += 2) { durations[i] = 333; durations[i + 1] = 667; }
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(durations, false, false), 44100);
        short[] samples = new short[254];
        int total = 0;
        int count;
        while ((count = wave.render(samples, 127, new AuxWaveform.Levels(100, 0, 1))) != 0) total += count;
        assertEquals(220500, total); // Exactly five seconds, even with sub-sample segment lengths.
    }

    @Test public void bufferBoundariesAndLoopBoundariesAreTransparent() {
        AuxWaveform.Plan plan = new AuxWaveform.Plan(new int[]{333, 667, -1, 1001}, true, false);
        AuxWaveform.Levels levels = new AuxWaveform.Levels(75, -20, 1);
        short[] uninterrupted = new short[20000];
        new AuxWaveform(plan, 44100).render(uninterrupted, 10000, levels);
        short[] chunked = new short[20000];
        short[] chunk = new short[146];
        AuxWaveform wave = new AuxWaveform(plan, 44100);
        for (int position = 0; position < 10000;) {
            int frames = Math.min(73, 10000 - position);
            assertEquals(frames, wave.render(chunk, frames, levels));
            System.arraycopy(chunk, 0, chunked, position * 2, frames * 2);
            position += frames;
        }
        assertArrayEquals(uninterrupted, chunked);
    }

    @Test public void brightnessScalesHighAndLowAmplitudes() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{1000, 1000}, false, false), 1000);
        short[] samples = new short[4];
        assertEquals(2, wave.render(samples, 2, new AuxWaveform.Levels(100, -100, 1, 50)));
        assertArrayEquals(new short[]{16384, 16384, -16384, -16384}, samples);
    }

    @Test public void brightnessRetainsFractionalVoltagePrecisionAndClampsGain() {
        AuxWaveform.Levels fractional = new AuxWaveform.Levels(1, -1, 1, 50);
        assertEquals(164, fractional.high);
        assertEquals(-164, fractional.low);
        AuxWaveform.Levels silent = new AuxWaveform.Levels(100, -100, 1, 0);
        assertEquals(0, silent.high);
        assertEquals(0, silent.low);
        assertEquals(0, new AuxWaveform.Levels(100, -100, 1, -10).high);
        AuxWaveform.Levels maximum = new AuxWaveform.Levels(100, -100, 1, 200);
        assertEquals(32767, maximum.high);
        assertEquals(-32768, maximum.low);
    }

    @Test public void liveBrightnessChangesDoNotRestartPulseAndCanRestoreFullLevel() {
        AuxWaveform wave = new AuxWaveform(new AuxWaveform.Plan(new int[]{3000, 1000}, true, false), 1000);
        short[] frame = new short[2];
        wave.render(frame, 1, new AuxWaveform.Levels(100, -100, 0, 100));
        assertArrayEquals(new short[]{32767, 0}, frame);
        wave.render(frame, 1, new AuxWaveform.Levels(100, -100, 0, 25));
        assertArrayEquals(new short[]{8192, 0}, frame);
        wave.render(frame, 1, new AuxWaveform.Levels(100, -100, 0, 100));
        assertArrayEquals(new short[]{32767, 0}, frame);
        wave.render(frame, 1, new AuxWaveform.Levels(100, -100, 0, 50));
        assertArrayEquals(new short[]{-16384, 0}, frame);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectTimelineWithNoDuration() {
        new AuxWaveform.Plan(new int[]{-1, 0}, true, false);
    }
}
