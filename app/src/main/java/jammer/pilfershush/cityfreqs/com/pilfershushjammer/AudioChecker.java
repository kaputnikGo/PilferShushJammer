package jammer.pilfershush.cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;

public class AudioChecker {
    private Context context;
    private AudioSettings audioSettings;

    private int sampleRate;
    private int bufferSize;
    private int encoding;
    private int channelConfig;
    private int audioSource;

    public AudioChecker(Context context, AudioSettings audioSettings) {
        this.context = context;
        this.audioSettings = audioSettings;
    }


    protected boolean determineInternalAudioType() {
        // guaranteed default for Android is 44.1kHz, PCM_16BIT, CHANNEL_IN_DEFAULT
        int buffSize = 0;
        for (int rate : AudioSettings.SAMPLE_RATES) {
            for (short audioFormat : new short[] {
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioFormat.ENCODING_PCM_8BIT}) {

                for (short channelConfig : new short[] {
                        AudioFormat.CHANNEL_IN_DEFAULT, // 1 - switched by OS, not native?
                        AudioFormat.CHANNEL_IN_MONO,    // 16, also CHANNEL_IN_FRONT == 16
                        AudioFormat.CHANNEL_IN_STEREO }) {  // 12
                    try {
                        MainActivity.entryLogger("Try rate " + rate + "Hz, bits: " + audioFormat + ", channelConfig: "+ channelConfig, false);
                        buffSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        // force buffSize to powersOfTwo if it isnt (ie.S5)
                        buffSize = AudioSettings.getClosestPowersHigh(buffSize);

                        if (buffSize != AudioRecord.ERROR_BAD_VALUE) {
                            AudioRecord recorder = new AudioRecord(
                                    audioSource,
                                    rate,
                                    channelConfig,
                                    audioFormat,
                                    buffSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                MainActivity.entryLogger("found, rate: " + rate + ", buffer: " + buffSize + ", channel count: " + recorder.getChannelCount(), false);
                                // set found values
                                // AudioRecord.getChannelCount() is number of input audio channels (1 is mono, 2 is stereo)
                                sampleRate = rate;
                                this.channelConfig = channelConfig;
                                encoding = audioFormat;
                                bufferSize = buffSize;
                                audioSettings.setBasicAudioSettings(sampleRate, bufferSize, encoding, this.channelConfig, recorder.getChannelCount());
                                audioSettings.setAudioSource(audioSource);
                                recorder.release();
                                return true;
                            }
                        }
                    }
                    catch (Exception e) {
                        MainActivity.entryLogger("Rate: " + rate + "Exception, keep trying, e:" + e.toString(), false);
                    }
                }
            }
        }
        MainActivity.entryLogger(context.getString(R.string.audio_check_1), true);
        return false;
    }

}
