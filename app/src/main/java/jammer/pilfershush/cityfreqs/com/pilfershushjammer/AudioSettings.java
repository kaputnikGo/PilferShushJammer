package jammer.pilfershush.cityfreqs.com.pilfershushjammer;

import android.media.AudioFormat;

import java.nio.ByteBuffer;

public class AudioSettings {
    // helper vars and defaults
    // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
    public static final int[] SAMPLE_RATES = new int[] {
            48000, 44100, 22050, 16000, 11025, 8000 };

    public static final int[] POWERS_TWO_HIGH = new int[] {
            512, 1024, 2048, 4096, 8192, 16384 };

    public static final int[] POWERS_TWO_LOW = new int[] {
            2, 4, 8, 16, 32, 64, 128, 256 };

    // vars for AudioRecord creation and use
    private int sampleRate;
    private int bufferSize; // in bytes
    private int encoding;
    private int channelConfig;
    private int channelCount;
    private int audioSource;

    public AudioSettings() {
        //default, empty constructor
    }

    public void setBasicAudioSettings(int sampleRate, int bufferSize, int encoding, int channelConfig, int channelCount) {
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
        this.encoding = encoding;
        this.channelConfig = channelConfig;
        this.channelCount = channelCount;
    }

    public void setEncoding(int encoding) {
        this.encoding = encoding;
    }
    public void setAudioSource(int audioSource) {
        this.audioSource = audioSource;
    }

    public int getSampleRate() {
        return sampleRate;
    }
    public int getBufferSize() {
        return bufferSize;
    }
    public int getEncoding() {
        return encoding;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getChannelCount() {
        return channelCount;
    }
    public int getAudioSource() {
        return audioSource;
    }

    public String toString() {
        return new String("audio record format: "
                + sampleRate + ", " + bufferSize + ", "
                + encoding + ", " + channelConfig + ", " + audioSource);
    }

    public String saveFormatToString() {
        return new String(sampleRate + " Hz, "
                + getBitDepth() + " bits, "
                + channelCount + " channel");
    }

    public int getBitDepth() {
        // encoding == int value of bit depth
        if (encoding == AudioFormat.ENCODING_PCM_8BIT) return 8;
        else if (encoding == AudioFormat.ENCODING_PCM_16BIT) return 16;
        else if (encoding == AudioFormat.ENCODING_PCM_FLOAT) return 32;
        else {
            // default or error, return "guaranteed" default
            return 16;
        }
    }

    /********************************************************************/
    /*
     * Utilities, that may be useful...
     *
     */
    public static int getClosestPowersHigh(int reported) {
        // return the next highest power from the minimum reported
        // 512, 1024, 2048, 4096, 8192, 16384
        for (int power : POWERS_TWO_HIGH) {
            if (reported <= power) {
                return power;
            }
        }
        // didn't find power, return reported
        return reported;
    }

    public static int getClosestPowersLow(int reported) {
        // return the next highest power from the minimum reported
        // 2, 4, 8, 16, 32, 64, 128, 256
        for (int power : POWERS_TWO_LOW) {
            if (reported <= power) {
                return power;
            }
        }
        // didn't find power, return reported
        return reported;
    }

    public static byte[] toBytes(short s) {
        // convert shorts to bytes
        // Java short is a 16-bit type, and byte is an 8-bit type.
        return new byte[]{(byte)(s & 0x00FF),(byte)((s & 0xFF00)>>8)};
    }

    public static byte[] shortToByte(short[] arr) {
        ByteBuffer bb = ByteBuffer.allocate(arr.length * 2);
        bb.asShortBuffer().put(arr);
        return bb.array();
    }

    public double soundPressureLevel(final float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        double value = Math.pow(power, 0.5) / buffer.length;
        return 20.0 * Math.log10(value);
    }

    /********************************************************************/
    /*
     * NOTES re. audio capabilities on android
     *
     */


    /********************************************************************/
    /*
    AudioRecord.cpp ::

    if (inputSource == AUDIO_SOURCE_DEFAULT) {
        inputSource = AUDIO_SOURCE_MIC;
    }

    */

/*
 *      Find audio record format for device.
 *
 *      NOTES
 *      channelConfig != number of channels of audio
 *      CHANNEL_IN_MONO (channel count = 1, mono ) = CHANNEL_IN_FRONT (channel count = 2, stereo)
 *
 *      other possible values to consider:
 *      AudioFormat.ENCODING_PCM_FLOAT = 4
 *      AudioFormat.ENCODING_AC3 = 5
 *      AudioFormat.ENCODING_E_AC3 = 6
 *
 *      below has channel count = 2 (stereo)
 *      AudioFormat.CHANNEL_IN_FRONT = 16 // n.b. CHANNEL_IN_MONO = CHANNEL_IN_FRONT
 *      AudioFormat.CHANNEL_IN_BACK = 32
 *
 *
 *
/system/media/audio/include/system/audio.h
/android/media/AudioFormat.java
/android/media/AudioRecord.java

 typedef enum {
    //input devices
    AUDIO_DEVICE_IN_COMMUNICATION         = 0x10000,
    AUDIO_DEVICE_IN_AMBIENT               = 0x20000,
    AUDIO_DEVICE_IN_BUILTIN_MIC           = 0x40000,
    AUDIO_DEVICE_IN_BLUETOOTH_SCO_HEADSET = 0x80000,
    AUDIO_DEVICE_IN_WIRED_HEADSET         = 0x100000,
    AUDIO_DEVICE_IN_AUX_DIGITAL           = 0x200000,
    AUDIO_DEVICE_IN_VOICE_CALL            = 0x400000,
    AUDIO_DEVICE_IN_BACK_MIC              = 0x800000,
    AUDIO_DEVICE_IN_DEFAULT               = 0x80000000,
}

typedef enum {
    AUDIO_SOURCE_DEFAULT             = 0,
    AUDIO_SOURCE_MIC                 = 1,
    AUDIO_SOURCE_VOICE_UPLINK        = 2,  // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
    AUDIO_SOURCE_VOICE_DOWNLINK      = 3,  // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
    AUDIO_SOURCE_VOICE_CALL          = 4,  // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
    AUDIO_SOURCE_CAMCORDER           = 5,  // for video recording, same orientation as camera
    AUDIO_SOURCE_VOICE_RECOGNITION   = 6,  // tuned for voice recognition
    AUDIO_SOURCE_VOICE_COMMUNICATION = 7,  // VoIP with echo cancel, auto gain ctrl if available
    AUDIO_SOURCE_CNT,
    AUDIO_SOURCE_MAX                 = AUDIO_SOURCE_CNT - 1,
} audio_source_t;

also -

@SystemApi
public static final int HOTWORD = 1999; //  always-on software hotword detection,
         while gracefully giving in to any other application
         that might want to read from the microphone.
         This is a hidden audio source.

         same gain and tuning as VOICE_RECOGNITION
         Flat frequency response (+/- 3dB) from 100Hz to 4kHz
         Effects/pre-processing must be disabled by default
         Near-ultrasound requirements: no band-pass or anti-aliasing filters.

         android.Manifest.permission.HOTWORD_RECOGNITION

         ** the HOTWORD may not be detectable by technique of forcing errors when polling mic

 *
 */

}


