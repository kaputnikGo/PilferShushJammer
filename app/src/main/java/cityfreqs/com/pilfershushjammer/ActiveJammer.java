package cityfreqs.com.pilfershushjammer;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.Equalizer;

import java.util.Random;

public class ActiveJammer {
    private Context context;
    private AudioSettings audioSettings;

    private double carrierFrequency;
    private int volume;
    private float amplitude;
    private double maximumDeviceFrequency;
    private boolean maximumDeviceFrequencyOverride;

    private AudioTrack audioTrack;
    private boolean isPlaying;
    private boolean testRangeSwitch;
    private int audioSessionId;
    private Equalizer equalizer;

    private Thread jammerThread;

    public ActiveJammer(Context context, AudioSettings audioSettings) {
        this.context = context;
        this.audioSettings = audioSettings;
        testRangeSwitch = false;
        audioSessionId = 0;

        resetActiveJammer();
    }

    public void resetActiveJammer() {
        volume = 100;
        amplitude = 1.0f;
        audioTrack = null;
        isPlaying = false;

        // device unique
        // ie. if sampleRate found is 22kHz, try run over it anyway
        // TODO - hard value, not the sampleRate * 0.5 as it is so far, below can be a default?
        maximumDeviceFrequency = audioSettings.getSampleRate() * 0.5;
        maximumDeviceFrequencyOverride = false;
    }

    /*
        PUBLIC CONTROLS
     */
    public void play(int type) {
        //TODO add control to manually sweep carrierFrequency?
        // or an FFT to find key NUHF freq in the environment and tune jammer to it...
        if (isPlaying) {
            return;
        }
        //stop();
        isPlaying = true;
        threadPlay(type);
    }

    public void stop() {
        isPlaying = false;
        if (audioTrack == null) {
            return;
        }
        stopPlayer();
    }

    public void setVolume(int volume) {
        // 0 - 100
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setAmplitude(float amplitude) {
        // 0.0f - 1.0f
        this.amplitude = amplitude;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public double getCarrierFrequency() {
        return carrierFrequency;
    }

    public void setTestRangeSwitch(boolean testRangeSwitch) {
        this.testRangeSwitch = testRangeSwitch;
    }
    public boolean getTestRangeSwitch() {
        return testRangeSwitch;
    }

    public int getAudioSessionId() {
        return audioSessionId;
    }

    public void setCarrierFrequency(double carrierFrequency) {
        // allow option/switch to override device reported maximum
        if (carrierFrequency > maximumDeviceFrequency && !maximumDeviceFrequencyOverride) {
            // note this, and restrict:
            MainActivity.entryLogger(context.getResources().getString(R.string.audio_check_5), false);
            this.carrierFrequency = maximumDeviceFrequency;
        }
        else {
            this.carrierFrequency = carrierFrequency;
        }
    }

    public void setMaximumDeviceFrequencyOverride(boolean override) {
        maximumDeviceFrequencyOverride = override;
    }

    public boolean getMaximumDeviceFrequencyOverride() {
        return maximumDeviceFrequencyOverride;
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
                            audioSettings.getSampleRate(),
                            audioSettings.getChannelOutConfig(),
                            audioSettings.getEncoding(),
                            audioSettings.getBufferOutSize(),
                            AudioTrack.MODE_STREAM);

                    audioTrack.setStereoVolume(amplitude, amplitude);

                    if (audioSettings.getHasEQ()) {
                        onboardEQ(audioTrack.getAudioSessionId());
                    }

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
                    MainActivity.entryLogger(context.getResources().getString(R.string.active_state_1), true);
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
                MainActivity.entryLogger(context.getResources().getString(R.string.active_state_2), true);
            }
        }
        try {
            audioTrack.pause();
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
        catch (IllegalStateException e) {
            MainActivity.entryLogger(context.getResources().getString(R.string.active_state_3), true);
        }
    }

    private synchronized void createTone() {
        //TODO UI controls
        // change testRangeSwitch boolean to an int for different drift modes
        double sample[] = new double[audioSettings.getSampleRate()];
        byte soundData[] = new byte[2 * audioSettings.getSampleRate()];

        int driftFreq;
        if (testRangeSwitch)
            driftFreq = AudioSettings.getTestValue();
        else
            driftFreq = AudioSettings.getNuhfValue();

        // TODO different modes here:
        // driftFreq = (int)carrierFrequency;
        // driftFreq = AudioSettings.getDefaultRangedValue(int carrierFrequency)
        // driftFreq = AudioSettings.getUserRangedValue(int carrierFrequency, int limit)

        for (int i = 0; i < audioSettings.getSampleRate(); ++i) {
            sample[i] = Math.sin(
                    driftFreq * 2 * Math.PI * i / (audioSettings.getSampleRate()));
        }

        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            soundData[idx++] = (byte) (val & 0x00ff);
            soundData[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        playSound(soundData);
    }

    private synchronized void createWhiteNoise() {
        //int soundBufferLength = 500;
        int numSamples = 500 * (audioSettings.getSampleRate() / 1000);
        byte soundData[] = new byte[2 * numSamples];
        new Random().nextBytes(soundData);

        for (int i = 0; i < soundData.length; i++) {
            soundData[i] *= amplitude;
        }
        playSound(soundData);
    }

    private synchronized void playSound(byte[] soundData) {
        if (audioSettings == null) {
            MainActivity.entryLogger(context.getResources().getString(R.string.audio_check_3), true);
            return;
        }

        try {
            if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                audioTrack.play();
                audioTrack.write(soundData, 0, soundData.length);
            }
            else {
                MainActivity.entryLogger(context.getResources().getString(R.string.audio_check_3), true);
                return;
            }
        }
        catch (Exception e) {
            MainActivity.entryLogger(context.getResources().getString(R.string.audio_check_4), true);
        }
    }


    // this works reasonably well for the tone, but not whitenoise.
    private void onboardEQ(int audioSessionId) {
        try {
            equalizer = new Equalizer(0, audioSessionId);
            equalizer.setEnabled(true);
            short bands = equalizer.getNumberOfBands();
            final short minEQ = equalizer.getBandLevelRange()[0];
            final short maxEQ = equalizer.getBandLevelRange()[1];

            // attempt a HPF, to reduce (-15dB) all freqs in bands 0-3, leaving band 4 free...
            // run twice
            for (int i = 0; i < 2; i++) {
                for (short j = 0; j < bands; j++) {
                    equalizer.setBandLevel(j, minEQ);
                }
                // boost band 4 twice?
                //equalizer.setBandLevel((short)4, maxEQ);
            }
            // boost band 4 once only?
            equalizer.setBandLevel((short)4, maxEQ);
        }
        catch (Exception ex) {
            MainActivity.entryLogger("onboardEQ Exception.", true);
            ex.printStackTrace();
        }
    }
}
