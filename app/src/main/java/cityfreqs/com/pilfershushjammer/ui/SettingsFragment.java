package cityfreqs.com.pilfershushjammer.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import cityfreqs.com.pilfershushjammer.R;
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
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

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

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch activeTypeSwitch = view.findViewById(R.id.active_type_switch);
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

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch activeSoundSwitch = view.findViewById(R.id.active_sound_switch);
        activeSoundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                audioBundle.putBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[7], isChecked);
                if (isChecked) {
                    //tones, default
                    entryLogger(getResources().getString(R.string.app_status_8), false);
                }
                else {
                    //whitenoise
                    entryLogger(getResources().getString(R.string.app_status_9), false);
                }
            }
        });

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch bufferTypeSwitch = view.findViewById(R.id.buffer_type_switch);
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
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                    }
                });
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
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                    }
                });

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
                .setNegativeButton(R.string.dialog_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // dismissed
                    }
                });

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
    }
}
