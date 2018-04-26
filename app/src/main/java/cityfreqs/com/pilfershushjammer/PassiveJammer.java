package cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioRecord;

import jammer.pilfershush.cityfreqs.com.pilfershushjammer.R;

public class PassiveJammer {
    Context context;
    AudioSettings audioSettings;
    AudioRecord audioRecord;

    public PassiveJammer(Context context, AudioSettings audioSettings) {
        this.context = context;
        this.audioSettings = audioSettings;
    }

    protected boolean startPassiveJammer() {
        // grab mic via AudioRecord object,
        // zero the input
        // battery use check, CPU use check
        if (audioRecord == null) {
            try {
                audioRecord = new AudioRecord(audioSettings.getAudioSource(),
                        audioSettings.getSampleRate(),
                        audioSettings.getChannelConfig(),
                        audioSettings.getEncoding(),
                        audioSettings.getBufferSize());

                MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_1), false);
                return true;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_2), true);
            }
        }
        return false;
    }

    // assumption:: only one api call can be made to the mic at a time

    protected boolean runPassiveJammer() {
        if ((audioRecord != null) || (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)) {
            try {
                // Transport Control Method, Starts recording from the AudioRecord instance.
                /*
                After it's created the track is not active. Call start() to make it active. <--
                AudioRecord.java
                if (native_start(MediaSyncEvent.SYNC_EVENT_NONE, 0) == SUCCESS)
                status_t start(int [AudioSystem::sync_event_t] event, int triggerSession)
                */
                audioRecord.startRecording();
                MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_3), false);

                // check for initialising audioRecord
                short buffer[] = new short[audioSettings.getBufferSize()];
                int audioStatus = audioRecord.read(buffer, 0, audioSettings.getBufferSize());
                // check for error on pre 6.x and 6.x API
                if (audioStatus == AudioRecord.ERROR_INVALID_OPERATION) {
                    MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_4), true);
                    return false;
                }
                else  if (audioStatus == AudioRecord.STATE_UNINITIALIZED) {
                    MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_5_1), true);
                    MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_5_2), true);
                    // adb:  status code -38
                    return false;
                }

                // check for running audioRecord
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    return true;
                }
                else {
                    MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_6), true);
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

                /*
                short[] tempBuffer = new short[audioSettings.getBufferSize()];;
                do {
                    audioRecord.read(tempBuffer, 0, audioSettings.getBufferSize());
                } while (true);
                */
            }
            catch (IllegalStateException exState) {
                exState.printStackTrace();
                MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_7), true);
                return false;
            }
        }
        // uninitialised state
        MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_8), true);
        return false;
    }

    protected void stopPassiveJammer() {
        // get AudioRecord object, null it, clean up
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_9), false);
        }
        else {
            MainActivity.entryLogger(context.getResources().getString(R.string.passive_state_10), false);
        }
    }
}

