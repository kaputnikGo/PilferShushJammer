package cityfreqs.com.pilfershushjammer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String VERSION = "3.1.2";
    // note:: API 23+ AudioRecord READ_BLOCKING const
    // https://developer.android.com/reference/android/app/admin/DevicePolicyManager
    // public void setCameraDisabled (ComponentName admin, boolean disabled)
    // Currently, MediaRecorder does not work on the emulator.
    private static final int REQUEST_AUDIO_PERMISSION = 1;

    private static TextView debugText;
    private ToggleButton passiveJammerButton;
    private ToggleButton activeJammerButton;

    private Bundle audioBundle;

    private String[] jammerTypes;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;

    private BackgroundChecker backgroundChecker;

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedPrefsEditor;
    private boolean PASSIVE_RUNNING;
    private boolean ACTIVE_RUNNING;
    private boolean IRQ_TELEPHONY;
    private boolean DEBUG;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        debugText = findViewById(R.id.debug_text);
        debugText.setTextColor(Color.parseColor("#00ff00"));
        debugText.setMovementMethod(new ScrollingMovementMethod());
        debugText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugText.setSoundEffectsEnabled(false); // no further click sounds
            }
        });

        passiveJammerButton = findViewById(R.id.run_passive_button);
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

        activeJammerButton = findViewById(R.id.run_active_button);
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

        // permissions ask:
        // check API version, above 23 permissions are asked at runtime
        // if API version < 23 (6.x) fallback is manifest.xml file permission declares
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {

            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    String message = getResources().getString(R.string.perms_state_2_1) + "\n\n";
                    message += getResources().getString(R.string.perms_state_2_2) + "\n\n";
                    message += Manifest.permission.RECORD_AUDIO;

                    showPermissionsDialog(message, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.RECORD_AUDIO},
                                            REQUEST_AUDIO_PERMISSION);
                                }
                            });
                }
                else {
                    // no reasoning, show perms request
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_AUDIO_PERMISSION);
                }
            }
            else {
                // assume already runonce, has permissions
                initApplication();
            }
        }
        else {
            // pre API 23, check permissions anyway
            if (PermissionChecker.checkSelfPermission(MainActivity.this,
                    Manifest.permission.RECORD_AUDIO) == PermissionChecker.PERMISSION_GRANTED) {
                initApplication();
            }
            else {
                closeApp();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        PASSIVE_RUNNING = sharedPrefs.getBoolean("passive_running", false);
        ACTIVE_RUNNING = sharedPrefs.getBoolean("active_running", false);
        IRQ_TELEPHONY = sharedPrefs.getBoolean("irq_telephony", false);
        DEBUG = sharedPrefs.getBoolean("debug", false);

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

        //TODO check service behaviours when IRQ_TELEPHONY triggered
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int status = audioFocusCheck();

        if (IRQ_TELEPHONY && PASSIVE_RUNNING) {
            // return from background with state irq_telephony and passive_running
            // check audio focus status
            if (status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

                if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_5), false);
                // reset booleans to init state
                PASSIVE_RUNNING = false;
                IRQ_TELEPHONY = false;
                // cancel notification as part of reset
                //runPassive();
            }
            // we could be checking against ourselves...
            else if (status == AudioManager.AUDIOFOCUS_LOSS) {
                // possible music player etc that has speaker focus but no need of microphone,
                if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_6), false);
            }
        }
        else if (PASSIVE_RUNNING) {
            // return from background without irq_telephony
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
    protected void onPause() {
        super.onPause();
        // save state first
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.putBoolean("irq_telephony", IRQ_TELEPHONY);
        sharedPrefsEditor.putBoolean("debug", DEBUG);
        sharedPrefsEditor.apply();
        // check toggle jammer off (UI) due to irq_telephony
        if (PASSIVE_RUNNING && IRQ_TELEPHONY) {
            stopPassive();
        }
        if (ACTIVE_RUNNING && IRQ_TELEPHONY) {
            stopActive();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // save state
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.putBoolean("irq_telephony", IRQ_TELEPHONY);
        sharedPrefsEditor.putBoolean("debug", DEBUG);
        sharedPrefsEditor.apply();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_about:
                aboutDialog();
                return true;
            case R.id.action_jammer:
                jammerDialog();
                return true;
            case R.id.action_drift_speed:
                speedDriftDialog();
                return true;
            case R.id.action_userapp_check:
                userappCheckDialog();
                return true;
            case R.id.action_userapp_summary:
                userappSummary();
                return true;
            case R.id.action_nuhfsdk_list:
                displayBeaconSdkList();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*

        MAIN APPLICATION FUNCTIONS

    */
    private void showPermissionsDialog(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.dialog_button_continue), okListener)
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION: {
                // Check for RECORD_AUDIO
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initApplication();
                }
                else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.perms_state_3), Toast.LENGTH_LONG)
                            .show();
                    closeApp();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void closeApp() {
        Log.d("PS_JAMMER", "closing app due to no RECORD_AUDIO permission granted.");
        finishAffinity();
    }

    private void introText() {
        // simple and understandable statements about app usage.
        entryLogger(getResources().getString(R.string.intro_1) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_2) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_3) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_4) + "\n", true);
        entryLogger(getResources().getString(R.string.intro_5) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_6) + "\n", false);
    }

    private void initApplication() {
        introText();

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        initAudioFocusListener();

        // apply audio checker settings to bundle for services
        AudioChecker audioChecker = new AudioChecker(MainActivity.this);
        audioBundle = audioChecker.getAudioBundle();
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[7], false);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_TEST);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[9], AudioSettings.CARRIER_TEST_FREQUENCY);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[10], AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT);
        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[11], AudioSettings.MINIMUM_DRIFT_LIMIT);
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14], false);
        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], DEBUG);

        entryLogger(getResources().getString(R.string.audio_check_pre_1), false);
        if (!audioChecker.determineRecordAudioType()) {
            // have a setup error getting the audio for record
            entryLogger(getResources().getString(R.string.audio_check_1), true);
            return;
        }
        entryLogger(getResources().getString(R.string.audio_check_pre_2), false);
        if (!audioChecker.determineOutputAudioType()) {
            // have a setup error getting the audio for output
            entryLogger(getResources().getString(R.string.audio_check_2), true);
            return;
        }
        // background checker
        backgroundChecker = new BackgroundChecker(MainActivity.this);

        populateMenuItems();
        entryLogger("\n" + getResources().getString(R.string.intro_8) +
                jammerTypes[AudioSettings.JAMMER_TYPE_TEST], false);

        entryLogger("\n" + getResources().getString(R.string.intro_7) + "\n", true);

        initBackgroundChecker();
    }

    private void initBackgroundChecker() {
        if (backgroundChecker != null) {
            if (runBackgroundChecks()) {
                // report
                int audioNum = backgroundChecker.getUserRecordNumApps();
                if (audioNum > 0) {
                    entryLogger(getResources().getString(R.string.userapp_scan_8) + audioNum, true);
                } else {
                    entryLogger(getResources().getString(R.string.userapp_scan_9), false);
                }
                if (backgroundChecker.hasAudioBeaconApps()) {
                    entryLogger(backgroundChecker.getAudioBeaconAppNames().length
                            + getResources().getString(R.string.userapp_scan_10) + "\n", true);
                } else {
                    entryLogger(getResources().getString(R.string.userapp_scan_11) + "\n", false);
                }
            }
        }
        else {
            if (DEBUG) Log.d("PS_JAMMER", "backgroundChecker is NULL at init.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void resetApplication() {
        // from a background state IRQ LOSS,
        if (PASSIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_6), false);
        }
        if (ACTIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_6), false);
        }
    }

    // As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
    // For backwards compatibility, it will still return the caller's own services.
    private boolean checkServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void runPassive() {
        if (PASSIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_7), false);
        }
        else {
            Intent startIntent = new Intent(MainActivity.this, PassiveJammerService.class);
            startIntent.setAction(PassiveJammerService.ACTION_START_PASSIVE);
            startIntent.putExtras(audioBundle);
            startService(startIntent);
            PASSIVE_RUNNING = true;
            entryLogger(getResources().getString(R.string.main_scanner_3), true);
        }
    }

    private void stopPassive() {
        Intent stopIntent = new Intent(MainActivity.this, PassiveJammerService.class);
        stopIntent.setAction(PassiveJammerService.ACTION_STOP_PASSIVE);
        startService(stopIntent);
        PASSIVE_RUNNING = false;
        entryLogger(getResources().getString(R.string.main_scanner_4), true);
    }

    private void runActive() {
        if (ACTIVE_RUNNING) {
            if (DEBUG) entryLogger(getResources().getString(R.string.resume_status_8), false);
        }
        else {
            Intent startIntent = new Intent(MainActivity.this, ActiveJammerService.class);
            startIntent.setAction(ActiveJammerService.ACTION_START_ACTIVE);
            startIntent.putExtras(audioBundle);
            startService(startIntent);
            ACTIVE_RUNNING = true;
            entryLogger(getResources().getString(R.string.main_scanner_5), true);
        }
    }

    private void stopActive() {
        Intent stopIntent = new Intent(MainActivity.this, ActiveJammerService.class);
        stopIntent.setAction(ActiveJammerService.ACTION_STOP_ACTIVE);
        startService(stopIntent);
        ACTIVE_RUNNING = false;
        entryLogger(getResources().getString(R.string.main_scanner_6), true);
    }

    /*

        BACKGROUND CHECKER FUNCTIONS

    */
    protected boolean runBackgroundChecks() {
        // already checked for null at initBackground
        if (backgroundChecker.initChecker(MainActivity.this.getPackageManager())) {
            backgroundChecker.runChecker();
            backgroundChecker.checkAudioBeaconApps();
            return true;
        }
        else {
            entryLogger(getResources().getString(R.string.userapp_scan_1), true);
            return false;
        }
    }

    private void userappCheckDialog() {
        if (backgroundChecker != null) {
            String[] appNames = backgroundChecker.getOverrideScanAppNames();

            if (appNames != null && appNames.length > 0) {
                dialogBuilder = new AlertDialog.Builder(MainActivity.this);
                dialogBuilder.setItems(appNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // index position of clicked app name
                        listAppOverrideScanDetails(which);
                    }
                });
                dialogBuilder.setTitle(R.string.dialog_userapps);
                alertDialog = dialogBuilder.create();
                if (!isFinishing())
                    alertDialog.show();
            } else {
                entryLogger(getResources().getString(R.string.userapp_scan_4), true);
            }
        }
        else {
            if (DEBUG) Log.d("PS_JAMMER", "backgroundChecker is NULL at userApp check.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void userappSummary() {
        if (backgroundChecker != null) {
            entryLogger("\n--------------------------------------\n", false);
            backgroundChecker.audioAppEntryLog();
        }
        else {
            if (DEBUG) Log.d("PS_JAMMER", "backgroundChecker is NULL at userApp summary.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    protected void displayBeaconSdkList() {
        // current list (in /raw) of NUHF/ACR SDK package names
        if (backgroundChecker != null) {
            entryLogger("\n--------------------------------------\n", false);
            entryLogger(getResources().getString(R.string.sdk_names_list) + "\n"
                    + backgroundChecker.displayAudioSdkNames(), false);
        }
        else {
            if (DEBUG) Log.d("PS_JAMMER", "backgroundChecker is NULL.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void listAppOverrideScanDetails(int selectedIndex) {
        // already checked for null
        // check for receivers too?
        entryLogger("\n" + getResources().getString(R.string.userapp_scan_5)
                + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getActivityName()
                + ": " + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServicesNum(), true);

        if (backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServicesNum() > 0) {
            logAppEntryInfo(backgroundChecker.getOverrideScanAppEntry(selectedIndex).getServiceNames());
        }

        entryLogger(getResources().getString(R.string.userapp_scan_6)
                + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getActivityName()
                + ": " + backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiversNum(), true);

        if (backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiversNum() > 0) {
            logAppEntryInfo(backgroundChecker.getOverrideScanAppEntry(selectedIndex).getReceiverNames());
        }
    }

    private void logAppEntryInfo(String[] appEntryInfoList) {
        entryLogger("\n" + getResources().getString(R.string.userapp_scan_7) + "\n", false);
        for (String entryInfo : appEntryInfoList) {
            entryLogger(entryInfo + "\n", false);
        }
    }

    /*

        UTILITY FUNCTIONS

    */
    private void interruptRequestAudio(int focusChange) {
        // possible system app request audio focus
        entryLogger(getResources().getString(R.string.audiofocus_check_5), true);
        // may want to handle focus change reason differently in future
        // loss from back button press
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            resetApplication();
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // system forced loss, assuming telephony
            if (DEBUG) entryLogger("IRQ_TELEPHONY loss_transient", false);
            IRQ_TELEPHONY = true;
        }
    }

    private int audioFocusCheck() {
        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_1), false);
        // note: api 26+ use AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        int result = audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_2), false);
        }
        else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_3), false);
        }
        else {
            if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_check_4), false);
        }
        return result;
    }

    // currently, audioFocus listener is the only method of auto-triggering the jammer behaviour
    // this can get false positives with returning non-jamming app from back button press
    private void initAudioFocusListener() {
        audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch(focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // -1
                        // loss for unknown duration
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_1), false);
                        audioManager.abandonAudioFocus(audioFocusListener);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // -2
                        // temporary loss ? API docs says a "transient loss"!
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_2), false);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // -3
                        // loss to other audio source, this can duck for the short duration if it wants
                        // can be system notification or ringtone sounds
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_3), false);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                        // 0
                        // failed focus change request
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_4), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        //case AudioManager.AUDIOFOCUS_REQUEST_GRANTED: <- duplicate int value...
                        // 1
                        // has gain, or request for gain, of unknown duration
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_5), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        // 2
                        // temporary gain or request for gain, for short duration (ie. notification)
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_6), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        // 3
                        // as above but with other background audio ducked for duration
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_7), false);
                        break;
                    default:
                        //
                        if (DEBUG) entryLogger(getResources().getString(R.string.audiofocus_8), false);
                }
            }
        };
    }

    private void populateMenuItems() {
        jammerTypes = new String[5];
        jammerTypes[0] = getResources().getString(R.string.jammer_dialog_2);
        jammerTypes[1] = getResources().getString(R.string.jammer_dialog_3);
        jammerTypes[2] = getResources().getString(R.string.jammer_dialog_4);
        jammerTypes[3] = getResources().getString(R.string.jammer_dialog_5);
        jammerTypes[4] = getResources().getString(R.string.jammer_dialog_12);
    }

    private void aboutDialog() {
        String aboutString =
                (getResources().getString(R.string.about_dialog_2) + "\n\n")
            + (getResources().getString(R.string.about_dialog_3) + "\n\n")
            + (getResources().getString(R.string.about_dialog_4) + "\n\n")
            + (getResources().getString(R.string.about_dialog_5) + "\n\n")
            + (getResources().getString(R.string.about_dialog_6));

        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setTitle(getResources().getString(R.string.about_version) + VERSION);
        dialogBuilder.setMessage(aboutString);
        dialogBuilder.setCancelable(true);
        dialogBuilder
                .setPositiveButton(R.string.options_dialog_1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        bufferReadOptionDialog();
                    }
                })
                .setNegativeButton(R.string.dialog_button_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });


        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    /*
    private void debugOptionDialog() {
        // pop dialog with explanation and switch DEBUG
        //entryLogger(getResources().getString(R.string.options_dialog_5), true);

        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.debug_print_switcher, null);
        dialogBuilder.setView(inputView);

        dialogBuilder.setTitle(R.string.options_dialog_0);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.options_dialog_3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], true);
                        entryLogger(getResources().getString(R.string.app_status_15), false);
                    }
                })
                .setNegativeButton(R.string.options_dialog_4, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
                        entryLogger(getResources().getString(R.string.app_status_16), false);
                    }
                });
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();

    }
    */

    private void bufferReadOptionDialog() {
        // pop dialog with explanation and switch BUFFER_READ in passiveJammer
        // will need to restart service if running
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.buffer_read_switcher, null);
        dialogBuilder.setView(inputView);

        dialogBuilder.setTitle(R.string.options_dialog_1);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.options_dialog_3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14], true);
                        entryLogger(getResources().getString(R.string.app_status_13), false);
                    }
                })
                .setNegativeButton(R.string.options_dialog_4, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14], false);
                        entryLogger(getResources().getString(R.string.app_status_14), false);
                    }
                });
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void jammerDialog() {
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        dialogBuilder.setItems(jammerTypes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                // types listing as above
                // other user input needed for the below options
                switch(which) {
                    case 0:
                        audioBundle.putInt("jammerType", AudioSettings.JAMMER_TYPE_TEST);
                        entryLogger(getResources().getString(R.string.jammer_dialog_13)
                                + jammerTypes[which], false);
                        break;
                    case 1:
                        audioBundle.putInt("jammerType", AudioSettings.JAMMER_TYPE_NUHF);
                        entryLogger(getResources().getString(R.string.jammer_dialog_13)
                                + jammerTypes[which], false);
                        break;
                    case 2:
                        defaultRangedDialog();
                        break;
                    case 3:
                        userRangedDialog();
                        break;
                    case 4:
                        activeTypeDialog();
                        break;
                    default:
                        break;

                }
            }
        });
        dialogBuilder.setTitle(R.string.jammer_dialog_1);
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }
    private void defaultRangedDialog() {
        // open dialog with field for carrierfrequency
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.default_ranged_form, null);
        dialogBuilder.setView(inputView);
        final EditText userCarrierInput = inputView.findViewById(R.id.carrier_input);

        dialogBuilder.setTitle(R.string.jammer_dialog_4);

        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int userInputCarrier = AudioSettings.DEFAULT_NUHF_FREQUENCY;
                        String regexStr = "^[0-9]*$";

                        //Integer.MAX_VALUE(2147483647) and Integer.MIN_VALUE(-2147483648)
                        // simple check that string is length <= 5, max length of usable frequency
                        if (userCarrierInput.getText().length() != 0 && userCarrierInput.getText().length() <= 5) {
                            if(userCarrierInput.getText().toString().trim().matches(regexStr))

                                userInputCarrier = Integer.parseInt(userCarrierInput.getText().toString());
                        }
                        userInputCarrier = checkCarrierFrequency(userInputCarrier);

                        audioBundle.putInt("userCarrier", userInputCarrier);
                        audioBundle.putInt("jammerType", AudioSettings.JAMMER_TYPE_DEFAULT_RANGED);

                        entryLogger(getResources().getString(R.string.jammer_dialog_14)
                                + userInputCarrier, false);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });

        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }
    private void userRangedDialog() {
        // open dialog with 2 fields - carrier and limit
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.user_ranged_form, null);
        dialogBuilder.setView(inputView);

        final EditText userCarrierInput = inputView.findViewById(R.id.carrier_input);
        final EditText userLimitInput = inputView.findViewById(R.id.limit_input);

        dialogBuilder.setTitle(R.string.jammer_dialog_5);

        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int userInputCarrier = AudioSettings.DEFAULT_NUHF_FREQUENCY;
                        String regexStr = "^[0-9]*$";

                        // simple check that string is length < 5, max length of usable frequency
                        if (userCarrierInput.getText().length() != 0 && userCarrierInput.getText().length() <= 5) {
                            if(userCarrierInput.getText().toString().trim().matches(regexStr))
                                userInputCarrier = Integer.parseInt(userCarrierInput.getText().toString());
                        }
                        userInputCarrier = checkCarrierFrequency(userInputCarrier);

                        // simple check that string is length <= 4, max length of drift limit
                        int userInputLimit = AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT;
                        if (userLimitInput.getText().length() != 0 && userLimitInput.getText().length() <= 4) {
                            if(userLimitInput.getText().toString().trim().matches(regexStr))
                                userInputLimit = Integer.parseInt(userLimitInput.getText().toString());
                        }
                        userInputLimit = checkDriftLimit(userInputLimit);

                        audioBundle.putInt("userCarrier", userInputCarrier);
                        audioBundle.putInt("userLimit", userInputLimit);
                        audioBundle.putInt("jammerType", AudioSettings.JAMMER_TYPE_USER_RANGED);

                        entryLogger("Jammer type changed to " + userInputLimit
                                + " Hz drift with carrier at " + userInputCarrier, false);

                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });

        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void speedDriftDialog() {
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.drift_speed_form, null);
        dialogBuilder.setView(inputView);

        final EditText userDriftInput = inputView.findViewById(R.id.drift_input);

        dialogBuilder.setTitle(R.string.drift_dialog_1);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // set a default and check edit text field
                        int userInputSpeed = 1;
                        String regexStr = "^[0-9]*$";

                        // simple check that string is length <= 2, max length of drift speed
                        if (userDriftInput.getText().length() != 0  && userDriftInput.getText().length() <= 2) {
                            if(userDriftInput.getText().toString().trim().matches(regexStr))
                                userInputSpeed = Integer.parseInt(userDriftInput.getText().toString());
                        }
                        userInputSpeed = checkDriftSpeed(userInputSpeed);

                        audioBundle.putInt("userSpeed", userInputSpeed);
                        entryLogger("Jammer drift speed changed to " + userInputSpeed, false);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                        alertDialog.cancel();
                    }
                });
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    private void activeTypeDialog() {
        dialogBuilder = new AlertDialog.Builder(MainActivity.this);

        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.active_type_switcher, null);
        dialogBuilder.setView(inputView);

        dialogBuilder.setTitle(R.string.active_dialog_1);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.active_dialog_3, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[7], false);
                        entryLogger(getResources().getString(R.string.app_status_8), false);
                    }
                })
                .setNegativeButton(R.string.active_dialog_4, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[7], true);
                        entryLogger(getResources().getString(R.string.app_status_9), false);
                    }
                });
        alertDialog = dialogBuilder.create();
        if(!isFinishing())
            alertDialog.show();
    }

    /*

        CONFORM CHECKS FOR USER INPUT
    */
    private int checkCarrierFrequency(int carrierFrequency) {
        if (carrierFrequency > audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]))
            return audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);

        else if (carrierFrequency < AudioSettings.MINIMUM_NUHF_FREQUENCY)
            return AudioSettings.MINIMUM_NUHF_FREQUENCY;

        else
            return carrierFrequency;
    }

    private int checkDriftLimit(int driftLimit) {
        if (driftLimit > AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT)
            return AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT;

        else if (driftLimit < AudioSettings.MINIMUM_DRIFT_LIMIT)
            return AudioSettings.MINIMUM_DRIFT_LIMIT;

        else
            return driftLimit;
    }

    private int checkDriftSpeed(int driftSpeed) {
        // is 1 - 10, then * 1000
        if (driftSpeed < 1)
            return 1;
        else if (driftSpeed > 10)
            return 10;
        else
            return driftSpeed;
    }

    /*

        UI OUTPUT TO DEBUG WINDOW
    */
    protected static void entryLogger(String entry, boolean caution) {
        int start = debugText.getText().length();
        debugText.append("\n" + entry);
        int end = debugText.getText().length();
        Spannable spannableText = (Spannable) debugText.getText();
        if (caution) {
            spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.GREEN), start, end, 0);
        }
    }
}
