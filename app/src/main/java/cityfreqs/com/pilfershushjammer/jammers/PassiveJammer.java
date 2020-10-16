package cityfreqs.com.pilfershushjammer.jammers;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;
import cityfreqs.com.pilfershushjammer.utilities.RecordingCallback;


public class PassiveJammer {
    private static final String TAG = "PilferShush_PASSIVE";
    private final Context context;
    private final Bundle audioBundle;
    private AudioRecord audioRecord;
    private MediaRecorder placeboRecorder;
    private static String placeboMediaRecorderFileName;
    private RecordingCallback recordCallback;

    private final boolean DEBUG;

    public PassiveJammer(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
        // reserve a file handle in the application specific cache directory in the filesystem
        placeboMediaRecorderFileName = context.getCacheDir().getAbsolutePath();
        placeboMediaRecorderFileName += "/PilferShushPlacebo.raw";
        // it is never written to.

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recordCallback = new RecordingCallback(context);
        }
    }

    boolean startPassiveJammer() {
        if (audioRecord == null) {
            try {
                // this set to M/23 for AudioRecord.Builder in prep for API 30/Android 11
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    /*
                    // Configuration for capturing audio played by other apps - probably not necessary
                    MediaProjection mediaProjection;
                    // Retrieve a audio capable projection from the MediaProjectionManager
                    AudioPlaybackCaptureConfiguration config =
                            new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA) // .USAGE_UNKNOWN
                                    .build();
                    */
                    audioRecord = new AudioRecord.Builder()
                        .setAudioSource(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]))
                            .setAudioFormat(new AudioFormat.Builder()
                                //.setAudioPlaybackCaptureConfig(config) // api29 concurrent capture
                                //.setPrivacySensitive(true) // api30 block concurrent capture
                                .setEncoding(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]))
                                .setSampleRate(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]))
                                .setChannelMask(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[2]))
                                .build())
                        .setBufferSizeInBytes(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]))
                        .build();
                }
                else {
                    // fallback to the older method < M/23
                    audioRecord = new AudioRecord(
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[2]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));
                }

                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    registerCallback();
                }

                debugLogger(context.getResources().getString(R.string.passive_state_1), false);
                debugLogger("Jammer using mic source: " + audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]), false);
                return true;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                debugLogger(context.getResources().getString(R.string.passive_state_2), true);
            }
        }
        return false;
    }

    boolean runPassiveJammer() {
        if (audioRecord != null) {
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                try {
                    // Transport Control Method, Starts recording from the AudioRecord instance.
                    /*
                    After it's created the track is not active. Call start() to make it active.
                    AudioRecord.java
                    if (native_start(MediaSyncEvent.SYNC_EVENT_NONE, 0) == SUCCESS)
                    status_t start(int [AudioSystem::sync_event_t] event, int triggerSession)
                    */
                    audioRecord.startRecording();
                    debugLogger(context.getResources().getString(R.string.passive_state_3) + "\n", true);

                    // get PSJammer audio config
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        getAudioConfig();
                    }

                    // optional switch for accessing the hardware buffers via audioRecord.read()
                    // reason: possible battery use at hardware level
                    if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14])) {
                        debugLogger(context.getResources().getString(R.string.passive_state_11), true);
                        // check for initialising audioRecord
                        short[] buffer = new short[audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4])];

                        // returns either 0, number of shorts read, or an error code - not audio data
                        int audioStatus = audioRecord.read(buffer, 0, audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));
                        // check for error on pre 6.x and 6.x API
                        if (audioStatus == AudioRecord.ERROR_INVALID_OPERATION) {
                            debugLogger(context.getResources().getString(R.string.passive_state_4), true);
                            // error from improper use of method
                            return false;
                        }
                        else if (audioStatus == AudioRecord.STATE_UNINITIALIZED) {
                            debugLogger(context.getResources().getString(R.string.passive_state_5_1), true);
                            debugLogger(context.getResources().getString(R.string.passive_state_5_2), true);
                            // adb:  status code -38
                            return false;
                        }
                    }

                    // check for running audioRecord
                    if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                        // check MediaRecord placebo function running
                        if (runMediaRecorderPlacebo()) {
                            debugLogger(context.getResources().getString(R.string.passive_state_12), true);
                        }
                        else {
                            debugLogger(context.getResources().getString(R.string.passive_state_13), true);
                        }
                        return true;
                    }
                    else {
                        debugLogger(context.getResources().getString(R.string.passive_state_6), true);
                        return false;
                    }

                    /*
                    NOTE::
                    This is the point where reading the microphone hardware buffer would occur,

                    Audio data supply, Reads audio data from the audio hardware for recording into a buffer.
                    AudioRecord.cpp
                        ssize_t AudioRecord::read(void* buffer, size_t userSize)
                          memcpy(buffer, audioBuffer.i8, bytesRead);
                          read += bytesRead;
                          return read;
                    */

                    // the lines below demonstrate typical use of recording audio - not used in this app.
                    /*
                    short[] tempBuffer = new short[audioSettings.getBufferSize()];;
                    do {
                        audioRecord.read(tempBuffer, 0, audioSettings.getBufferSize());
                    } while (true);
                    */
                } catch (IllegalStateException exState) {
                    exState.printStackTrace();
                    debugLogger(context.getResources().getString(R.string.passive_state_7), true);
                    return false;
                }
            }
        }
        // uninitialised state
        debugLogger(context.getResources().getString(R.string.passive_state_8), true);
        return false;
    }

