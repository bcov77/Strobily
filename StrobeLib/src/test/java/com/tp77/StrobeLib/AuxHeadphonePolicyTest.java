package com.tp77.StrobeLib;

import android.media.AudioDeviceInfo;
import org.junit.Test;
import static org.junit.Assert.*;

public class AuxHeadphonePolicyTest {
    @Test public void wiredHeadphonesAndHeadsetsQualify() {
        assertTrue(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_WIRED_HEADPHONES));
        assertTrue(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_WIRED_HEADSET));
    }

    @Test public void usbHeadphoneDonglesAndAudioInterfacesQualify() {
        assertTrue(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_USB_HEADSET));
        assertTrue(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_USB_DEVICE));
        assertTrue(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_USB_ACCESSORY));
    }

    @Test public void speakersAndUnrelatedDigitalOutputsDoNotQualify() {
        assertFalse(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER));
        assertFalse(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE));
        assertFalse(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP));
        assertFalse(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_HDMI));
        assertFalse(AuxAudioOutput.isHeadphoneType(AudioDeviceInfo.TYPE_UNKNOWN));
    }
}
