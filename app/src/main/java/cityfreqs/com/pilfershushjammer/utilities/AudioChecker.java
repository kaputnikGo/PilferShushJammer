package cityfreqs.com.pilfershushjammer.utilities;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MicrophoneInfo;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.List;

import cityfreqs.com.pilfershushjammer.R;

import static android.content.Context.AUDIO_SERVICE;

public class AudioChecker {
    private static final String TAG = "PilferShush_AUDIO";
    private final Context context;
    private final Bundle audioBundle;
    private final AudioManager audioManager;
    private final boolean DEBUG;
    private List<MicrophoneInfo> microphoneInfoList;

    public AudioChecker(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        audioManager = (AudioManager)context.getSystemService(AUDIO_SERVICE);
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
    }

    private int getClosestPowersHigh(int reported) {
        // return the next HIGHEST power from the minimum reported
        // 512, 1024, 2048, 4096, 8192, 16384
        // n.b. bad devices are getting a derived 16384.
        for (int power : AudioSettings.POWERS_TWO_HIGH) {
            if (reported <= power) {
                return power;
            }
        }
        // didn't find power, return reported
        return reported;
    }
    /*
    // not yet used
    private int getClosestPowersLow(int reported) {
        // return the next LOWEST power from the minimum reported
        // 512, 1024, 2048, 4096, 8192, 16384
        // ie if 7688, return 4096
        for (int i = 5; i >= 0 ; i--) {
          if (reported >= AudioSettings.POWERS_TWO_HIGH[i]) {
              return AudioSettings.POWERS_TWO_HIGH[i];
          }
        }
        // didn't find power, return reported
        return reported;
    }
    */

