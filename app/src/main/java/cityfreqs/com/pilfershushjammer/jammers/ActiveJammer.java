package cityfreqs.com.pilfershushjammer.jammers;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.util.Log;

import java.util.Random;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class ActiveJammer {
    private static final String TAG = "PilferShush_ACTIVE";
    private Context context;
    private Bundle audioBundle;
    private float amplitude;
    private AudioTrack audioTrack;
    private boolean isPlaying;
    private Thread jammerThread;
    private boolean DEBUG;

    public ActiveJammer(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
        resetActiveJammer();
    }

    private void resetActiveJammer() {
        amplitude = 1.0f;
        audioTrack = null;
        isPlaying = false;
    }

    /*
        PUBLIC CONTROLS
     */
    void play(int type) {
        // FFT to find key NUHF freq in the environment and tune jammer to it?
        if (isPlaying) {
            return;
        }
        //stop();
        isPlaying = true;
        threadPlay(type);
    }

    void stop() {
        isPlaying = false;
        if (audioTrack == null) {
            return;
        }
        stopPlayer();
    }

    /*
        AUDIO PLAY FUNCTIONS
     */
    private synchronized void threadPlay(int typeIn) {
        final int type = typeIn;
        jammerThread = new Thread() {
            public void run() {
                try {
                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                            audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]),
                            audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[5]),
                            audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]),
                            audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[6]),
                            AudioTrack.MODE_STREAM);

                    audioTrack.setStereoVolume(amplitude, amplitude);

                    if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12])) {
                        onboardEQ(audioTrack.getAudioSessionId());
                    }
                    // AudioTrack doesn't like an empty buffer, add an empty one until proper jammer tones
                    // but: AudioTrack will wait until it has enough data before starting.
                    audioTrack.play();

                    while (isPlaying) {
                        if (type == AudioSettings.JAMMER_TONE) {
                            createTone();
                        }
                        if (type == AudioSettings.JAMMER_WHITE) {
                            createWhiteNoise();
                        }
                    }
                }
                catch (Exception ex) {
                    Log.d("PSJammer", context.getResources().getString(R.string.active_state_1));
                }
            }
        };
        jammerThread.start();
    }

    private void stopPlayer() {
        isPlaying = false;
        if (jammerThread != null) {
            try {
                jammerThread.interrupt();
                jammerThread.join();
                jammerThread = null;
            }
            catch (Exception ex) {
                debugLogger(context.getResources().getString(R.string.active_state_2), true);
            }
        }
        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.release();
                audioTrack = null;
            }
            catch (IllegalStateException e) {
                debugLogger(context.getResources().getString(R.string.active_state_3), true);
            }
        }
    }

    private synchronized int loadDriftTone() {
        switch (audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[8])) {
            case AudioSettings.JAMMER_TYPE_TEST:
                return getTestDrift();

            case AudioSettings.JAMMER_TYPE_NUHF:
                return getNuhfDrift(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]));

            case AudioSettings.JAMMER_TYPE_DEFAULT_RANGED:
                return getDefaultRangedDrift(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[9]));

            case AudioSettings.JAMMER_TYPE_USER_RANGED:
                return getUserRangedDrift(
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[9]),
                        audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[10]));

            default:
                return getTestDrift();
        }
    }

    private synchronized void createTone() {
        int sampleRate = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]);
        int driftSpeed = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[11]) * AudioSettings.DRIFT_SPEED_MULTIPLIER; // get into ms ranges
        double[] sample = new double[sampleRate];
        byte[] soundData = new byte[2 * sampleRate];

        // NOTES: remove clicks from android audio emit, waveform at pop indicates no zero crossings either side

        // The format specified in the AudioTrack constructor should be AudioFormat.ENCODING_PCM_8BIT
        //  to correspond to the data in the array.
        // yet: The format can be AudioFormat.ENCODING_PCM_16BIT, but this is deprecated.

        // and: Audio data format: PCM 8 bit per sample. Not guaranteed to be supported by devices.



        // - AMPLITUDE RAMPS pre and post every loadDriftTone()
        // - ZERO VALUE SAMPLES either side of loadDriftTone()

        int driftFreq = loadDriftTone();
        // every nth iteration get a new drift freq (48k rate / driftSpeed )
        for (int i = 0; i < sampleRate; ++i) {
            if (audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[8]) != AudioSettings.JAMMER_TYPE_TEST && i % driftSpeed == 0) {
                driftFreq = loadDriftTone();
            }
            sample[i] = Math.sin(driftFreq * 2 * Math.PI * i / sampleRate);
        }

        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767)); // max the amplitude
            // in 16 bit wav PCM, first byte is the low order byte
            soundData[idx] = (byte) (val & 0x00ff);
            idx++;
            soundData[idx] = (byte) ((val & 0xff00) >>> 8);
            idx++;
        }
        playSound(soundData);
    }

    private synchronized void createWhiteNoise() {
        byte[] soundData = new byte[audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1])];
        new Random().nextBytes(soundData);

        for (int i = 0; i < soundData.length; i++) {
            soundData[i] *= amplitude;
        }
        playSound(soundData);
    }

    private synchronized void playSound(byte[] soundData) {
        if (audioBundle == null) {
            debugLogger(context.getResources().getString(R.string.audio_check_3), true);
            return;
        }

        try {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                //audioTrack.play();
                audioTrack.write(soundData, 0, soundData.length);
            }
            else {
                debugLogger(context.getResources().getString(R.string.audio_check_3), false);
            }
        }
        catch (Exception e) {
            debugLogger(context.getResources().getString(R.string.audio_check_4), true);
        }
    }

    private void onboardEQ(int audioSessionId) {
        try {
            Equalizer equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            short bands = equalizer.getNumberOfBands();
            final short minEQ = equalizer.getBandLevelRange()[0];
            final short maxEQ = equalizer.getBandLevelRange()[1];

            // attempt a HPF, to reduce (~15dB) all freqs in bands 0-3, boost band 4
            for (int i = 0; i < 2; i++) {
                for (short j = 0; j < bands; j++) {
                    equalizer.setBandLevel(j, minEQ);
                }
                // boost band 4 twice
                equalizer.setBandLevel((short)4, maxEQ);
            }
        }
        catch (Exception ex) {
            debugLogger("onboardEQ Exception.", true);
            ex.printStackTrace();
        }
    }

    /*
        JAMMER CHECKER FUNCTIONS
    */

    private int getTestDrift() {
        return new Random().nextInt(AudioSettings.MAXIMUM_TEST_FREQUENCY
                - AudioSettings.MINIMUM_TEST_FREQUENCY)
                + AudioSettings.MINIMUM_TEST_FREQUENCY;
    }

    private int getNuhfDrift(int maxFreq) {
        return new Random().nextInt(maxFreq
                - AudioSettings.MINIMUM_NUHF_FREQUENCY)
                + AudioSettings.MINIMUM_NUHF_FREQUENCY;
    }

    private int getDefaultRangedDrift(int carrierFrequency) {
        int min = conformMinimumRangedValue(carrierFrequency - AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT);
        int max = conformMaximumRangedValue(carrierFrequency + AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT);

        return new Random().nextInt(max - min) + min;
    }

    // carrier should be between 18k - 24k
    private int getUserRangedDrift(int carrierFrequency, int limit) {
        carrierFrequency = conformCarrierFrequency(carrierFrequency);
        int min = conformMinimumRangedValue(carrierFrequency - limit);
        int max = conformMaximumRangedValue(carrierFrequency + limit);

        return new Random().nextInt(max - min) + min;
    }

    private int conformCarrierFrequency(int carrier) {
        if (carrier < AudioSettings.MINIMUM_NUHF_FREQUENCY)
            carrier = AudioSettings.MINIMUM_NUHF_FREQUENCY;

        if (carrier > audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]))
            carrier = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);
        return carrier;
    }

    private int conformMinimumRangedValue(int minValue) {
        if (minValue >= AudioSettings.MINIMUM_NUHF_FREQUENCY)
            return minValue;
        else
            return AudioSettings.MINIMUM_NUHF_FREQUENCY;
    }

    private int conformMaximumRangedValue(int maxValue) {
        if (maxValue <= audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]))
            return maxValue;
        else
            return audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);
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
