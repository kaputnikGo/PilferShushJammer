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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = "PilferShush_Jammer";
    public static final String VERSION = "2.0.03"; // active jammer version

    // note:: API 23+ AudioRecord READ_BLOCKING const

    //TODO add headset toggleButton
    //TODO ugly notification 0xffffff icon
    //TODO make clear in UI audioFocus is to do with output

    //TODO determine whether to rely only on audioFocus as auto-trigger for jammer

    private static final int REQUEST_AUDIO_PERMISSION = 1;
    private static final int NOTIFY_PASSIVE_ID = 112;
    private static final int NOTIFY_ACTIVE_ID = 113;

    private static TextView debugText;
    private ToggleButton passiveJammerButton;
    private ToggleButton activeJammerButton;
    private Switch activeTypeSwitch;
    private Switch testRangeSwitch;
    private int activeTypeValue;
    private boolean testRangeValue;

    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener audioFocusListener;
    private HeadsetIntentReceiver headsetReceiver;
    private IntentFilter headsetFilter;

    private AudioSettings audioSettings;
    private AudioChecker audioChecker;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
        debugText = (TextView) findViewById(R.id.debug_text);
        debugText.setTextColor(Color.parseColor("#00ff00"));
        debugText.setMovementMethod(new ScrollingMovementMethod());
        debugText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugText.setSoundEffectsEnabled(false); // no further click sounds
            }
        });

        passiveJammerButton = (ToggleButton) findViewById(R.id.run_passive_button);
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

        activeJammerButton = (ToggleButton) findViewById(R.id.run_active_button);
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

        activeTypeSwitch = (Switch) findViewById(R.id.active_type_switch);
        activeTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // ON position, whitenoise = 1
                    activeTypeValue = AudioSettings.JAMMER_WHITE;
                    //TODO check behaviour if ACTIVE_RUNNING

                }
                else {
                    // OFF position, tone = 0
                    activeTypeValue = AudioSettings.JAMMER_TONE;
                }
            }
        });

        testRangeSwitch = (Switch) findViewById(R.id.test_range_switch);
        testRangeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                testRangeValue = isChecked;
                if (activeJammer != null) {
                    activeJammer.setTestRangeSwitch(testRangeValue);
                }
            }
        });

        headsetReceiver = new HeadsetIntentReceiver();

        // permissions ask:
        // check API version, above 23 permissions are asked at runtime
        // if API version < 23 (6.x) fallback is manifest.xml file permission declares
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {

            List<String> permissionsNeeded = new ArrayList<String>();
            final List<String> permissionsList = new ArrayList<String>();

            if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))
                permissionsNeeded.add(getResources().getString(R.string.perms_state_1));

            if (permissionsList.size() > 0) {
                if (permissionsNeeded.size() > 0) {
                    // Need Rationale
                    String message = getResources().getString(R.string.perms_state_2) + permissionsNeeded.get(0);
                    for (int i = 1; i < permissionsNeeded.size(); i++) {
                        message = message + ", " + permissionsNeeded.get(i);
                    }
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
                return;
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
        // work out if need to restart jamming
        PASSIVE_RUNNING = sharedPrefs.getBoolean("passive_running", false);
        ACTIVE_RUNNING = sharedPrefs.getBoolean("active_running", false);
        IRQ_TELEPHONY = sharedPrefs.getBoolean("irq_telephony", false);

        headsetFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetReceiver, headsetFilter);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        // refocus app
        //toggleHeadset();
        int status = audioFocusCheck();

        //TODO add ACTIVE_RUNNING
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
                // TODO may get an error from VOIP here.
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
        sharedPrefsEditor.commit();
        // then work out if need to toggle jammer off (UI) due to irq_telephony
        if (PASSIVE_RUNNING && IRQ_TELEPHONY) {
            // make UI conform to jammer override by system telephony
            stopPassive();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            aboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))
                return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION: {
                Map<String, Integer> perms = new HashMap<String, Integer>();
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

    private boolean initApplication() {
        introText();

        headsetReceiver = new HeadsetIntentReceiver();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        HAS_HEADSET = false;
        //toggleHeadset();

        initAudioFocusListener();

        audioSettings = new AudioSettings();
        audioChecker = new AudioChecker(this, audioSettings);
        entryLogger(getResources().getString(R.string.audio_check_pre_1), false);
        if (audioChecker.determineRecordAudioType() == false) {
            // have a setup error getting the audio for record
            return false;
        }
        entryLogger(getResources().getString(R.string.audio_check_pre_2), false);
        if (audioChecker.determineOutputAudioType() == false) {
            // have a setup error getting the audio for output
            return false;
        }

        // after audio inits, can call the jammers
        passiveJammer = new PassiveJammer(this, audioSettings);
        PASSIVE_RUNNING = false;
        IRQ_TELEPHONY = false;

        //TODO test with fixed carrierFrequency
        activeJammer = new ActiveJammer(this, audioSettings);
        activeJammer.setTestRangeSwitch(testRangeValue);
        activeJammer.setCarrierFrequency(audioSettings.CARRIER_TEST_FREQUENCY);
        ACTIVE_RUNNING = false;

        sharedPrefs = getPreferences(Context.MODE_PRIVATE);
        sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putBoolean("passive_running", PASSIVE_RUNNING);
        sharedPrefsEditor.putBoolean("active_running", ACTIVE_RUNNING);
        sharedPrefsEditor.putBoolean("irq_telephony", IRQ_TELEPHONY);
        sharedPrefsEditor.commit();

        createNotifications();

        return true;
    }

    private void runPassive() {
        if (passiveJammer != null && PASSIVE_RUNNING == false) {
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
        if (passiveJammer != null && PASSIVE_RUNNING == true) {
            passiveJammer.stopPassiveJammer();
            PASSIVE_RUNNING = false;
            entryLogger(getResources().getString(R.string.main_scanner_4), false);
            notifyManager.cancel(NOTIFY_PASSIVE_ID);
        }
    }

    private void runActive() {
        //TODO
        if (activeJammer != null && ACTIVE_RUNNING == false) {
            // run it
            ACTIVE_RUNNING = true;
            notifyManager.notify(NOTIFY_ACTIVE_ID, notifyActiveBuilder.build());
            entryLogger(getResources().getString(R.string.main_scanner_5), false);
            activeJammer.play(activeTypeValue);
            //toggleHeadset();
        }
    }

    private void stopActive() {
        //TODO
        if (activeJammer != null && ACTIVE_RUNNING == true) {
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
    private void interruptRequestAudio(int focusChange) {
        // possible system app request audio focus
        entryLogger(getResources().getString(R.string.audiofocus_check_5), true);
        // may want to handle focus change reason differently in future
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            // total loss, focus abandoned
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
        // TODO change based upon headset and jamming
        // do even need this?
        if (HAS_HEADSET && ACTIVE_RUNNING) {
            // volume to 100% for jamming
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    AudioManager.FLAG_SHOW_UI);
        }
        else if (HAS_HEADSET) {
            // volume to 50%
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) / 2,
                    AudioManager.FLAG_SHOW_UI);
        }
        else {
            // volume to 0
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    0,
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
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        entryLogger(getResources().getString(R.string.headset_state_1), false);
                        HAS_HEADSET = false;
                        //toggleHeadset();
                        break;
                    case 1:
                        entryLogger(getResources().getString(R.string.headset_state_2), false);
                        HAS_HEADSET = true;
                        //toggleHeadset();
                        break;
                    default:
                        entryLogger(getResources().getString(R.string.headset_state_3), false);
                        HAS_HEADSET = false;
                }
            }
        }
    }

    private void aboutDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append(getResources().getString(R.string.about_version) + VERSION  + "\n\n");
        sb.append(getResources().getString(R.string.about_dialog_2) + "\n\n");
        sb.append(getResources().getString(R.string.about_dialog_3) + "\n\n");
        sb.append(getResources().getString(R.string.about_dialog_4));

        dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setTitle(R.string.about_dialog_1);
        dialogBuilder.setMessage(sb.toString());
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