    public String checkAudioManagerSupport() {
        // for API 23+
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String support;
            assert audioManager != null;
            String micSupportString = audioManager.getProperty(
                    AudioManager.PROPERTY_SUPPORT_MIC_NEAR_ULTRASOUND);
            if (micSupportString == null) {
                micSupportString = "null";
            }
            support = "Support Mic NUHF: " + micSupportString + "\n";

            String spkrSupportString = audioManager.getProperty(
                    AudioManager.PROPERTY_SUPPORT_SPEAKER_NEAR_ULTRASOUND);
            if (spkrSupportString == null) {
                spkrSupportString = "null";
            }
            support += "Support Spkr NUHF: " + spkrSupportString;
            return support;
        }
        else {
            return "Device under API23 (M) no NUHF property check.";
        }
    }

    public void setCapturePolicy() {
        // probably be expanded as Android 11 becomes prevalent
        // test for Android 10 concurrent audio blocking for source VOICE_COMMS
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // specifically blocks other apps recording this app's audio - which is zero data
            audioManager.setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_NONE);
        }
    }

    public boolean determineRecordAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        /*
        AudioRecord.cpp (samsung fork?)::
        if (inputSource == AUDIO_SOURCE_DEFAULT) {
            inputSource = AUDIO_SOURCE_MIC;
        }
        */
        // audio source:: AUDIO_SOURCE_VOICE_COMMUNICATION (7)
        // FOR PRIORITY BUMP IN ANDROID 10 (API29)
        // possible cause for Process_VoIP as OS may be doing DSP on the zero-data buffer
        // D/AudioSPELayer: Process_VoIP, mULInBufQLenTotal=1420, mDLInBufQLenTotal=4496, SPERecBufSize=1280,ULIncopysize=35
        // now as a switch in SettingsFragment
        //DEFAULT == 0, VOICE_COMMUNICATION == 7
        int audioSource = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]);
        // notes::
        // some pre-processing like echo cancellation, noise suppression is applied on the audio captured using VOICE_COMMUNICATION
        // assumption is that # 6,7 add DSP to the DEFAULT/MIC input
        /* MediaRecorder.java:: Microphone audio source tuned for voice communications such as VoIP. It
         *  will for instance take advantage of echo cancellation or automatic gain control
         *  if available.
         */

        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelInConfig : new short[] {
                        AudioFormat.CHANNEL_IN_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_IN_MONO,    // 16, also CHANNEL_IN_FRONT == 16
                        AudioFormat.CHANNEL_IN_STEREO }) {  // 12
                    try {
                        if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                            debugLogger("Try AudioRecord rate " + rate + "Hz, bits: " + audioFormat + ", channelInConfig: " + channelInConfig, false);
                        }
                        int buffSize = AudioRecord.getMinBufferSize(rate, channelInConfig, audioFormat);
                        // force buffSize to powersOfTwo if it isnt (ie.S5)

                        if (buffSize != AudioRecord.ERROR_BAD_VALUE) {
                            buffSize = getClosestPowersHigh(buffSize);

                            AudioRecord recorder = new AudioRecord(
                                    audioSource,
                                    rate,
                                    channelInConfig,
                                    audioFormat,
                                    buffSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                // AudioRecord.getChannelCount() is number of input audio channels (1 is mono, 2 is stereo)
                                if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                                    debugLogger("AudioRecord found: " + rate + ", buffer: " + buffSize + ", channel count: " + recorder.getChannelCount(), true);
                                }
                                // set found values
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[0], audioSource);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[1], rate);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[2], channelInConfig);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[3], audioFormat);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[4], buffSize);

                                recorder.release();
                                if (determineOutputAudioType()) {
                                    // testing output checks here to avoid possible audiofocus conflicts
                                    debugLogger("determineAudioOutput inner call returns true.", true);
                                    return true;
                                }
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                            debugLogger("Error, keep trying.", false);
                        }
                    }
                }
            }
        }
        return false;
    }

    public String determineMediaRecorderType() {
        debugLogger("determineMediaRecorderType called.", false);
        // n.b. this method slows down app start-up by approx ~2 seconds

        // as per changes to API28+ background mic use now only available to
        // foreground services using the MediaRecorder instance
        // change AudioSource here for Android 10 boost (VOICE_COMM or CAMCORDER or DEFAULT)
        MediaRecorder placeboRecorder = new MediaRecorder();
        // reserve a file handle in the application specific cache directory in the filesystem
        String placeboMediaRecorderFileName = context.getCacheDir().getAbsolutePath();
        placeboMediaRecorderFileName += "/PilferShushPlacebo.raw";
        // it is never written to.

        // causing crashes below Android 11?
        placeboRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT); // was VOICE_COMMUNICATION

        placeboRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        placeboRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        placeboRecorder.setOutputFile(placeboMediaRecorderFileName);

        // try and trip the routedDevice, need prepare() and start()
        // can cause exceptions in SOME devices from restart app while passive running
        // MediaRecorder: start failed: -38 // means start failed, post-init success, mic in use
        // INVALID_OPERATION   = -ENOSYS
        try {
            // MediaRecorder.java only checks for file exists, not mic hardware
            placeboRecorder.prepare();
            // Begins capturing and encoding data to the file specified with setOutputFile().
            placeboRecorder.start();
        }
        catch (IOException e) {
            debugLogger(context.getResources().getString(R.string.passive_state_14), true);
            return "MediaRecorder prepare, start error IO ex.";
        }
        catch (IllegalStateException ex) {
            debugLogger(context.getResources().getString(R.string.passive_state_14), true);
            return "MediaRecorder prepare error, mic in use.";
        }

        try {
            // optional attempt to enum device mics and capabilities
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // test things are plugged in, n.b.
                // The query is only valid if the MediaRecorder is currently recording.
                // If the recorder is not recording, the returned device can be null or correspond
                // to previously selected device when the recorder was last active.
                debugLogger("attempt getRoutedDevice().", false);

                if (placeboRecorder.getRoutedDevice() != null) {
                    int routedType = placeboRecorder.getRoutedDevice().getType();
                    debugLogger("routed mic type: " + AudioSettings.AUDIO_DEVICE_INFO_TYPE[routedType], false);
                }
                else {
                    // probably unreachable if no start(), will result in java.lang.RuntimeException: getRoutedDeviceId failed.
                    debugLogger("getRoutedDevice is null.", true);
                }

                // list of MicrophoneInfo (object of floats x,y,z for orientation)
                // getting: E/MediaRecorderJNI: MediaRecorder::getActiveMicrophones error -38
                // and: I/MediaRecorder: getActiveMicrophones failed, fallback on routed device info
                // a device with no firmware filling this micInfo will only get Android fallback for default ?
                microphoneInfoList = placeboRecorder.getActiveMicrophones();
                return scanMicrophoneInfoList();
            }
            else {
                debugLogger("Device is under Android P, no mic scan.", false);
                return "Device under API 28 (P), no mic info checks possible.";
            }
        }
        catch (IOException e) {
            debugLogger(context.getResources().getString(R.string.passive_state_14), true);
            return "MediaRecorder checks pAndroid P error IO ex.";
        }
        catch (Exception ex) {
            debugLogger("Caught non IO exception in mediaRecorder init, ex: " + ex, true);
            return "MediaRecorder checks pAndroid P error non-IO ex.";
        }
        finally {
            //placeboRecorder.stop(); // <- has not started so no need to stop.
            placeboRecorder.reset();
            placeboRecorder.release();
            debugLogger(context.getResources().getString(R.string.passive_state_15), true);
        }
    }

    private String scanMicrophoneInfoList() {
        // get some infos, check list is populated
        debugLogger("scanMicrophoneInfo list now...", false);
        if (microphoneInfoList.isEmpty()) {
            // check for finding zero mics regardless of the reason
            return "micInfoList is empty.";
        }
        // no really, how many?
        int howManyMics = microphoneInfoList.size();
        debugLogger("how many mics: " + howManyMics, false);
        // P = api28 for MicrophoneInfo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int micId;
            int micType;
            float micSens;
            List<Pair<Float, Float>> freqPair;
            float first = 0.0f, second = 0.0f;
            int micDirection;
            int micLocation;
            String[] micInfoPrint = new String[howManyMics];
            int i = 0;
            micInfoPrint[i] = "Microphone check:" + "\nnumber of mics: " + howManyMics + "\n";
            // account for multiple mics - dev device only has one :(
            for (MicrophoneInfo microphoneInfo : microphoneInfoList) {
                micId = microphoneInfo.getId();
                micType = microphoneInfo.getType(); // hopefully only returns an input device
                micSens = microphoneInfo.getSensitivity();
                freqPair = microphoneInfo.getFrequencyResponse();
                if (!freqPair.isEmpty()) {
                    first = freqPair.get(0).first;
                    second = freqPair.get(0).second;
                }
                micDirection = microphoneInfo.getDirectionality();
                micLocation = microphoneInfo.getLocation();

                micInfoPrint[i] +=
                        "\nMic ID: " + micId
                        + "\nMic type: " + AudioSettings.AUDIO_DEVICE_INFO_TYPE[micType];
                // checks for unknown device specs print 'unknown' and not just a zero number
                if (freqPair.isEmpty()) {
                    micInfoPrint[i] +=  "\nFrequency range pair is unknown";
                }
                else {
                    micInfoPrint[i] += "\nFrequency range pair 1: " + first + " 2: " + second;
                }
                // if dB Full Scale reports as -3.4028235E38 is const SENSITIVITY_UNKNOWN
                if (micSens == MicrophoneInfo.SENSITIVITY_UNKNOWN) {
                    micInfoPrint[i] += "\nSensitivity (dB FS): unknown";
                }
                else {
                    micInfoPrint[i] += "\nSensitivity (dB FS): " + micSens;
                }
                micInfoPrint[i] +=
                        "\nMic location: " + AudioSettings.MIC_INFO_LOCATION[micLocation]
                        + "\nMic polar pattern: " + AudioSettings.MIC_INFO_DIRECTION[micDirection]
                        + "\n";

                // advance counter for multiple mics
                i++;
            }
            // make nice return string from array
            StringBuilder micInfoBuilder = new StringBuilder();
            for (String s : micInfoPrint) {
                micInfoBuilder.append(s);
            }
            return micInfoBuilder.toString();
        }
        return "Build version under Android P, no mic info possible.";
    }

    public boolean determineOutputAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelOutConfig : new short[] {
                        AudioFormat.CHANNEL_OUT_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_OUT_MONO,    // 4
                        AudioFormat.CHANNEL_OUT_STEREO }) {  // 12
                    try {
                        if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                            debugLogger("Try Output rate " + rate + "Hz, bits: " + audioFormat + ", channelOutConfig: " + channelOutConfig, false);
                        }

                        int buffSize = AudioTrack.getMinBufferSize(rate, channelOutConfig, audioFormat);
                        if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                            debugLogger("reported minBufferSize: " + buffSize, false);
                        }
                        // AudioTrack at create wants bufferSizeInBytes, the total size (in bytes)
                        // force buffSize to powersOfTwo if it isnt (ie.S5)
                        // check for Invalid channel configuration first
                        // -1 ERROR, -2 ERROR_BAD_VALUE, -3 ERROR_INVALID_OPERATION
                        if (buffSize != AudioTrack.ERROR_BAD_VALUE) {
                            buffSize = getClosestPowersHigh(buffSize);
                            debugLogger("AudioOut buffer changed to closest powers two: " + buffSize, true);
                        }

                        // if/else for += API 26 (Oreo, 8.0) deprecation stream_types for focus
                        AudioTrack audioTrack;
                        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build();

                            AudioFormat audioFormatObject = new AudioFormat.Builder()
                                    .setEncoding(audioFormat)
                                    .setSampleRate(rate)
                                    .setChannelMask(channelOutConfig)
                                    .build();

                            audioTrack = new AudioTrack(playbackAttributes,
                                    audioFormatObject,
                                    buffSize,
                                    AudioTrack.MODE_STREAM,
                                    AudioManager.AUDIO_SESSION_ID_GENERATE);

                        }
                        else {
                            audioTrack = new AudioTrack(
                                    AudioManager.STREAM_MUSIC,
                                    rate,
                                    channelOutConfig,
                                    audioFormat,
                                    buffSize,
                                    AudioTrack.MODE_STREAM);
                        }

                        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                            if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                                debugLogger("Output found: " + rate + ", buffer: " + buffSize + ", channelOutConfig: " + channelOutConfig, true);
                            }
                            // set output values
                            // buffOutSize may not be same as buffInSize conformed to powersOfTwo
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[5], channelOutConfig);
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[6], buffSize);
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[13], (int)(rate * 0.5f));
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[17], audioFormat);

                            // test onboardEQ
                            if (testOnboardEQ(audioTrack.getAudioSessionId())) {
                                if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                                    debugLogger(context.getString(R.string.eq_check_2) + "\n", false);
                                }
                                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12], true);
                            }
                            else {
                                if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                                    debugLogger(context.getString(R.string.eq_check_3) + "\n", true);
                                }
                                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12], false);
                            }
                            audioTrack.pause();
                            audioTrack.flush();
                            audioTrack.release();

                            return true;
                        }
                    }
                    catch (Exception e) {
                        if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15]))
                            debugLogger("Error, keep trying.", false);
                    }
                }
            }
        }
        return false;
    }

    //TODO
    // vers 4.5.0 - testing EQ boosts
    // concern is boosting passed safe device specific speaker thresholds
    // n.b. API 28 addition of DynamicsProcessing that includes:
    // inputGain, preEq, multibandEq, postEq, limiter for each channel

    private boolean testOnboardEQ(int audioSessionId) {
        // eq settings saved here will be used by ActiveJammer audioBundle.hasEq == true
        // up the priority to much greater than 0 to override current audio effect engine owner,
        // audioSessionId = 0 (all audio) is deprecated
        try {
            Equalizer equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            // get some info
            short numBands = equalizer.getNumberOfBands();
            final short minEQ = equalizer.getBandLevelRange()[0]; // returns milliBel
            final short maxEQ = equalizer.getBandLevelRange()[1];

            if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                debugLogger("\n" + context.getString(R.string.eq_check_1), false);
                debugLogger(context.getString(R.string.eq_check_4) + numBands, false);
                debugLogger(context.getString(R.string.eq_check_5) + minEQ, false);
                debugLogger(context.getString(R.string.eq_check_6) + maxEQ, false);
            }

            if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15])) {
                for (short band = 0; band < numBands; band++) {
                    // divide by 1000 to get numbers into recognisable ranges
                    debugLogger("\nband " + band + " freq range min: " + (equalizer.getBandFreqRange(band)[0] / 1000), false);
                    debugLogger("Band " + band + " center freq Hz: " + (equalizer.getCenterFreq(band) / 1000), true);
                    debugLogger("band " + band + " freq range max: " + (equalizer.getBandFreqRange(band)[1] / 1000), false);
                }
                // see what if anything
                debugLogger("EQ says number of presets is: " + equalizer.getNumberOfPresets(), false);
                debugLogger("Preshow EQ preset name is: " + equalizer.getPresetName(equalizer.getCurrentPreset()), false);
                debugLogger("Preshow settings: " + equalizer.getProperties().toString(), false);
            }
            // attenuate all bands except last
            for (short j = 0; j < numBands-2; j++) {
                equalizer.setBandLevel(j, minEQ); // -1500
            }
            // boost last band
            equalizer.setBandLevel((short)(numBands-1), maxEQ); // +1500

            audioBundle.putString(AudioSettings.AUDIO_BUNDLE_KEYS[19], equalizer.getProperties().toString());
            debugLogger("testOnboardEq settings: " + equalizer.getProperties().toString(), true);
            return true;
        }
        catch (Exception ex) {
            debugLogger(context.getString(R.string.eq_check_8), true);
            ex.printStackTrace();
            return false;
        }
    }

    private void debugLogger(String message, boolean caution) {
        // for the times that fragments arent attached etc, print to adb
        if (caution && DEBUG) {
            Log.e(TAG, message);
        }
        else if ((!caution) && DEBUG) {
            Log.d(TAG, message);
        }
        else {
            Log.i(TAG, message);
        }
    }
}
/*

 NOTES FOR EQ:
    // EQ is audioSessionId specific
    // not a filter... reduced amplitude seems the best description when using eq.
    // currently only active test is to min all freqs in bands 0-3 and max last band (4)
    // ie: attempt a HPF, to reduce (~15dB) all freqs in bands 0-3, boost band 4
    // repeat calls to -15 dB improves sound reduction <- check works in android eq
    // band4 to maxEQ will prob not do anything useful in NUHF range?

             on dev device:
            default EQ preset name is: Normal
            default.settings toString: Equalizer;curPreset=0;numBands=5;band1Level=300;band2Level=0;band3Level=0;band4Level=0;band5Level=300
            band1 is band0 at 30Hz - 120Hz, center at 60Hz, level(gain in milB) = 300 // +3dB
            band2,3,4 level (gain) = 0 // +0dB
            band5 is band4 at 7kHz - 20kHz, center at 1kHz, level(gain in milB) = 300 // +3dB

                NOTE::
                dev device S4 returns:
                12 presets
                adjustEq 1000 mB
                has band 5 at center 16kHz, min 8kHz, max 24kHz
                and band 0 at center 15Hz, min 7hz, max 23Hz


                dev device S5 returns:
                bands: 5
                minEQ: -1500 (-15 dB)
                maxEQ: 1500  (+15 dB)
                eqLevelRange: 2
                band 0
                    ctr: 60
                    min: 30
                    max: 120
                band 1
                    ctr: 230
                    min: 120
                    max: 460
                band 2
                    ctr: 910
                    min: 460
                    max: 1800
                band 3
                    ctr: 3600
                    min: 1800
                    max: 7000
                band 4
                    ctr: 14000
                    min: 7000
                    max: 20000

notes: media/libeffects/lvm/lib/Eq/lib/LVEQNB.h
    /*      Gain        is in integer dB, range -15dB to +15dB inclusive                    */
/*      Frequency   is the centre frequency in Hz, range DC to Nyquist                  */
/*      QFactor     is the Q multiplied by 100, range 0.25 (25) to 12 (1200)            */
/*                                                                                      */
/*  Example:                                                                            */
/*      Gain = 7            7dB gain                                                    */
/*      Frequency = 2467    Centre frequency = 2.467kHz                                 */
    /*      QFactor = 1089      Q = 10.89

    // --> THERE'S A Q ?

*/

