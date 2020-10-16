package cityfreqs.com.pilfershushjammer.utilities;

public class AudioSettings {
    // audio utility helper class
    static final int[] SAMPLE_RATES = new int[]{
            48000, 44100, 22050, 16000, 11025, 8000};

    static final int[] POWERS_TWO_HIGH = new int[]{
            512, 1024, 2048, 4096, 8192, 16384};

    /*
    private static final int[] POWERS_TWO_LOW = new int[]{
            2, 4, 8, 16, 32, 64, 128, 256};
    */
    public static final double TWO_PI = 6.283185307179586;
    public static final int MINIMUM_NUHF_FREQUENCY = 18000;
    public static final int DEFAULT_NUHF_FREQUENCY = 19000;
    public static final int SHADOW_CARRIER_FREQUENCY = 24000;
    public static final int SHADOW_MINIMUM_FREQUENCY = 22000;
    public static final int SHADOW_FLOOR_FREQUENCY = 25000;
    public static final int SHADOW_CEILING_FREQUENCY = 26000;
    public static final int SHADOW_DRIFT_RANGE = 500;

    public static final int CARRIER_TEST_FREQUENCY = 440;
    public static final int MAXIMUM_TEST_FREQUENCY = CARRIER_TEST_FREQUENCY + (int) (CARRIER_TEST_FREQUENCY * 0.5);
    public static final int MINIMUM_TEST_FREQUENCY = CARRIER_TEST_FREQUENCY - (int) (CARRIER_TEST_FREQUENCY * 0.5);

    public static final int DEFAULT_RANGE_DRIFT_LIMIT = 1000;
    public static final int MINIMUM_DRIFT_LIMIT = 10;
    public static final int DRIFT_SPEED_MULTIPLIER = 1000;

    public static final int JAMMER_NOISE = 0;
    public static final int JAMMER_TONE = 1;
    public static final int JAMMER_SHADOW = 2;

    public static final int JAMMER_TYPE_TEST = 0;
    public static final int JAMMER_TYPE_NUHF = 1;
    public static final int JAMMER_TYPE_DEFAULT_RANGED = 2;
    public static final int JAMMER_TYPE_USER_RANGED = 3;

    public static final int WAVEFORM_SIN = 0;
    public static final int WAVEFORM_SQR = 1;
    public static final int WAVEFORM_SAW = 2;

    public static final int MIC_SOURCE_DEFAULT = 0;
    public static final int MIC_SOURCE_VOICE_COMM = 7;

    public static final String[] JAMMER_TYPES = new String[]{
            "Slow audible test tone drift (~440Hz)",
            "Full NUHF drift (18kHz-24kHz)",
            "1000Hz NUHF drift with user carrier",
            "User NUHF drift with user carrier"
    };

    // Bundle keys string names: number = 19
    public static final String[] AUDIO_BUNDLE_KEYS = new String[]{
            "audioSource", "sampleRate", "channelInConfig", "encoding", //3
            "bufferInSize", "channelOutConfig", "bufferOutSize", "activeType", //7
            "jammerType", "userCarrier", "userLimit", "userSpeed", "hasEQ", //12
            "maxFreq", "bufferRead", "debug", "permissions", "formatOut", //17
            "waveform", "eqPreset" //19
    };

    /*
    // just another way of doing the above
    public static String GET_AUDIO_BUNDLE_KEY(int i) {
        switch (i) {
            case 0:
                return "audioSource";
            case 1:
                return "sampleRate";
            case 2:
                return "channelInConfig";
            case 3:
                return "encoding";
            case 4:
                return "bufferInSize";
            case 5:
                return "channelOutConfig";
            case 6:
                return "bufferOutSize";
            case 7:
                return "activeType"; // no longer bool, is now int (0,1,2)
            case 8:
                return "jammerType";
            case 9:
                return "userCarrier";
            case 10:
                return "userLimit";
            case 11:
                return "userSpeed";
            case 12:
                return "hasEQ";
            case 13:
                return "maxFreq";
            case 14:
                return "bufferRead";
            case 15:
                return "debug";
            case 16:
                return "permission";
            case 17:
                return "formatOut";
            default:
                return "unknown";
        }
    }
    */

