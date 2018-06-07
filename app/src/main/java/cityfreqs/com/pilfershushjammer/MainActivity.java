package cityfreqs.com.pilfershushjammer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    //private static final String TAG = "PilferShush_Jammer";
    public static final String VERSION = "2.0.08";
    // note:: API 23+ AudioRecord READ_BLOCKING const
    // note:: MediaRecorder.AudioSource.VOICE_COMMUNICATION == VoIP

    private static final int REQUEST_AUDIO_PERMISSION = 1;
    private static final int NOTIFY_PASSIVE_ID = 112;
    private static final int NOTIFY_ACTIVE_ID = 113;

    private static TextView debugText;
    private ToggleButton passiveJammerButton;

    private boolean activeTypeValue;
    private String[] jammerTypes;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private HeadsetIntentReceiver headsetReceiver;

    private PassiveJammer passiveJammer;
    private ActiveJammer activeJammer;

    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor sharedPrefsEditor;
    private NotificationManager notifyManager;
    private Notification.Builder notifyPassiveBuilder;
    private Notification.Builder notifyActiveBuilder;
    private boolean PASSIVE_RUNNING;
    private boolean ACTIVE_RUNNING;
    private boolean IRQ_TELEPHONY;
    private boolean HAS_HEADSET;

    private AlertDialog.Builder dialogBuilder;
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
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
                    runPassive();
                }
                else {
                    stopPassive();
                }
            }
        });

        ToggleButton activeJammerButton = findViewById(R.id.run_active_button);
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

        Switch activeTypeSwitch = findViewById(R.id.active_type_switch);
        activeTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activeTypeValue = isChecked;
            }
        });

        Switch eqSwitch = findViewById(R.id.eq_switch);
        eqSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                toggleEq(isChecked);
            }
        });

        HAS_HEADSET = false;
        headsetReceiver = new HeadsetIntentReceiver();

        // permissions ask:
        // check API version, above 23 permissions are asked at runtime
        // if API version < 23 (6.x) fallback is manifest.xml file permission declares
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {

            List<String> permissionsNeeded = new ArrayList<>();
            final List<String> permissionsList = new ArrayList<>();

            if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
                permissionsNeeded.add(getResources().getString(R.string.perms_state_1));

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    StringBuilder sb = new StringBuilder().append(getResources().getString(R.string.perms_state_2)).append(permissionsNeeded.get(0));
                    for (int i = 1; i < permissionsNeeded.size(); i++) {
                        sb.append(", ").append(permissionsNeeded.get(i));
                    }
                    String message = sb.toString();

                    showPermissionsDialog(message,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            permissionsList.toArray(new String[permissionsList.size()]),
                                            REQUEST_AUDIO_PERMISSION);
                                }
                            });
                    return;
                }
                ActivityCompat.requestPermissions(this,
                        permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_AUDIO_PERMISSION);
            }
            else {
                // assume already runonce, has permissions
                initApplication();
            }

        }
        else {
            // pre API 23
            initApplication();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        PASSIVE_RUNNING = sharedPrefs.getBoolean("passive_running", false);
        ACTIVE_RUNNING = sharedPrefs.getBoolean("active_running", false);
        IRQ_TELEPHONY = sharedPrefs.getBoolean("irq_telephony", false);

        IntentFilter headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, headsetFilter);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int status = audioFocusCheck();
        // do not resume active jammer from an IRQ
        if (IRQ_TELEPHONY && PASSIVE_RUNNING) {
            // return from background with state irq_telephony and passive_running
            // check audio focus status
            if (status == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // reset booleans to init state
                PASSIVE_RUNNING = false;
                IRQ_TELEPHONY = false;
                runPassive();
            }
            else if (status == AudioManager.AUDIOFOCUS_LOSS) {
                // possible music player etc that has speaker focus but no need of microphone,
                // can end up fighting for focus with music player,
                // reset booleans to init state
                PASSIVE_RUNNING = false;
                IRQ_TELEPHONY = false;
                runPassive();
            }
        }
        else if (PASSIVE_RUNNING) {
            // return from background without irq_telephony
            entryLogger(getResources().getString(R.string.app_status_1), true);
        }
        else {
            entryLogger(getResources().getString(R.string.app_status_2), true);
        }
        if (ACTIVE_RUNNING) {
            // return from background without irq_telephony
            entryLogger(getResources().getString(R.string.app_status_3), true);
        }
        else {
            entryLogger(getResources().getString(R.string.app_status_4), true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // backgrounded, possible audio_focus loss due to telephony...
        unregisterReceiver(headsetReceiver);

        // save state first
        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.putBoolean("irq_telephony", IRQ_TELEPHONY);
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
                .setPositiveButton(getResources().getString(R.string.dialog_button_okay), okListener)
                .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), null)
                .create()
                .show();
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            return (ActivityCompat.shouldShowRequestPermissionRationale(this, permission));
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }
                // Check for RECORD_AUDIO
                if (perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initApplication();
                }
                else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.perms_state_3), Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void introText() {
        // simple and understandable statements about app usage.
        entryLogger(getResources().getString(R.string.intro_1) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_2) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_3) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_4) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_5) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_6) + "\n", true);
        entryLogger(getResources().getString(R.string.intro_7) + "\n", false);
    }

    private void initApplication() {
        introText();

        headsetReceiver = new HeadsetIntentReceiver();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        initAudioFocusListener();

        AudioSettings audioSettings = new AudioSettings();
        AudioChecker audioChecker = new AudioChecker(this, audioSettings);
        entryLogger(getResources().getString(R.string.audio_check_pre_1), false);
        if (!audioChecker.determineRecordAudioType()) {
            // have a setup error getting the audio for record
            return;
        }
        entryLogger(getResources().getString(R.string.audio_check_pre_2), false);
        if (!audioChecker.determineOutputAudioType()) {
            // have a setup error getting the audio for output
            return;
        }

        // after audio inits, can call the jammers
        passiveJammer = new PassiveJammer(this, audioSettings);
        PASSIVE_RUNNING = false;
        IRQ_TELEPHONY = false;

        activeJammer = new ActiveJammer(this, audioSettings);
        ACTIVE_RUNNING = false;

        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.putBoolean("irq_telephony", IRQ_TELEPHONY);
        sharedPrefsEditor.apply();

        createNotifications();

        // inform current state of active jammer tone
        populateMenuItems();
        entryLogger("Active jammer set to: " + jammerTypes[activeJammer.getJammerTypeSwitch()], true);
    }

    private void runPassive() {
        if (passiveJammer != null && !PASSIVE_RUNNING) {
            if (passiveJammer.startPassiveJammer()) {
                if (!passiveJammer.runPassiveJammer()) {
                    // check for errors in running
                    passiveJammerButton.toggle();
                    stopPassive();
                }
                else {
                    entryLogger(getResources().getString(R.string.main_scanner_3), false);
                    PASSIVE_RUNNING = true;
                    notifyManager.notify(NOTIFY_PASSIVE_ID, notifyPassiveBuilder.build());
                }
            }
        }
    }
    private void stopPassive() {
        if (passiveJammer != null && PASSIVE_RUNNING) {
            passiveJammer.stopPassiveJammer();
            PASSIVE_RUNNING = false;
            entryLogger(getResources().getString(R.string.main_scanner_4), false);
            notifyManager.cancel(NOTIFY_PASSIVE_ID);
        }
    }

    private void runActive() {
        if (activeJammer != null && !ACTIVE_RUNNING) {
            // run it
            ACTIVE_RUNNING = true;
            notifyManager.notify(NOTIFY_ACTIVE_ID, notifyActiveBuilder.build());
            entryLogger(getResources().getString(R.string.main_scanner_5), false);
            activeJammer.play(activeTypeValue ? 1 : 0); // to change to proper int
        }
    }

    private void stopActive() {
        if (activeJammer != null && ACTIVE_RUNNING) {
            // stop it
            ACTIVE_RUNNING = false;
            notifyManager.cancel(NOTIFY_ACTIVE_ID);
            entryLogger(getResources().getString(R.string.main_scanner_6), false);
            activeJammer.stop();
        }
    }

    private void createNotifications() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent,0);

        notifyManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        notifyPassiveBuilder = new Notification.Builder(this);
        notifyActiveBuilder = new Notification.Builder(this);

        notifyPassiveBuilder.setSmallIcon(R.mipmap.ic_stat_logo_notify_jammer)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("passive jammer running")
                .setContentText("Tap to return to app")
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false);

        notifyActiveBuilder.setSmallIcon(R.mipmap.ic_stat_logo_notify_jammer)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle("active jammer running")
                .setContentText("Tap to return to app")
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false);
    }

    /*

        UTILITY FUNCTIONS

    */
    private void toggleEq(boolean eqOn) {
        if (activeJammer != null) {
            activeJammer.setEqOn(eqOn);
            // need to stop so eq change can take effect
            stopActive();
        }
        if (eqOn) entryLogger(getResources().getString(R.string.app_status_6), false);
        else entryLogger(getResources().getString(R.string.app_status_5), false);

    }

    private void interruptRequestAudio(int focusChange) {
        // possible system app request audio focus
        entryLogger(getResources().getString(R.string.audiofocus_check_5), true);
        // may want to handle focus change reason differently in future
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // total loss, focus abandoned
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // system forced loss, assuming telephony
            IRQ_TELEPHONY = true;
        }
    }

    private int audioFocusCheck() {
        entryLogger(getResources().getString(R.string.audiofocus_check_1), false);
        int result = audioManager.requestAudioFocus(audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            entryLogger(getResources().getString(R.string.audiofocus_check_2), false);
        }
        else if (result == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
            entryLogger(getResources().getString(R.string.audiofocus_check_3), false);
        }
        else {
            entryLogger(getResources().getString(R.string.audiofocus_check_4), false);
        }
        return result;
    }

    private void toggleHeadset() {
        // do even need this?
        if (HAS_HEADSET) {
            // volume to 50%
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                    AudioManager.FLAG_SHOW_UI);
            entryLogger(getResources().getString(R.string.headset_state_4), true);
        }
        else {
            // volume to 100%
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI);
        }
    }

    // currently, audioFocus listener is the only method of auto-triggering the jammer behaviour
    private void initAudioFocusListener() {
        audioFocusListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch(focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        // -1
                        // loss for unknown duration
                        entryLogger(getResources().getString(R.string.audiofocus_1), false);
                        audioManager.abandonAudioFocus(audioFocusListener);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        // -2
                        // temporary loss ? API docs says a "transient loss"!
                        entryLogger(getResources().getString(R.string.audiofocus_2), false);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        // -3
                        // loss to other audio source, this can duck for the short duration if it wants
                        // can be system notification or ringtone sounds
                        entryLogger(getResources().getString(R.string.audiofocus_3), false);
                        interruptRequestAudio(focusChange);
                        break;
                    case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                        // 0
                        // failed focus change request
                        entryLogger(getResources().getString(R.string.audiofocus_4), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        //case AudioManager.AUDIOFOCUS_REQUEST_GRANTED: <- duplicate int value...
                        // 1
                        // has gain, or request for gain, of unknown duration
                        entryLogger(getResources().getString(R.string.audiofocus_5), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        // 2
                        // temporary gain or request for gain, for short duration (ie. notification)
                        entryLogger(getResources().getString(R.string.audiofocus_6), false);
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                        // 3
                        // as above but with other background audio ducked for duration
                        entryLogger(getResources().getString(R.string.audiofocus_7), false);
                        break;
                    default:
                        //
                        entryLogger(getResources().getString(R.string.audiofocus_8), false);
                }
            }
        };
    }

    private class HeadsetIntentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        entryLogger(getResources().getString(R.string.headset_state_1), false);
                        HAS_HEADSET = false;
                        toggleHeadset();
                        break;
                    case 1:
                        entryLogger(getResources().getString(R.string.headset_state_2), false);
                        HAS_HEADSET = true;
                        toggleHeadset();
                        break;
                    default:
                        entryLogger(getResources().getString(R.string.headset_state_3), false);
                        HAS_HEADSET = false;
                }
            }
        }
    }

    private void aboutDialog() {
        String aboutString = (getResources().getString(R.string.about_version) + VERSION  + "\n\n")
            + (getResources().getString(R.string.about_dialog_2) + "\n\n")
            + (getResources().getString(R.string.about_dialog_3) + "\n\n")
            + (getResources().getString(R.string.about_dialog_4));

        dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle(R.string.about_dialog_1);
        dialogBuilder.setMessage(aboutString);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                alertDialog.cancel();
            }
        });

        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void populateMenuItems() {
        jammerTypes = new String[4];
        jammerTypes[0] = getResources().getString(R.string.jammer_dialog_2);
        jammerTypes[1] = getResources().getString(R.string.jammer_dialog_3);
        jammerTypes[2] = getResources().getString(R.string.jammer_dialog_4);
        jammerTypes[3] = getResources().getString(R.string.jammer_dialog_5);
    }

    private void jammerDialog() {
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setItems(jammerTypes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                // types listing as above
                // other user input needed for the below options
                switch(which) {
                    case 0:
                        activeJammer.setJammerTypeSwitch(AudioSettings.JAMMER_TYPE_TEST);
                        entryLogger("Jammer type changed to " + jammerTypes[which], false);
                        break;
                    case 1:
                        activeJammer.setJammerTypeSwitch(AudioSettings.JAMMER_TYPE_NUHF);
                        entryLogger("Jammer type changed to " + jammerTypes[which], false);
                        break;
                    case 2:
                        defaultRangedDialog();
                        break;
                    case 3:
                        userRangedDialog();
                        break;
                    default:
                        break;

                }
            }
        });
        dialogBuilder.setTitle(R.string.jammer_dialog_1);
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }
    private void defaultRangedDialog() {
        // open dialog with field for carrierfrequency
        dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.default_ranged_form, null);
        dialogBuilder.setView(inputView);
        final EditText userCarrierInput = (EditText) inputView.findViewById(R.id.carrier_input);

        dialogBuilder.setTitle(R.string.jammer_dialog_4);

        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int userInputCarrier = Integer.parseInt(userCarrierInput.getText().toString());
                        activeJammer.setUserCarrier(userInputCarrier);
                        activeJammer.setJammerTypeSwitch(AudioSettings.JAMMER_TYPE_DEFAULT_RANGED);
                        entryLogger("Jammer type changed to 1000Hz drift with carrier at " + userInputCarrier, false);
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
        alertDialog.show();
    }
    private void userRangedDialog() {
        // open dialog with 2 fields - carrier and limit
        dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.user_ranged_form, null);
        dialogBuilder.setView(inputView);

        final EditText userCarrierInput = (EditText) inputView.findViewById(R.id.carrier_input);
        final EditText userLimitInput = (EditText) inputView.findViewById(R.id.limit_input);

        dialogBuilder.setTitle(R.string.jammer_dialog_5);

        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int userInputCarrier = Integer.parseInt(userCarrierInput.getText().toString());
                        int userInputLimit = Integer.parseInt(userLimitInput.getText().toString());

                        activeJammer.setUserCarrier(userInputCarrier);
                        activeJammer.setUserLimit(userInputLimit);
                        activeJammer.setJammerTypeSwitch(AudioSettings.JAMMER_TYPE_USER_RANGED);
                        entryLogger("Jammer type changed to " + userInputLimit + " Hz drift with carrier at " + userInputCarrier, false);

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
        alertDialog.show();
    }

    private void speedDriftDialog() {
        dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View inputView = inflater.inflate(R.layout.drift_speed_form, null);
        dialogBuilder.setView(inputView);

        final EditText userDriftInput = (EditText) inputView.findViewById(R.id.drift_input);

        dialogBuilder.setTitle(R.string.drift_dialog_1);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        int userInputDrift = Integer.parseInt(userDriftInput.getText().toString());
                        activeJammer.setDriftSpeed(userInputDrift);
                        entryLogger("Jammer drift speed changed to " + userInputDrift, false);
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
        alertDialog.show();
    }


    /*


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
