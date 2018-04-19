package jammer.pilfershush.cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioRecord;

public class PassiveJammer {
    Context context;
    AudioSettings audioSettings;
    AudioRecord audioRecord;

    private boolean RUN_PASSIVE_JAMMER;

    public PassiveJammer(Context context, AudioSettings audioSettings) {
        this.context = context;
        this.audioSettings = audioSettings;
        RUN_PASSIVE_JAMMER = false;
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

                MainActivity.entryLogger("Passive Jammer init.", false);
                return true;
            }
            catch (Exception ex) {
                ex.printStackTrace();
                MainActivity.entryLogger("Passive Jammer failed to init.", true);
            }
        }
        return false;
    }

    // TODO determine how little is needed to occupy/hold the microphone without actually recording and saving any audio
    // assuming that only one api call can be made to the mic at a time
    protected void runPassiveJammer() {
        if ((audioRecord != null) || (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)) {
            try {
                // android source says: Transport Control Method, Starts recording from the AudioRecord instance.
                /*
                After it's created the track is not active. Call start() to make it active. <--
                AudioRecord.java
                if (native_start(MediaSyncEvent.SYNC_EVENT_NONE, 0) == SUCCESS)
                status_t start(int [AudioSystem::sync_event_t] event, int triggerSession)
                */
                audioRecord.startRecording();
                MainActivity.entryLogger("Passive Jammer start.", false);

                short buffer[] = new short[audioSettings.getBufferSize()];
                int audioStatus = audioRecord.read(buffer, 0, audioSettings.getBufferSize());
                // check for error on pre 6.x and 6.x API
                if (audioStatus == AudioRecord.ERROR_INVALID_OPERATION
                        || audioStatus == AudioRecord.STATE_UNINITIALIZED) {
                    MainActivity.entryLogger("Passive Jammer audio status: error.", true);
                }

                // TODO check is this state enough
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    //MainActivity.entryLogger("Passive Jammer audio status: running.", true);
                    RUN_PASSIVE_JAMMER = true;
                }
                else {
                    MainActivity.entryLogger("Passive Jammer audio status: NOT running, status: " + audioStatus, true);
                }

                // TODO prefer not to do any of this below
                // android source says: Audio data supply, Reads audio data from the audio hardware for recording into a buffer.
                /*
                snip AudioRecord.cpp
                    ssize_t AudioRecord::read(void* buffer, size_t userSize)
                      memcpy(buffer, audioBuffer.i8, bytesRead);
                      read += bytesRead;
                      return read;
                */

                /*
                short[] tempBuffer = new short[audioSettings.getBufferSize()];;
                do {
                    audioRecord.read(tempBuffer, 0, audioSettings.getBufferSize());
                } while (RUN_PASSIVE_JAMMER);
                */
            }
            catch (IllegalStateException exState) {
                exState.printStackTrace();
                MainActivity.entryLogger("Passive Jammer failed to run.", true);
            }
        }
    }

    protected void stopPassiveJammer() {
        // get AudioRecord object, null it, clean up
        if (audioRecord != null) {
            RUN_PASSIVE_JAMMER = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            MainActivity.entryLogger("Passive Jammer stop and release.", false);
        }
        else {
            MainActivity.entryLogger("Passive Jammer not running.", false);
        }
    }
}

