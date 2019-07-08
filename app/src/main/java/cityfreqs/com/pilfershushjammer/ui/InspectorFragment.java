package cityfreqs.com.pilfershushjammer.ui;

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
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AppEntry;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;
import cityfreqs.com.pilfershushjammer.utilities.BackgroundChecker;


public class InspectorFragment extends Fragment {
    private static final String TAG = "PilferShush_Jammer-INSP";
    private boolean DEBUG;

    private Context context;
    private Bundle audioBundle;

    private BackgroundChecker backgroundChecker;
    private TextView scannerText;

    private ImageButton appSummaryButton;
    private ImageButton appInspectButton;
    private ImageButton sdkListButton;

    private InspectorFragment() {
        // no-args constructor
    }

    private InspectorFragment(Bundle audioBundle) {
        this.audioBundle = audioBundle;
    }

    static InspectorFragment newInstance(Bundle audioBundle) {
        InspectorFragment inspectorFragment = new InspectorFragment(audioBundle);

        Bundle args = new Bundle();
        args.putBundle("audioBundle", audioBundle);
        inspectorFragment.setArguments(args);

        return inspectorFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            audioBundle = getArguments().getBundle("audioBundle");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
        backgroundChecker = new BackgroundChecker(context, DEBUG);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inspector, container, false);
        scannerText = view.findViewById(R.id.scan_text);
        appSummaryButton = view.findViewById(R.id.app_summary_button);
        appInspectButton = view.findViewById(R.id.app_inspect_button);
        sdkListButton = view.findViewById(R.id.sdk_list_button);

        populateElements();
        return view;
    }

    /**********************************************************************************************/

    private void populateElements() {
        scannerText.setTextColor(Color.parseColor("#00ff00"));
        scannerText.setMovementMethod(new ScrollingMovementMethod());
        scannerText.setSoundEffectsEnabled(false); // no further click sounds
        scannerText.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public void onClick(View v) {
                // nothing
            }
        });

        appSummaryButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                userAppSummary();
            }
        });

        appInspectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                userAppCheckDialog();
            }
        });

        sdkListButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                displaySdkList();
            }
        });

        backgroundChecker.initChecker(context.getPackageManager());
        backgroundChecker.runChecker();

        introText();
    }

    private void introText() {
        // simple and understandable statements about app usage.
        entryLogger(getResources().getString(R.string.intro_5) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_6_1) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_6_2) + "\n", false);
        entryLogger(getResources().getString(R.string.intro_6_3) + "\n", false);
    }


    /**********************************************************************************************/

    private void userAppSummary() {
        if (backgroundChecker != null) {
            entryLogger("\n--------------------------------------\n", false);
            entryLogger(getResources().getString(R.string.userapp_scan_13), true);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_1), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_2) + "\n", false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_3), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_4), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_5), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_6), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_7), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_8), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_9) + "\n", false);
            audioAppEntryLog();
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at userApp summary.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void audioAppEntryLog() {
        ArrayList<AppEntry> appEntries = backgroundChecker.getAppEntries();

        if (appEntries.size() > 0) {
            for (AppEntry appEntry : appEntries) {
                entryLogger(appEntry.entryPrint(), appEntry.checkForCaution());
            }
        }
        else {
            entryLogger(context.getResources().getString(R.string.userapp_scan_12), false);
        }
    }

    private void userAppCheckDialog() {
        if (backgroundChecker != null) {
            String[] appNames = backgroundChecker.getOverrideScanAppNames();

            if (appNames != null && appNames.length > 0) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                dialogBuilder.setItems(appNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int which) {
                        // index position of clicked app name
                        listAppOverrideScanDetails(which);
                    }
                });
                dialogBuilder.setTitle(R.string.dialog_userapps);
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
            }
            else {
                entryLogger(getResources().getString(R.string.userapp_scan_4), true);
            }
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at userApp check.");
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

    private void displaySdkList() {
        // current list (in /raw) of NUHF/ACR SDK package names
        if (backgroundChecker != null) {
            entryLogger("\n--------------------------------------\n", false);
            entryLogger(getResources().getString(R.string.sdk_names_list) + "\n"
                    + backgroundChecker.displayAudioSdkNames(), false);
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL.");
            entryLogger("Background Checker not initialised.", true);
        }
    }


    /**********************************************************************************************/

    private void entryLogger(String entry, boolean caution) {
        int start = scannerText.getText().length();
        scannerText.append("\n" + entry);
        int end = scannerText.getText().length();
        Spannable spannableText = (Spannable) scannerText.getText();
        if (caution) {
            spannableText.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorAccent)), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
        }
    }
}