    public static String GET_MIC_SOURCE(int i) {
        switch (i) {
            case 0:
                return "AUDIO_SOURCE_DEFAULT";
            case 1:
                return "AUDIO_SOURCE_MIC";
            case 2:
                return "AUDIO_SOURCE_VOICE_UPLINK";  // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
            case 3:
                return "AUDIO_SOURCE_VOICE_DOWNLINK"; // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
            case 4:
                return "AUDIO_SOURCE_VOICE_CALL"; // system only, requires Manifest.permission#CAPTURE_AUDIO_OUTPUT
            case 5:
                return "AUDIO_SOURCE_CAMCORDER"; // for video recording, same orientation as camera
            case 6:
                return "AUDIO_SOURCE_VOICE_RECOGNITION"; // tuned for voice recognition
            case 7:
                return "AUDIO_SOURCE_VOICE_COMMUNICATION"; // tuned for VoIP with echo cancel, auto gain ctrl if available
            default:
                return "AUDIO_SOURCE_DEFAULT";
        }
    }

    // MicrophoneInfo for min API28
    public static final String[] MIC_INFO_LOCATION = new String[]{
            "unknown", "mainbody", "mainbody movable", "peripheral"
    };

    public static final String[] MIC_INFO_DIRECTION = new String[]{
            "unknown", "omni", "bi-directional", "cardioid", "hyper-cardioid", "super-cardioid"
    };

    // added API 23
    // API 30 has 25: "builtin-speaker-safe" -  outputting sounds like notifications and alarms
    // (i.e. sounds the user couldn't necessarily anticipate)
    public static final String[] AUDIO_DEVICE_INFO_TYPE = new String[]{
            "unknown", "builtin-earpiece", "builtin-speaker", "wired-headset", "wired-headphones", //5
            "line-analog", "line-digital", "bluetooth-sco", "bluetooth-a2dp", "hdmi", "hdmi-arc", //10
            "usb-device", "usb-accessory", "dock", "FM", "builtin-mic", //15
            "FM-tuner", "TV-tuner", "telephony", "aux-line", "IP", //20
            "bus", "usb-headset", "hearing-aid" //24
    };

    /*
    // just another way of doing the above but returning the constant names...
    public static String GET_DEVICE_TYPE(int i) {
        switch (i) {
            case 0:
                return "UNKNOWN";
            case 1:
                return "TYPE_BUILTIN_EARPIECE";
            case 2:
                return "TYPE_BUILTIN_SPEAKER";
            case 3:
                return "TYPE_WIRED_HEADSET";
            case 4:
                return "TYPE_WIRED_HEADPHONES";
            case 5:
                return "TYPE_LINE_ANALOG";
            case 6:
                return "TYPE_LINE_DIGITAL";
            case 7:
                return "TYPE_BLUETOOTH_SCO";
            case 8:
                return "TYPE_BLUETOOTH_A2DP";
            case 9:
                return "TYPE_HDMI";
            case 10:
                return "TYPE_HDMI_ARC";
            case 11:
                return "TYPE_USB_DEVICE";
            case 12:
                return "TYPE_USB_ACCESSORY";
            case 13:
                return "TYPE_DOCK";
            case 14:
                return "TYPE_FM";
            case 15:
                return "TYPE_BUILTIN_MIC";
            case 16:
                return "TYPE_FM_TUNER";
            case 17:
                return "TYPE_TV_TUNER";
            case 18:
                return "TYPE_TELEPHONY";
            case 19:
                return "TYPE_AUX_LINE";
            case 20:
                return "TYPE_IP";
            case 21:
                return "TYPE_BUS";
            case 22:
                return "TYPE_USB_HEADSET";
            case 23:
                return "HEARING_AID";
            default:
                return "TYPE_UNKNOWN";
        }
    }
    */

    /*
    // these will resolve to a UUID when/if later APIs enabled
    public static String[] GET_AUDIO_EFFECT_TYPE = new String[] {
        "EFFECT_TYPE_AEC", "EFFECT_TYPE_AGC", "EFFECT_TYPE_BASS_BOOST", "EFFECT_TYPE_DYNAMICS_PROCESSING", // last for API 28
        "EFFECT_TYPE_ENV_REVERB", "EFFECT_TYPE_EQUALIZER", "EFFECT_TYPE_LOUDNESS_ENHANCER", // last for API 19
        "EFFECT_TYPE_NS", "EFFECT_TYPE_PRESET_REVERB", "EFFECT_TYPE_VIRTUALIZER"
    };
    */
}

