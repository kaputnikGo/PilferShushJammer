package cityfreqs.com.pilfershushjammer;

import android.media.AudioFormat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class AudioSettings {
    // audio utility helper class
    public static final int[] SAMPLE_RATES = new int[]{
            48000, 44100, 22050, 16000, 11025, 8000};

    public static final int[] POWERS_TWO_HIGH = new int[]{
            512, 1024, 2048, 4096, 8192, 16384};

    private static final int[] POWERS_TWO_LOW = new int[]{
            2, 4, 8, 16, 32, 64, 128, 256};

    public static final int MINIMUM_NUHF_FREQUENCY = 18000;
    public static final int DEFAULT_NUHF_FREQUENCY = 19000;

    public static final int CARRIER_TEST_FREQUENCY = 440;
    public static final int MAXIMUM_TEST_FREQUENCY = CARRIER_TEST_FREQUENCY + (int)(CARRIER_TEST_FREQUENCY * 0.5);
    public static final int MINIMUM_TEST_FREQUENCY = CARRIER_TEST_FREQUENCY - (int)(CARRIER_TEST_FREQUENCY * 0.5);

    public static final int DEFAULT_RANGE_DRIFT_LIMIT = 1000;
    public static final int MINIMUM_DRIFT_LIMIT = 10;
    public static final int DRIFT_SPEED_MULTIPLIER = 1000;

    public static final int JAMMER_TONE = 0;
    public static final int JAMMER_WHITE = 1;

    public static final int JAMMER_TYPE_TEST = 0;
    public static final int JAMMER_TYPE_NUHF = 1;
    public static final int JAMMER_TYPE_DEFAULT_RANGED = 2;
    public static final int JAMMER_TYPE_USER_RANGED = 3;

    // Bundle keys string names
    public static final String[] AUDIO_BUNDLE_KEYS = new String[] {
            "audioSource", "sampleRate", "channelInConfig", "encoding",
            "bufferInSize", "channelOutConfig", "bufferOutSize", "activeType",
            "jammerType", "userCarrier", "userLimit", "userSpeed", "hasEQ",
            "maxFreq"
    };

    /*
    public String toString() {
        return ("audio record format: "
                + sampleRate + ", " + bufferInSize + ", "
                + encoding + ", " + channelInConfig + ", " + audioSource);
    }
    */


    /********************************************************************/
    /*
     * Utilities, unused, but may be useful one day
     *
     */
    public static int getBitDepth(int encoding) {
        // encoding == int value of bit depth
        if (encoding == AudioFormat.ENCODING_PCM_8BIT) return 8;
        else if (encoding == AudioFormat.ENCODING_PCM_16BIT) return 16;
        else if (encoding == AudioFormat.ENCODING_PCM_FLOAT) return 32;
        else {
            // default or error, return "guaranteed" default
            return 16;
        }
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
        return new byte[]{(byte) (s & 0x00FF), (byte) ((s & 0xFF00) >> 8)};
    }

    public static byte[] shortToByte(short[] arr) {
        ByteBuffer bb = ByteBuffer.allocate(arr.length * 2);
        bb.asShortBuffer().put(arr);
        return bb.array();
    }

    public static double soundPressureLevel(final float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        double value = Math.pow(power, 0.5) / buffer.length;
        return 20.0 * Math.log10(value);
    }

    public static byte[] floatArrayToByteArray(float floatArray[]) {
        byte byteArray[] = new byte[floatArray.length * 4];
        ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        // store to byte array
        floatBuf.put(floatArray);
        return byteArray;
    }
}

    /********************************************************************/
    /*
     * NOTES re. audio capabilities on android
     *
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


