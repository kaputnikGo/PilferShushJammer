package cityfreqs.com.pilfershushjammer.jammers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.util.Random;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;

//check getResource calls in debugLogger
public class ActiveJammer {
    private static final String TAG = "PilferShush_ACTIVE";
    private final Context context;
    private final Bundle audioBundle;
    private float amplitude;
    private AudioTrack audioTrack;
    private AudioAttributes playbackAttributes;
    private AudioFormat audioFormat;
    private boolean isPlaying;
    private Thread jammerThread;
    private final boolean DEBUG;

    private byte[] soundData;
    private int driftSpeed;
    private double level;
    private double K;
    private double f;
    private double angle;
    private short[] shortBuffer;
    private double frequency;
    private int bufferSize;
    private int waveform;

    public ActiveJammer(Context context, Bundle audioBundle) {
        this.context = context;
        this.audioBundle = audioBundle;
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
        resetActiveJammer();
    }

    private void resetActiveJammer() {
        amplitude = 1.0f; // unity gain
        audioTrack = null;
        isPlaying = false;
        soundData = new byte[audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1])];
        driftSpeed = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[11]) * AudioSettings.DRIFT_SPEED_MULTIPLIER; // get into ms ranges
        // new version Active jammer vars
        bufferSize = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[6]);
        angle = 0;
        level = Short.MAX_VALUE; // 32767
        K = AudioSettings.TWO_PI / audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]);
        frequency = 1000; // default start tone
        f = frequency;
        angle = 0.0;
        waveform = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[18]);
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
        debugLogger("active play type: " + type, true);
        debugLogger("buffer size: " + bufferSize, true);
        debugLogger("waveform: " + waveform, true);
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
                    // if/else for += API 26 (Oreo, 8.0) deprecation stream_types for focus
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        playbackAttributes = new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build();

                        audioFormat = new AudioFormat.Builder()
                                .setEncoding(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[17]))
                                .setSampleRate(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]))
                                .setChannelMask(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[5]))
                                .build();

                        audioTrack = new AudioTrack(playbackAttributes,
                                audioFormat,
                                audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[4]),
                                AudioTrack.MODE_STREAM,
                                AudioManager.AUDIO_SESSION_ID_GENERATE);

                        audioTrack.setVolume(amplitude);
                    }
                    else {
                        // API 23 wants AudioTrack.builder with no inst StreamType
                        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[1]),
                                audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[5]),
                                audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[3]),
                                audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[6]),
                                AudioTrack.MODE_STREAM);

                        audioTrack.setStereoVolume(amplitude, amplitude);
                    }

                    if (audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12])) {
                        onboardEQ(audioTrack.getAudioSessionId());
                    }
                    // AudioTrack doesn't like an empty buffer, add an empty one until proper jammer tones
                    // but: AudioTrack will wait until it has enough data before starting.
                    // this probably causes the non-sensible buffer values to be played
                    audioTrack.play();
                    shortBuffer = new short[bufferSize];
                    angle = 0.0; // call here else in tone gen code produces clicks, non-zero-crossings

                    while (isPlaying) {
                        if (type == AudioSettings.JAMMER_NOISE) {
                            createWhiteNoise();
                        }
                        if (type == AudioSettings.JAMMER_TONE) {
                            createTone();
                        }
                        if (type == AudioSettings.JAMMER_SHADOW) {
                            createShadowSound();
                        }
                    }
                }
                catch (Exception ex) {
                    debugLogger(context.getResources().getString(R.string.active_state_1) + ex, true);
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
                return getFullRangeDrift();
        }
    }

    private synchronized void createTone() {
        // The format specified in the AudioTrack constructor should be AudioFormat.ENCODING_PCM_8BIT
        //  to correspond to the data in the array.
        // yet: The format can be AudioFormat.ENCODING_PCM_16BIT, but this is deprecated.
        // and: Audio data format: PCM 8 bit per sample. Not guaranteed to be supported by devices.
        frequency = loadDriftTone();
        // every nth iteration get a new drift freq (48k rate / driftSpeed )
        for (int i = 0; i < shortBuffer.length; i++) {
            if (audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[8]) != AudioSettings.JAMMER_TYPE_TEST && i % driftSpeed == 0) {
                // only at zero-crossing
                if (angle == 0.0) frequency = loadDriftTone();
            }
            //f += frequency / bufferSize; // slow frequency sweep up/down loop
            f += (frequency - f) / bufferSize;
            angle += (angle < Math.PI) ? f * K : (f * K) - (AudioSettings.TWO_PI);
            switch (waveform) {
                case AudioSettings.WAVEFORM_SIN:
                    shortBuffer[i] = (short) Math.round(Math.sin(angle) * level);
                    break;
                case AudioSettings.WAVEFORM_SQR:
                    shortBuffer[i] = (short) ((angle > 0.0) ? level : -level);
                    break;
                case AudioSettings.WAVEFORM_SAW:
                    shortBuffer[i] = (short) Math.round((angle / Math.PI) * level);
                    break;
            }
        }
        playSound(shortBuffer);
    }

    private synchronized void createWhiteNoise() {
        new Random().nextBytes(soundData);

        for (int i = 0; i < soundData.length; i++) {
            soundData[i] *= amplitude;
        }
        playSound(soundData);
    }

    // shadowTone notes:
    // only functions with random hz within range 25000-26000
    // should change freq every ~ 0.45 ms
    // output dB on dev S5 device:
    // 20kHz == -9 dB
    // 21kHz == -18 dB
    // 22kHz == -30 dB
    // 23kHz == -55 dB
    // 23500Hz == -72dB
    // 23950Hz == null
    private synchronized void createShadowSound() {
        // n.b. with current devices this is NOT an example of NUHF creating shadow bands,
        // in MEMs microphones but merely artifacts produced in code and/or speaker output
        // this code is here as placeholder for if/when devices get suitable output capabilities
        //TODO
        // profile freq = freq, freq += freq for useful variations compared to freq += freq - f
        frequency = getShadowTone(); //22000.0
        f = frequency;
        for (int i = 0; i < shortBuffer.length; i++) {
            f += (frequency - f) / bufferSize;
            angle += (angle < Math.PI) ? f * K : (f * K) - (AudioSettings.TWO_PI);
            switch (waveform) {
                case AudioSettings.WAVEFORM_SIN:
                    shortBuffer[i] = (short) Math.round(Math.sin(angle) * level);
                    break;
                case AudioSettings.WAVEFORM_SQR:
                    shortBuffer[i] = (short) ((angle > 0.0) ? level : -level);
                    break;
                case AudioSettings.WAVEFORM_SAW:
                    shortBuffer[i] = (short) Math.round((angle / Math.PI) * level);
                    break;
            }
        }
        playSound(shortBuffer);
    }

    // short audioTrack version
    private synchronized void playSound(short[] shortBuffer) {
        if (audioBundle == null) {
            debugLogger(context.getResources().getString(R.string.audio_check_3), true);
            return;
        }
        try {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.write(shortBuffer, 0, shortBuffer.length);
            }
            else {
                debugLogger(context.getResources().getString(R.string.audio_check_3), false);
            }
        }
        catch (Exception e) {
            debugLogger(context.getResources().getString(R.string.audio_check_4), true);
        }
    }

    // byte audioTrack version
    private synchronized void playSound(byte[] soundData) {
        if (audioBundle == null) {
            debugLogger(context.getResources().getString(R.string.audio_check_3), true);
            return;
        }
        try {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
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

    // based upon AudioChecker creating a settings property
    // ad-hocs tests show NUHF boosted to -12dB @ 22kHz
    // even though dev device reports eq band 4 max 20kHz
    private void onboardEQ(int audioSessionId) {
        try {
            Equalizer equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            equalizer.setProperties(new Equalizer.Settings(audioBundle.getString(AudioSettings.AUDIO_BUNDLE_KEYS[19])));
            debugLogger("ActiveJammer onboardEQ set to PSJAM: " + equalizer.getProperties().toString(), true);
        }
        catch (Exception ex) {
            debugLogger("ActiveJammer onboardEQ Exception.", true);
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

    private double getShadowTone() {
        // get a random frequency ideally between 25kHz and 26kHz (maxFreq - 1000)
        // but most devices only capable of 23.5kHz - 24kHz - so shadow concept not work
        int min;
        int max;
        // best possible for standard device (48kHz) including dev
        if (audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]) == AudioSettings.SHADOW_CARRIER_FREQUENCY) {
            // return random from carrier to minimum as range
            max = AudioSettings.SHADOW_MINIMUM_FREQUENCY;
            min = max - AudioSettings.SHADOW_DRIFT_RANGE;
        }
        // check if its above (a newer device maybe) then adjust range
        // if it is >= CEILING, then we have optimal device
        else if (audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]) >= AudioSettings.SHADOW_CEILING_FREQUENCY) {
            max = AudioSettings.SHADOW_CEILING_FREQUENCY;
            min = max - AudioSettings.SHADOW_FLOOR_FREQUENCY;
        }
        // lastly if its under the minimum
        else {
            // get from device maxFreq and minus 500
            max = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);
            min = max - AudioSettings.SHADOW_DRIFT_RANGE;
        }
        return new Random().nextInt(max - min) + min;
    }

    // carrier should be between 18k - 24k
    private int getUserRangedDrift(int carrierFrequency, int limit) {
        carrierFrequency = conformCarrierFrequency(carrierFrequency);
        int min = conformMinimumRangedValue(carrierFrequency - limit);
        int max = conformMaximumRangedValue(carrierFrequency + limit);

        return new Random().nextInt(max - min) + min;
    }

    private int getFullRangeDrift() {
        // get random within range of device maximum output freq and useful minimum of 100hz
        return new Random().nextInt(audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]) - 100) + 100;
    }

    private int conformCarrierFrequency(int carrier) {
        if (carrier < AudioSettings.MINIMUM_NUHF_FREQUENCY)
            carrier = AudioSettings.MINIMUM_NUHF_FREQUENCY;

        if (carrier > audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]))
            carrier = audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);
        return carrier;
    }

    private int conformMinimumRangedValue(int minValue) {
        return Math.max(minValue, AudioSettings.MINIMUM_NUHF_FREQUENCY);
    }

    private int conformMaximumRangedValue(int maxValue) {
        return Math.min(maxValue, audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]));
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