// Android 10 (Q) API 29 additions for concurrent audio mitigation
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void registerCallback() {
        // android 10 concurrent audio callback register, dont use main thread Looper? dead on exit?
        // "The callback is executed only when the app is receiving audio and a change occurs."
        audioRecord.registerAudioRecordingCallback(context.getMainExecutor(), recordCallback);

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void unregisterCallback() {
        // moto (Q) killed the callback after receiving it with an ex
        try {
            audioRecord.unregisterAudioRecordingCallback(recordCallback);
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void getAudioConfig() {
        AudioRecordingConfiguration recordConfig = audioRecord.getActiveRecordingConfiguration();
        if (recordConfig != null) {
            debugLogger("registerCallback config, silenced: " + recordConfig.isClientSilenced(), false);
        }
        else {
            debugLogger("registerCallback config null", true);
        }
    }

    private boolean runMediaRecorderPlacebo() {
        // as per changes to API28+ background mic use now only available to
        // foreground services using the MediaRecorder instance
        // change AudioSource here for Android 10 boost (VOICE_COMM or CAMCORDER or DEFAULT)
        placeboRecorder = new MediaRecorder();
        // check API 30 gets RuntimeException at android.media.MediaRecorder.setAudioSource (Native Method), if/else?
        // was:
        //(MediaRecorder.AudioSource.DEFAULT); // VOICE_COMMUNICATION
        // now using the mic source switch
        placeboRecorder.setAudioSource(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]));
        placeboRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        placeboRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        placeboRecorder.setOutputFile(placeboMediaRecorderFileName);

        debugLogger("Placebo runner prepare.", false);
        try {
            // MediaRecorder.java only checks for file exists, not mic hardware
            placeboRecorder.prepare();
        }
        catch (IOException e) {
            debugLogger(context.getResources().getString(R.string.passive_state_14), true);
            return false;
        }
        // need to prepare() the MediaRecorder() object only
        // AudioRecord does the actual mic blocking
        // will not use start() method as that writes audio data to file
        // recorder.start();

        return true;
    }

    private void stopMediaRecorderPlacebo() {
        if (placeboRecorder != null) {
            //placeboRecorder.stop(); // <- has not started so no need to stop.
            placeboRecorder.reset();
            placeboRecorder.release();
            placeboRecorder = null;
        }
        debugLogger(context.getResources().getString(R.string.passive_state_15), true);
    }

    void stopPassiveJammer() {
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                unregisterCallback();
            }
            audioRecord.release();
            audioRecord = null;
            // testing
            stopMediaRecorderPlacebo();
            //
            debugLogger(context.getResources().getString(R.string.passive_state_9), false);
        }
        else {
            debugLogger(context.getResources().getString(R.string.passive_state_10), false);
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
