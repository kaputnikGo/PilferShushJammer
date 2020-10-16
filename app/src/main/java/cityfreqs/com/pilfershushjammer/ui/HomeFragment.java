package cityfreqs.com.pilfershushjammer.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.jammers.ActiveJammerService;
import cityfreqs.com.pilfershushjammer.jammers.PassiveJammerService;
import cityfreqs.com.pilfershushjammer.utilities.AudioChecker;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.AUDIO_SERVICE;
import static cityfreqs.com.pilfershushjammer.jammers.PassiveJammerService.ACTION_STOP_PASSIVE;

public class HomeFragment extends Fragment {
    private static final String TAG = "PilferShush_Jammer-HOME";

    // separate determineAudio() requests to originator specific
    private static final int INIT_REQ = 0;
    private static final int PASS_REQ = 1;
    private static final int ACTV_REQ = 2;

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedPrefsEditor;

    private boolean PASSIVE_RUNNING;
    private boolean ACTIVE_RUNNING;
    private boolean DEBUG;

    private TextView homeText;

    private Context context;
    private Bundle audioBundle;
    private AudioChecker audioChecker;

    private ToggleButton passiveJammerButton;
    private ToggleButton activeJammerButton;

    public HomeFragment() {
        // no-args constructor
    }

    static HomeFragment newInstance(Bundle audioBundle) {
        HomeFragment homeFragment = new HomeFragment();

        Bundle args = new Bundle();
        args.putBundle("audioBundle", audioBundle);
        homeFragment.setArguments(args);
        return homeFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (getArguments() != null) {
            audioBundle = getArguments().getBundle("audioBundle");
            if (audioBundle != null) {
                // usually set via dialog in main via pagerAdapter, first instance of audioBundle here
                DEBUG = true;
                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], DEBUG);

                // un-rem for production
                //DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
            }
        }
        else {
            // catch for no args bundle, will need to create new audioBundle with perms var...
            debugLogger("Failed to load audio settings.", true);
        }

        assert audioBundle != null;
        audioChecker = new AudioChecker(context, audioBundle);

        debugLogger("ON-ATTACH", false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        homeText = view.findViewById(R.id.home_text);
        passiveJammerButton = view.findViewById(R.id.run_passive_button);
        activeJammerButton = view.findViewById(R.id.run_active_button);

        populateElements();
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // called when fragment's activity method has returned
        debugLogger("OnActivityCreated called.", false);
        initApplication();
    }

    // deprecated method
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (getView() != null) {
            DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
            debugLogger("Home view visible", false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            getActivity().registerReceiver(passiveReceiver,
                    new IntentFilter("passive_running"));
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        PASSIVE_RUNNING = sharedPrefs.getBoolean("passive_running", false);
        ACTIVE_RUNNING = sharedPrefs.getBoolean("active_running", false);
        //DEBUG = sharedPrefs.getBoolean("debug", false);

        // override check for return from system destroy
        if (checkServiceRunning(PassiveJammerService.class)) {
            // jammer is running
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_1), false);
        }
        else {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_2), false);
            PASSIVE_RUNNING = false;
        }
        if (checkServiceRunning(ActiveJammerService.class)) {
            // jammer is running
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_3), false);
        }
        else {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_4), false);
            ACTIVE_RUNNING = false;
        }

        int status = audioFocusCheck();

        if (PASSIVE_RUNNING) {
            // return from background
            if (status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_5), false);
            }
            else if (status == AudioManager.AUDIOFOCUS_LOSS) {
                // possible music player etc that has speaker focus but no need of microphone,
                if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_6), false);
            }
            entryLogger(getResources().getString(R.string.app_status_1), true);
            // check button state on
            if (!passiveJammerButton.isChecked()) {
                passiveJammerButton.toggle();
            }
        }
        else {
            entryLogger(getResources().getString(R.string.app_status_2), false);
            // check button state off
            if (passiveJammerButton.isChecked()) {
                passiveJammerButton.toggle();
            }
        }
        if (ACTIVE_RUNNING) {
            // return from background without irq_telephony
            entryLogger(getResources().getString(R.string.app_status_3), true);
            // check button state on
            if (!activeJammerButton.isChecked()) {
                activeJammerButton.toggle();
            }
        }
        else {
            entryLogger(getResources().getString(R.string.app_status_4), false);
            // check button state off
            if (activeJammerButton.isChecked()) {
                activeJammerButton.toggle();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // save state first
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.apply();

        if (getActivity() != null) {
            getActivity().unregisterReceiver(passiveReceiver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        debugLogger("ONSTOP CALLED.", false);
        // save state
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**********************************************************************************************/

    private void populateElements(){
        homeText.setTextColor(Color.parseColor("#00ff00"));
        homeText.setMovementMethod(new ScrollingMovementMethod());
        homeText.setSoundEffectsEnabled(false); // no further click sounds
        homeText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                // nothing
            }
        });

        passiveJammerButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (DEBUG) entryLogger("PASSIVE IS CHECKED", false);
                    runPassive();
                }
                else {
                    stopPassive();
                }
            }
        });

        activeJammerButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    runActive();
                }
                else {
                    stopActive();
                }
            }
        });
        displayIntroText();
    }

    private void displayIntroText() {
        // simple and understandable statements about app usage.
        entryLogger(getResources().getString(R.string.intro_1) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_2) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_3) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_4) + "\n", true);
    }

    /**********************************************************************************************/

    private final Handler serviceHandler = new Handler();

    private void runPassive() {
        if (PASSIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_7), false);
        }
        else {
            // check audio type
            if (!checkAudio(PASS_REQ)) {
                //error in audio type getting
                debugLogger("Tried to start Passive Jammer without microphone permissions.", true);
            }
            Intent startIntent = new Intent(getActivity(), PassiveJammerService.class);
            startIntent.setAction(PassiveJammerService.ACTION_START_PASSIVE);
            startIntent.putExtras(audioBundle);
            //API 26+ requires startForegroundService
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            }
            else {
                context.startService(startIntent);
            }
            // 2 sec check for passive running (5 sec till timeout)
            serviceHandler.postDelayed(new Runnable() {
                public void run() {
                    checkPassiveRunning();
                }
            }, 2000);
        }
    }

    private void checkPassiveRunning() {
        if (PASSIVE_RUNNING) {
            entryLogger(context.getResources().getString(R.string.main_scanner_3), true);
        }
        else {
            entryLogger("Passive jammer failed to start.", true);
            // account for timeout
            // check button state is set to off
            if (passiveJammerButton.isChecked()) {
                passiveJammerButton.toggle();
            }
        }
    }

    private void stopPassive() {
        // npe here at getActivity, use context instead?
        if (isAdded()) {
            Intent stopIntent = new Intent(getActivity(), PassiveJammerService.class);
            stopIntent.setAction(ACTION_STOP_PASSIVE);
            context.startService(stopIntent);
            PASSIVE_RUNNING = false;
            entryLogger(getResources().getString(R.string.main_scanner_4), true);
        }
    }

    private void runActive() {
        if (ACTIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_8), false);
        }
        else {
            // check audio type
            if (!checkAudio(ACTV_REQ)) {
                //error in audio type getting
                debugLogger("Failed to start Active Jammer at checkAudio().", true);
            }
            Intent startIntent = new Intent(getActivity(), ActiveJammerService.class);
            startIntent.setAction(ActiveJammerService.ACTION_START_ACTIVE);
            startIntent.putExtras(audioBundle);
            // API 26+
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent);
            }
            else {
                context.startService(startIntent);
            }
            ACTIVE_RUNNING = true;
            entryLogger(getResources().getString(R.string.main_scanner_5), true);
        }
    }

    private void stopActive() {
        Intent stopIntent = new Intent(getActivity(), ActiveJammerService.class);
        stopIntent.setAction(ActiveJammerService.ACTION_STOP_ACTIVE);
        context.startService(stopIntent);
        ACTIVE_RUNNING = false;
        entryLogger(getResources().getString(R.string.main_scanner_6), true);
    }

    /**********************************************************************************************/

    private void initApplication() {
        debugLogger("INIT_APPLICATION", false);
        // apply audio checker settings to bundle for services
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[7], AudioSettings.JAMMER_TONE);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_TEST);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[9], AudioSettings.CARRIER_TEST_FREQUENCY);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[10], AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[11], AudioSettings.MINIMUM_DRIFT_LIMIT);
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14], false);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[18], AudioSettings.WAVEFORM_SIN);
        audioBundle.putString(AudioSettings.AUDIO_BUNDLE_KEYS[19], "eqPreset not set");
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[0], AudioSettings.MIC_SOURCE_DEFAULT);

        checkAudio(INIT_REQ);

        // test for Android 10 concurrent audio blocking for source VOICE_COMMS
        audioChecker.setCapturePolicy();
        entryLogger(audioChecker.checkAudioManagerSupport(), false);

        entryLogger("\n" + getResources().getString(R.string.intro_8) +
                AudioSettings.JAMMER_TYPES[AudioSettings.JAMMER_TYPE_TEST]
                + "\n", false);

        debugLogger("Debug mode set: " + audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15]), true);

        // crashes in < 11 maybe cos of audio perms and a mic info check
        // home fragment shows underneath dialogs, so would need a loading activity/screen first
        String audioCheckerReturn;
        //check for permissions here, as some are crashing
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(context)
                    .setMessage("This app needs RECORD_AUDIO permission to function.")
                    .setCancelable(false)
                    .setPositiveButton(context.getResources().getString(R.string.dialog_button_okay), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.d(TAG, context.getResources().getString(R.string.perms_state_4));
                            // this just stops mic info check for this app run
                        }
                    })
                    .create()
                    .show();
            audioCheckerReturn = "Audio Record permissions needed.";
        }
        else {
            try {
                audioCheckerReturn = audioChecker.determineMediaRecorderType();
            }
            catch (IllegalStateException ex) {
                entryLogger("attempt to determine audio record capability caused an exception.", true);
                audioCheckerReturn = null;
            }
        }

        if (audioCheckerReturn != null) {
            entryLogger(audioCheckerReturn + "\n", true);
        }
        else {
            entryLogger("Unable to determine Media Recorder type info.\n", true);
        }
    }

    private boolean checkAudio(int requester) {
        if (!audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[16])) {
            // permissions not granted, possible UI delay
            debugLogger("INIT audiocheck has perms FALSE.", true);
            return false;
        }
        else {
            if (!determineAudio(requester)) {
                //
                debugLogger("INIT determineAudio failed.", true);
                return false;
            }
            return true;
        }
    }

    private boolean determineAudio(int requester) {
        // due to conflicts with AudioFocus, init calls only check for audio record.
        // if the active jammer is engaged it will check for output as per usual.
        // update:  have added output check call to end of a true recordAudio check
        if (requester == PASS_REQ || requester == INIT_REQ) {
            entryLogger(getResources().getString(R.string.audio_check_pre_1), false);
            if (!audioChecker.determineRecordAudioType()) {
                // have a setup error getting the audio for record
                entryLogger(getResources().getString(R.string.audio_check_1), true);
                return false;
            }
        }
        if (requester == ACTV_REQ) {
            entryLogger(getResources().getString(R.string.audio_check_pre_2), false);
            if (!audioChecker.determineOutputAudioType()) {
                // have a setup error getting the audio for output
                entryLogger(getResources().getString(R.string.audio_check_2), true);
                return false;
            }
        }
        return true;
    }

    private int audioFocusCheck() {
        if (DEBUG) entryLogger("AudioFocus check", false);
        // note: api 26+ use AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        // NOTE: no resource context available in onAudioFocusChange
        // temp init of mediaPlayer simply for a systems check
        final AudioManager.OnAudioFocusChangeListener audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch(focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // -1
                        // loss for unknown duration
                        debugLogger("Audio Focus Listener: LOSS.", true);
                        //audioManager.abandonAudioFocus(audioFocusListener);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // -2
                        // temporary loss ? API docs says a "transient loss"!
                        debugLogger("Audio Focus Listener: LOSS_TRANSIENT.", false);
                        // system forced loss, assuming telephony
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // -3
                        // loss to other audio source, this can duck for the short duration if it wants
                        // can be system notification or ringtone sounds
                        debugLogger("Audio Focus Listener: LOSS_TRANSIENT_DUCK.", false);
                        break;
                    case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                        // 0
                        // failed focus change request
                        debugLogger("Audio Focus Listener: REQUEST_FAIL.", true);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        //case AudioManager.AUDIOFOCUS_REQUEST_GRANTED: <- duplicate int value...
                        // 1
                        // has gain, or request for gain, of unknown duration
                        debugLogger("Audio Focus Listener: GAIN.", false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        // 2
                        // temporary gain or request for gain, for short duration (ie. notification)
                        debugLogger("Audio Focus Listener: GAIN_TRANSIENT.", false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        // 3
                        // as above but with other background audio ducked for duration
                        debugLogger("Audio Focus Listener: GAIN_TRANSIENT_DUCK.", false);
                        break;
                    default:
                        //
                        debugLogger("Audio Focus Listener: UNKNOWN STATE.", false);
                }
            }
        };

        // if/else for += API 26 (Oreo, 8.0) deprecation stream_types for focus
        int result;
        AudioManager audioManager = (AudioManager)context.getSystemService(AUDIO_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(false)
                    .setOnAudioFocusChangeListener(audioFocusListener)
                    .build();
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(playbackAttributes);
            assert audioManager != null;
            result = audioManager.requestAudioFocus(focusRequest);
            // get rid of it
            mediaPlayer.release();
        }
        else {
            // method below is deprecated in API 26
            assert audioManager != null;
            result = audioManager.requestAudioFocus(audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
        }
        //final Object focusLock = new Object();
        //boolean playbackDelayed = false;
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (DEBUG) entryLogger(context.getResources().getString(R.string.audiofocus_check_2), false);
        }
        else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            if (DEBUG) entryLogger(context.getResources().getString(R.string.audiofocus_check_3), false);
        }
        else {
            if (DEBUG) entryLogger(context.getResources().getString(R.string.audiofocus_check_4), false);
        }
        return result;
    }

    // As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
    // For backwards compatibility, it will still return the caller's own services.
    private boolean checkServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private final BroadcastReceiver passiveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            assert message != null;
            PASSIVE_RUNNING = (message.equals("true"));
            debugLogger("Receive message Jammer Service running: " + message, false);
        }
    };

    /**********************************************************************************************/

    private void entryLogger(String entry, boolean caution) {
        int start = homeText.getText().length();
        homeText.append("\n" + entry);
        int end = homeText.getText().length();
        Spannable spannableText = (Spannable) homeText.getText();
        if (caution) {
            spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
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
