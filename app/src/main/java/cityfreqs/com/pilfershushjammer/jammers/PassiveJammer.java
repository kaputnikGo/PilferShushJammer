package cityfreqs.com.pilfershushjammer.jammers;

import android.content.Context;
import android.media.AudioRecord;
import android.os.Bundle;
import android.util.Log;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class PassiveJammer {
    private static final String TAG = "PilferShush_PASSIVE";
    private Context context;
    private Bundle audioBundle;
    private AudioRecord audioRecord;
    private boolean DEBUG;

    public PassiveJammer(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
    }

    boolean startPassiveJammer() {
        if (audioRecord == null) {
            try {
                audioRecord = new AudioRecord(
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[0]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[2]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]));

                debugLogger(context.getResources().getString(R.string.passive_state_1), false);
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

    void stopPassiveJammer() {
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
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
