package cityfreqs.com.pilfershushjammer.ui;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.assist.AssistJammerService;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;


public class SettingsFragment extends Fragment {
    private static final String TAG = "PilferShush_Jammer-SETT";
    private boolean DEBUG;

    private Context context;
    private TextView settingsText;
    private Bundle audioBundle;
    private ViewGroup settingsContainer;

    public SettingsFragment() {
        // no-args constructor
    }

    static SettingsFragment newInstance(Bundle audioBundle) {
        SettingsFragment settingsFragment = new SettingsFragment();

        Bundle args = new Bundle();
        args.putBundle("audioBundle", audioBundle);
        settingsFragment.setArguments(args);

        return settingsFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (getArguments() != null) {
            audioBundle = getArguments().getBundle("audioBundle");
            if (audioBundle != null) {
                DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
            }
        }
        else {
            // catch for no args bundle.
            entryLogger("Failed to load audio settings.", true);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    // deprecated method
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (DEBUG) {
            if (getView() != null) {
                Log.d(TAG, "Settings view visible");
            }
            else {
                Log.d(TAG, "Settings view null");
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_settings, container, false);

        SwitchCompat activeTypeSwitch = view.findViewById(R.id.active_type_switch);
        activeTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //nuhf
                    audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_NUHF);
                    entryLogger(getResources().getString(R.string.jammer_dialog_13)
                            + AudioSettings.JAMMER_TYPES[AudioSettings.JAMMER_TYPE_NUHF], false);
                }
                else {
                    //test, default
                    audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_TEST);
                    entryLogger(getResources().getString(R.string.jammer_dialog_13)
                            + AudioSettings.JAMMER_TYPES[AudioSettings.JAMMER_TYPE_TEST], false);
                }
            }
        });

        SwitchCompat micSourceSwitch = view.findViewById(R.id.mic_source_switch);
        micSourceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //VoIP
                    audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[0], AudioSettings.MIC_SOURCE_VOICE_COMM);
                    entryLogger(getResources().getString(R.string.jammer_dialog_15)
                            + AudioSettings.GET_MIC_SOURCE(AudioSettings.MIC_SOURCE_VOICE_COMM), false);
                }
                else {
                    //default
                    audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[0], AudioSettings.MIC_SOURCE_DEFAULT);
                    entryLogger(getResources().getString(R.string.jammer_dialog_15)
                            + AudioSettings.GET_MIC_SOURCE(AudioSettings.MIC_SOURCE_DEFAULT), false);
                }
            }
        });

        TextView assistJammerSwitch = view.findViewById(R.id.assist_jammer_text);
        assistJammerSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                assistDialog();
            }
        });

        TextView driftInput = view.findViewById(R.id.drift_input_text);
        driftInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                driftSpeedDialog();
            }
        });

        TextView carrierInput = view.findViewById(R.id.carrier_input_text);
        carrierInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                carrierInputDialog();
            }
        });

        TextView limitInput = view.findViewById(R.id.limit_input_text);
        limitInput.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                limitInputDialog();
            }
        });

        RadioGroup activeRadioGroup = view.findViewById(R.id.active_radio_group);
        activeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_noise:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[7], AudioSettings.JAMMER_NOISE);
                        entryLogger(getResources().getString(R.string.app_status_9), false);
                        break;
                    case R.id.radio_tones:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[7], AudioSettings.JAMMER_TONE);
                        entryLogger(getResources().getString(R.string.app_status_8), false);
                        break;
                    case R.id.radio_shadow:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[7], AudioSettings.JAMMER_SHADOW);
                        entryLogger(getResources().getString(R.string.app_status_9_1), false);
                        break;
                }
            }
        });

        RadioGroup waveformRadioGroup = view.findViewById(R.id.waveform_radio_group);
        waveformRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio_sine:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[18], AudioSettings.WAVEFORM_SIN);
                        entryLogger(getResources().getString(R.string.app_status_15), false);
                        break;
                    case R.id.radio_square:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[18], AudioSettings.WAVEFORM_SQR);
                        entryLogger(getResources().getString(R.string.app_status_16), false);
                        break;
                    case R.id.radio_sawtooth:
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[18], AudioSettings.WAVEFORM_SAW);
                        entryLogger(getResources().getString(R.string.app_status_17), false);
                        break;
                }
            }
        });

        SwitchCompat bufferTypeSwitch = view.findViewById(R.id.buffer_type_switch);
        bufferTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // default to false
                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[14], isChecked);
                if (isChecked) {
                    // enabled
                    entryLogger(getResources().getString(R.string.app_status_13), true);
                }
                else {
                    //disabled, default
                    entryLogger(getResources().getString(R.string.app_status_14), true);
                }

            }
        });

        settingsText = view.findViewById(R.id.settings_text);
        settingsText.setTextColor(Color.parseColor("#00ff00"));
        settingsText.setMovementMethod(new ScrollingMovementMethod());
        settingsText.setSoundEffectsEnabled(false); // no further click sounds
        settingsText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                // nothing
            }
        });

        settingsContainer = container;
        return view;
    }

    /**********************************************************************************************/

    private void assistDialog() {
        //Voice Assistant jamming
        // seems to only require one time setup in Settings OS window,
        // then even if not selected as Default Assist app it still jams the omnibox?
        // this dialog is for caution/info about the assistant jammer
        // location: Settings/Apps and notifications/Default apps/Assist app
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(getResources().getString(R.string.assist_dialog_1));
        dialogBuilder.setMessage(getResources().getString(R.string.assist_dialog_2));
        dialogBuilder.setCancelable(false);
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_on, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //turn it on, pop the dialog with info text
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[20], true);
                        entryLogger(getResources().getString(R.string.assist_dialog_activated), false);
                        activateAssistantJammer(true);
                    }
                })
                .setNegativeButton(R.string.dialog_button_off, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //off, also pop dialog with info text
                        audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[20], false);
                        entryLogger(getResources().getString(R.string.assist_dialog_deactivated), false);
                        activateAssistantJammer(false);
                    }
                });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void activateAssistantJammer(boolean activate) {
        // this is clunky while in testing mode
        Log.d(TAG, "Assistant Jammer attempting to activate with: " + activate);
        String assistant = Settings.Secure.getString(context.getContentResolver(), "voice_interaction_service");

        // this catches the PS_Jammer being added to Settings/Default assist app page but not being selected,
        // or rather firstrun has user select it, then OS deselects it when dev reinstall...
        if (!VoiceInteractionService.isActiveService(context,
                new ComponentName(context, AssistJammerService.class))) {
            Log.w(TAG, "AssistJammer is NOT current voice activeService!");
            //trip settings
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
            // start service
            Intent intent = new Intent(context, AssistJammerService.class);
            intent.setAction(Intent.ACTION_ASSIST);
            context.startService(intent);
        }
        else {
            // allow to switch it off as well,
            // note: AOSP 5.1 No GApps Settings/Language&Input/Voice Input/ has no "none" option, so PSJam remains selected
            // unless there is another app to toggle between...
            Log.w(TAG, "AssistJammer is current voice activeService, option to switch it off.");
            //trip settings
            startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
            // start service
            Intent intent = new Intent(context, AssistJammerService.class);
            intent.setAction(Intent.ACTION_ASSIST);
            context.startService(intent);
        }

        if (assistant == null || assistant.isEmpty()) {
            Log.d(TAG, "Setup assistant for voice interaction resulted in a null/empty string.");
            // notes:
            // Settings.java : @TestApi public static final String VOICE_INTERACTION_SERVICE = "voice_interaction_service";
            // /*The currently selected voice interaction service flattened ComponentName.*/

            // @UnsupportedAppUsage public static final String ASSISTANT = "assistant";
            // /* The current assistant component. It could be a voice interaction service,
            //  * or an activity that handles ACTION_ASSIST, or empty which means using the default
            //  * handling.
            // set using android.app.role.RoleManager#ROLE_ASSISTANT assistant role

            // add some useful info to UI...
            Toast.makeText(context,
                    getResources().getString(R.string.assist_error_1),
                    Toast.LENGTH_SHORT)
                    .show();
        }
        else {
            ComponentName componentName = ComponentName.unflattenFromString(assistant);
            assert componentName != null;
            Log.d(TAG, "ComponentName: " + componentName.toString());

            if (activate) {
                if (componentName.getPackageName().equals(context.getPackageName())) {
                    // is already activated
                    Toast.makeText(context,
                            getResources().getString(R.string.assist_dialog_activated),
                            Toast.LENGTH_SHORT)
                            .show();
                }
                else {
                    // needs to add PilferShush Jammer in voice assist settings OS window to activate
                    Toast.makeText(context,
                            getResources().getString(R.string.assist_dialog_3),
                            Toast.LENGTH_SHORT)
                            .show();
                    startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
                }
            }
            else {
                if (componentName.getPackageName().equals(context.getPackageName())) {
                    // is already activated, so can deactivate via OS settings window
                    Toast.makeText(context,
                            getResources().getString(R.string.assist_dialog_4),
                            Toast.LENGTH_SHORT)
                            .show();
                    startActivity(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
                }
                else {
                    // is already deactivated so toast and ignore
                    Toast.makeText(context,
                            getResources().getString(R.string.assist_dialog_deactivated),
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    /**********************************************************************************************/

    private void driftSpeedDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        // still get that view root null...

        View inputView = inflater.inflate(R.layout.drift_speed_form, settingsContainer, false);
        dialogBuilder.setView(inputView);

        final AlertDialog alertDialog;
        final EditText userDriftInput = inputView.findViewById(R.id.drift_input);

        dialogBuilder.setTitle(R.string.drift_dialog_1);
        dialogBuilder.setMessage("");
        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // set a default and check edit text field
                        int userInputSpeed = 10;
                        String regexStr = "^[0-9]*$";

                        // simple check that string is length <= 2, max length of drift speed
                        if (userDriftInput.getText().length() != 0  && userDriftInput.getText().length() <= 2) {
                            if(userDriftInput.getText().toString().trim().matches(regexStr))
                                userInputSpeed = Integer.parseInt(userDriftInput.getText().toString());

                            userInputSpeed = checkDriftSpeed(userInputSpeed);
                            audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[11], userInputSpeed);
                            entryLogger("Jammer drift speed changed to " + userInputSpeed, false);
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null);
        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void carrierInputDialog() {
        // open dialog with field for carrierfrequency
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View inputView = inflater.inflate(R.layout.carrier_input_form, settingsContainer, false);
        dialogBuilder.setView(inputView);

        final EditText userCarrierInput = inputView.findViewById(R.id.carrier_input);
        final AlertDialog alertDialog;
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

                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[9], userInputCarrier);
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_DEFAULT_RANGED);

                        entryLogger(getResources().getString(R.string.jammer_dialog_14)
                                + userInputCarrier, false);
                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null);

        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void limitInputDialog() {
        // open dialog with 2 fields - carrier and limit
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        View inputView = inflater.inflate(R.layout.drift_limit_form, settingsContainer, false);
        dialogBuilder.setView(inputView);

        final EditText userLimitInput = inputView.findViewById(R.id.limit_input);
        final AlertDialog alertDialog;

        dialogBuilder.setTitle(R.string.jammer_dialog_5);

        dialogBuilder
                .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        String regexStr = "^[0-9]*$";

                        // simple check that string is length <= 4, max length of drift limit
                        int userInputLimit = AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT;
                        if (userLimitInput.getText().length() != 0 && userLimitInput.getText().length() <= 4) {
                            if(userLimitInput.getText().toString().trim().matches(regexStr))
                                userInputLimit = Integer.parseInt(userLimitInput.getText().toString());
                        }
                        userInputLimit = checkDriftLimit(userInputLimit);

                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[10], userInputLimit);
                        audioBundle.putInt(AudioSettings.AUDIO_BUNDLE_KEYS[8], AudioSettings.JAMMER_TYPE_USER_RANGED);

                        entryLogger("Jammer type changed to " + userInputLimit
                                + " Hz drift", false);

                    }
                })
                .setNegativeButton(R.string.dialog_button_cancel, null);

        alertDialog = dialogBuilder.create();
        alertDialog.show();
    }


    /**********************************************************************************************/

    /*

        CONFORM CHECKS FOR USER INPUT
    */
    private int checkCarrierFrequency(int carrierFrequency) {
        entryLogger("maxFreq: " + audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]), true);
        if (carrierFrequency > audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]))
            return audioBundle.getInt(AudioSettings.AUDIO_BUNDLE_KEYS[13]);

        else
            return Math.max(carrierFrequency, AudioSettings.MINIMUM_NUHF_FREQUENCY);
    }

    private int checkDriftLimit(int driftLimit) {
        if (driftLimit > AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT)
            return AudioSettings.DEFAULT_RANGE_DRIFT_LIMIT;

        else
            return Math.max(driftLimit, AudioSettings.MINIMUM_DRIFT_LIMIT);
    }

    private int checkDriftSpeed(int driftSpeed) {
        // is 1 - 10, then * 1000
        if (driftSpeed < 1)
            return 1;
        else
            return Math.min(driftSpeed, 10);
    }

    /**********************************************************************************************/

    private void entryLogger(String entry, boolean caution) {
        int start = settingsText.getText().length();
        settingsText.append("\n" + entry);
        int end = settingsText.getText().length();
        Spannable spannableText = (Spannable) settingsText.getText();
        if (caution) {
            spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
        }
        settingsText.requestFocus();
    }
}