/*
 * *******************************************************************/
/*
 * Utilities, unused, but may be useful one day
 *
 */
    /*
    static int getBitDepth(int encoding) {
        // encoding == int value of bit depth
        if (encoding == AudioFormat.ENCODING_PCM_8BIT) return 8;
        else if (encoding == AudioFormat.ENCODING_PCM_16BIT) return 16;
        else if (encoding == AudioFormat.ENCODING_PCM_FLOAT) return 32;
        else {
            // default or error, return "guaranteed" default
            return 16;
        }
    }


    static int getClosestPowersLow(int reported) {
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

    static byte[] toBytes(short s) {
        // convert shorts to bytes
        // Java short is a 16-bit type, and byte is an 8-bit type.
        return new byte[]{(byte) (s & 0x00FF), (byte) ((s & 0xFF00) >> 8)};
    }

    static byte[] shortToByte(short[] arr) {
        ByteBuffer bb = ByteBuffer.allocate(arr.length * 2);
        bb.asShortBuffer().put(arr);
        return bb.array();
    }

    static double soundPressureLevel(final float[] buffer) {
        double power = 0.0D;
        for (float element : buffer) {
            power += element * element;
        }
        double value = Math.pow(power, 0.5) / buffer.length;
        return 20.0 * Math.log10(value);
    }

    static byte[] floatArrayToByteArray(float floatArray[]) {
        byte byteArray[] = new byte[floatArray.length * 4];
        ByteBuffer byteBuf = ByteBuffer.wrap(byteArray);
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        // store to byte array
        floatBuf.put(floatArray);
        return byteArray;
    }
    */

/*

 NOTES FOR MediaRecorder.AudioSource.VOICE_COMMUNICATION
 may cause spurious Process_VoIP calls as OS may be doing DSP on the zero-data buffer
 n.b. as per adb on taint device:
 calls made to :
 AudioALSAStreamManager
 AudioALSAStreamIn
 AudioSpeechEnhanceInfo
 AudioYusuParam
 AudioALSAHardware
 AudioALSAVoiceWakeUpController
 AudioALSAStreamOut
 AudioALSAVolumeController
 AudioALSAPlaybackHandlerFast
 AudioALSAHardwareResourceManager
 AudioALSADeviceParser
 AudioALSAPlaybackHandlerBase
 MtkAudioBitConverter // vendor mediatek
 AudioALSADriverUtility
 AudioALSADeviceConfigManager
 AudioVoiceUIDL

 VoIP is causing AudioALSAHardware: +routing createAudioPatch 80000004->Mixer Src 7
 and
 AudioALSAStreamManager: +createCaptureHandler(), mAudioMode = 0, input_source = 7, input_device = 0x80000004, mBypassDualMICProcessUL=0
 and
 AudioALSACaptureHandlerAEC: AudioALSACaptureHandlerAEC() // echo cancellation

D/AudioSPELayer: Process_VoIP, mULInBufQLenTotal=1420, mDLInBufQLenTotal=4550, SPERecBufSize=1280,ULIncopysize=35
D/AudioSPELayer: WriteReferenceBuffer,inBufLength=586
D/AudioSPELayer: AddtoInputBuffer queue downlink sec 362 nsec 173730713, downlink sec 362 nsec 150062907
D/AudioSPELayer: downlink estimate time bRemainInfo=1, pre tv_sec=362, pre nsec=131720316, mPreDLBufLen=588
D/AudioSPELayer: downlink queue estimate time, sec 362 nsec 150062907, inBufLength=586
D/AudioSPELayer: AddtoInputBuffer, mDLInBufferQ.size()=8, mDLPreQnum=5,mDLPreQLimit=0,mFirstVoIPUplink=0,mDLInBufQLenTotal=4496
D/AudioSPELayer: uplink estimate time bRemainInfo=1, pre tv_sec=362, pre nsec=133373292, mPreDLBufLen=586, tv_sec=362, nsec=153416523

 */


/*
 * *******************************************************************/
    /*
     * NOTES re. audio capabilities on android
     *
    /********************************************************************/
    /*
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
