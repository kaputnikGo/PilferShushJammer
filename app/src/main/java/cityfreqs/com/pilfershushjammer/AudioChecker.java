package cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;
import android.os.Bundle;

public class AudioChecker {
    private Context context;
    private Bundle audioBundle;
    private static final boolean DEBUG = false;

    protected AudioChecker(Context context) {
        this.context = context;
        audioBundle = new Bundle();
    }

    protected Bundle getAudioBundle() {
        return audioBundle;
    }

    private int getClosestPowersHigh(int reported) {
        // return the next highest power from the minimum reported
        // 512, 1024, 2048, 4096, 8192, 16384
        for (int power : AudioSettings.POWERS_TWO_HIGH) {
            if (reported <= power) {
                return power;
            }
        }
        // didn't find power, return reported
        return reported;
    }

    protected boolean determineRecordAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        int buffSize;
        int audioSource = 0; // MediaRecorder.AudioSource.DEFAULT

        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelInConfig : new short[] {
                        AudioFormat.CHANNEL_IN_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_IN_MONO,    // 16, also CHANNEL_IN_FRONT == 16
                        AudioFormat.CHANNEL_IN_STEREO }) {  // 12
                    try {
                        if (DEBUG) MainActivity.entryLogger("Try rate " + rate + "Hz, bits: " + audioFormat + ", channelInConfig: "+ channelInConfig, false);
                        buffSize = AudioRecord.getMinBufferSize(rate, channelInConfig, audioFormat);
                        // force buffSize to powersOfTwo if it isnt (ie.S5)
                        buffSize = getClosestPowersHigh(buffSize);

                        if (buffSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    audioSource,
                                    rate,
                                    channelInConfig,
                                    audioFormat,
                                    buffSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                // AudioRecord.getChannelCount() is number of input audio channels (1 is mono, 2 is stereo)
                                if (DEBUG) MainActivity.entryLogger("found: " + rate + ", buffer: " + buffSize + ", channel count: " + recorder.getChannelCount(), true);
                                // set found values
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[0], audioSource);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[1], rate);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[2], channelInConfig);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[3], audioFormat);
                                audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[4], buffSize);

                                recorder.release();
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        if (DEBUG) MainActivity.entryLogger("Error, keep trying.", false);
                    }
                }
            }
        }
        return false;
    }

    protected boolean determineOutputAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        int buffSize;
        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelOutConfig : new short[] {
                        AudioFormat.CHANNEL_OUT_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_OUT_MONO,    // 4
                        AudioFormat.CHANNEL_OUT_STEREO }) {  // 12
                    try {
                        if (DEBUG) MainActivity.entryLogger("Try rate " + rate + "Hz, bits: " + audioFormat + ", channelOutConfig: "+ channelOutConfig, false);

                        buffSize = AudioTrack.getMinBufferSize(rate, channelOutConfig, audioFormat);
                        // dont need to force buffSize to powersOfTwo if it isnt (ie.S5) as no FFT

                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                rate,
                                channelOutConfig,
                                audioFormat,
                                buffSize,
                                AudioTrack.MODE_STREAM);


                        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                            if (DEBUG) MainActivity.entryLogger("found: " + rate + ", buffer: " + buffSize + ", channelOutConfig: " + channelOutConfig, true);
                            // set output values
                            // buffOutSize may not be same as buffInSize conformed to powersOfTwo
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[5], channelOutConfig);
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[6], buffSize);
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[13], (int)(rate * 0.5f));

                            // test onboardEQ
                            if (testOnboardEQ(audioTrack.getAudioSessionId())) {
                                if (DEBUG) MainActivity.entryLogger(context.getString(R.string.eq_check_2)+ "\n", false);
                                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12], true);
                            }
                            else {
                                if (DEBUG) MainActivity.entryLogger(context.getString(R.string.eq_check_3) + "\n", true);
                                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[12], false);
                            }
                            audioTrack.pause();
                            audioTrack.flush();
                            audioTrack.release();
                            return true;
                        }
                    }
                    catch (Exception e) {
                        if (DEBUG) MainActivity.entryLogger("Error, keep trying.", false);
                    }
                }
            }
        }
        return false;
    }

    // testing android/media/audiofx/Equalizer
    // idea is to make the whitenoise less annoying
    private boolean testOnboardEQ(int audioSessionId) {
        try {
            Equalizer equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            // get some info
            short bands = equalizer.getNumberOfBands();
            final short minEQ = equalizer.getBandLevelRange()[0]; // returns milliBel
            final short maxEQ = equalizer.getBandLevelRange()[1];

            if (DEBUG) {
                MainActivity.entryLogger("\n" + context.getString(R.string.eq_check_1), false);
                MainActivity.entryLogger(context.getString(R.string.eq_check_4) + bands, false);
                MainActivity.entryLogger(context.getString(R.string.eq_check_5) + minEQ, false);
                MainActivity.entryLogger(context.getString(R.string.eq_check_6) + maxEQ, false);
            }

            if (DEBUG) {
                for (short band = 0; band < bands; band++) {
                    // divide by 1000 to get numbers into recognisable ranges
                    MainActivity.entryLogger("\nband freq range min: " + (equalizer.getBandFreqRange(band)[0] / 1000), false);
                    MainActivity.entryLogger("Band " + band + " center freq Hz: " + (equalizer.getCenterFreq(band) / 1000), true);
                    MainActivity.entryLogger("band freq range max: " + (equalizer.getBandFreqRange(band)[1] / 1000), false);
                    // band 5 reports center freq: 14kHz, minrange: 7000 and maxrange: 0  <- is this infinity? uppermost limit?
                    // could be 21kHz if report standard of same min to max applies.
                }
            }
            // only active test is to squash all freqs in bands 0-3, leaving last band (4) free...
            if (DEBUG) MainActivity.entryLogger("\n"+ context.getString(R.string.eq_check_7) + minEQ, false);

            for (int i = 0; i < 2; i++) {
                for (short j = 0; j < bands; j++) {
                    equalizer.setBandLevel(j, minEQ);
                }
            }
            // not a filter... reduced amplitude seems the best description when using eq.
            // repeat calls to -15 dB improves sound reduction
            // band4 to maxEQ will prob not do anything useful?

            return true;
        }
        catch (Exception ex) {
            MainActivity.entryLogger(context.getString(R.string.eq_check_8), true);
            ex.printStackTrace();
            return false;
        }
    }
}
/*
                S5 returns:
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
                    max: 0

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
