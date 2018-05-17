package cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;

public class AudioChecker {
    private Context context;
    private AudioSettings audioSettings;

    private int sampleRate;
    private int bufferInSize;
    private int bufferOutSize;
    private int encoding;
    private int channelInConfig;
    private int channelOutConfig;
    private int audioSource;
    private Equalizer equalizer;

    public AudioChecker(Context context, AudioSettings audioSettings) {
        this.context = context;
        this.audioSettings = audioSettings;
    }


    protected boolean determineRecordAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        int buffSize = 0;
        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelInConfig : new short[] {
                        AudioFormat.CHANNEL_IN_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_IN_MONO,    // 16, also CHANNEL_IN_FRONT == 16
                        AudioFormat.CHANNEL_IN_STEREO }) {  // 12
                    try {
                        MainActivity.entryLogger("Try rate " + rate + "Hz, bits: " + audioFormat + ", channelInConfig: "+ channelInConfig, false);
                        buffSize = AudioRecord.getMinBufferSize(rate, channelInConfig, audioFormat);
                        // force buffSize to powersOfTwo if it isnt (ie.S5)
                        buffSize = AudioSettings.getClosestPowersHigh(buffSize);

                        if (buffSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    audioSource,
                                    rate,
                                    channelInConfig,
                                    audioFormat,
                                    buffSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                MainActivity.entryLogger("found: " + rate + ", buffer: " + buffSize + ", channel count: " + recorder.getChannelCount(), true);
                                // set found values
                                // AudioRecord.getChannelCount() is number of input audio channels (1 is mono, 2 is stereo)
                                sampleRate = rate;
                                this.channelInConfig = channelInConfig;
                                encoding = audioFormat;
                                bufferInSize = buffSize;
                                audioSettings.setBasicAudioInSettings(sampleRate, bufferInSize, encoding, this.channelInConfig, recorder.getChannelCount());
                                audioSettings.setAudioSource(audioSource);
                                recorder.release();
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        MainActivity.entryLogger("Error, keep trying.", false);
                    }
                }
            }
        }
        MainActivity.entryLogger(context.getString(R.string.audio_check_1), true);
        return false;
    }

    protected boolean determineOutputAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        int buffSize = 0;
        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelOutConfig : new short[] {
                        AudioFormat.CHANNEL_OUT_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_OUT_MONO,    // 4
                        AudioFormat.CHANNEL_OUT_STEREO }) {  // 12
                    try {
                        MainActivity.entryLogger("Try rate " + rate + "Hz, bits: " + audioFormat + ", channelOutConfig: "+ channelOutConfig, false);

                        buffSize = AudioTrack.getMinBufferSize(rate, channelOutConfig, audioFormat);
                        // dont need to force buffSize to powersOfTwo if it isnt (ie.S5) as no FFT

                        AudioTrack audioTrack = new AudioTrack(
                                AudioManager.STREAM_MUSIC,
                                rate,
                                channelOutConfig,
                                audioFormat,
                                buffSize,
                                AudioTrack.MODE_STREAM);

                        if (audioTrack != null) {
                            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {

                                MainActivity.entryLogger("found: " + rate + ", buffer: " + buffSize + ", channelOutConfig: " + channelOutConfig, true);
                                // set output values
                                // buffOutSize may not be same as buffInSize conformed to powersOfTwo
                                audioSettings.setChannelOutConfig(this.channelOutConfig = channelOutConfig);
                                audioSettings.setBufferOutSize(bufferOutSize = buffSize);

                                // test onboardEQ
                                MainActivity.entryLogger("\nTesting for device audiofx equalizer.", false);
                                if (testOnboardEQ(audioTrack.getAudioSessionId())) {
                                    MainActivity.entryLogger("Device audiofx equalizer test passed.\n", false);
                                    // set a thing somewhere so that active jammer can use it
                                    // add settings to AudioSettings
                                } else {
                                    MainActivity.entryLogger("Device audiofx equalizer test failed.\n", true);
                                }

                                audioTrack.pause();
                                audioTrack.flush();
                                audioTrack.release();
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        MainActivity.entryLogger("Error, keep trying.", false);
                    }
                }
            }
        }
        MainActivity.entryLogger(context.getString(R.string.audio_check_2), true);
        return false;
    }

    // testing android/media/audiofx/Equalizer
    // idea is to make the whitenoise less annoying
    private boolean testOnboardEQ(int audioSessionId) {
        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            audioSettings.setHasEQ(true);
            // get some info
            short bands = equalizer.getNumberOfBands();
            final short minEQ = equalizer.getBandLevelRange()[0]; // returns milliBel
            final short maxEQ = equalizer.getBandLevelRange()[1];

            MainActivity.entryLogger("Number EQ bands: " + bands, false);
            MainActivity.entryLogger("EQ min mB: " + minEQ, false);
            MainActivity.entryLogger("EQ max mB: " + maxEQ, false);

            for (short band = 0; band < bands; band++) {
                // divide by 1000 to get numbers into recognisable ranges
                MainActivity.entryLogger("\nband freq range min: " + (equalizer.getBandFreqRange(band)[0] / 1000), false);
                MainActivity.entryLogger("Band " + band + " center freq Hz: " + (equalizer.getCenterFreq(band) / 1000), true);
                MainActivity.entryLogger("band freq range max: " + (equalizer.getBandFreqRange(band)[1] / 1000), false);
                // band 5 reports center freq: 14kHz, minrange: 7000 and maxrange: 0  <- is this infinity? uppermost limit?
                // could be 21kHz if report standard of same min to max applies.
            }


            // only active test is to squash all freqs in bands 0-3, leaving last band (4) free...
            MainActivity.entryLogger("\nHPF test reduce EQ bands 2x loop by minEQ value: " + minEQ, false);

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
            MainActivity.entryLogger("testEQ Exception.", true);
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
