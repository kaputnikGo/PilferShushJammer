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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AppEntry;
import cityfreqs.com.pilfershushjammer.utilities.AudioSettings;
import cityfreqs.com.pilfershushjammer.utilities.BackgroundChecker;


public class InspectorFragment extends Fragment implements InspectorAdapter.RecyclerViewClickListener{
    //TODO make this a RecyclerView with CardViews

    private static final String TAG = "PilferShush_Jammer-INSP";
    private boolean DEBUG;

    private Context context;

    private BackgroundChecker backgroundChecker;
    private TextView scannerText;

    private ImageButton appInspectButton;
    private ImageButton sdkListButton;

    protected RecyclerView recyclerView;
    protected RecyclerView.LayoutManager layoutManager;
    protected InspectorAdapter inspectorAdapter;
    protected String[] mDataset;
    protected TextView inspectorText;

    public InspectorFragment() {
        // no-args constructor
    }

    static InspectorFragment newInstance(Bundle audioBundle) {
        InspectorFragment inspectorFragment = new InspectorFragment();

        Bundle args = new Bundle();
        args.putBundle("audioBundle", audioBundle);
        inspectorFragment.setArguments(args);

        return inspectorFragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        if (getArguments() != null) {
            Bundle audioBundle = getArguments().getBundle("audioBundle");
            if (audioBundle != null) {
                DEBUG = audioBundle.getBoolean(AudioSettings.AUDIO_BUNDLE_KEYS[15], false);
            }
        }
        else {
            // catch for no args bundle.
            entryLogger("Failed to load audio settings.", true);
        }
        backgroundChecker = new BackgroundChecker(context, DEBUG);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_inspector, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(getActivity());
        // for non-changing size of list
        recyclerView.setHasFixedSize(true);
        // set to linear
        layoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(layoutManager);
        inspectorText = view.findViewById(R.id.inspector_text_view);

        scannerText = view.findViewById(R.id.scan_text);
        //appSummaryButton = view.findViewById(R.id.app_summary_button);
        appInspectButton = view.findViewById(R.id.app_inspect_button);
        sdkListButton = view.findViewById(R.id.sdk_list_button);

        // includes background checker init
        populateElements();
        // init recyclerView
        initDataset();
        //InspectorAdapter.RecyclerViewClickListener clickListener = null;
        inspectorAdapter = new InspectorAdapter(mDataset);
        inspectorAdapter.setClickListener(this); // Bind the listener
        recyclerView.setAdapter(inspectorAdapter);
        return view;
    }

    // clickListener listening...
    @Override
    public void onClick(View view, int position) {
        Log.d(TAG, "ClickListener position: " + position);
        setAppInfoDialog(backgroundChecker.getOverrideScanAppEntry(position));
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

        //TODO
        if (backgroundChecker.initChecker(context.getPackageManager())) {
            backgroundChecker.runChecker();
            backgroundChecker.checkAudioBeaconApps();
        }
        else {
            // has a problem getting SDK names from file, null or 0 length
            entryLogger(getResources().getString(R.string.userapp_scan_14), true);
        }

        introText();

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

    private void introText() {
        // simple and understandable statements about app usage.
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
            entryLogger(getResources().getString(R.string.userapp_scan_intro_9), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_10), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_11), false);
            entryLogger(getResources().getString(R.string.userapp_scan_intro_12) + "\n", false);
            audioAppEntryLog();
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at userApp summary.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    //TODO make each a clickable pop up with full report
    // needs to be a listview, major rewrite of Inspector
    private void initDataset() {
        // is zero here
        int datasetSize = backgroundChecker.getNumberAppEntries();
        Log.d(TAG, "Dataset size: " + datasetSize);
        mDataset = new String[datasetSize];

        ArrayList<AppEntry> appEntries = backgroundChecker.getAppEntries();
        if (appEntries.size() > 0) {
            int i = 0;
            for (AppEntry appEntry : appEntries) {
                mDataset[i] = appEntry.basicEntryPrint();
                i++;
            }
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
    // dialog class?
    private void setAppInfoDialog(AppEntry appEntry) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(appEntry.getActivityName());
        String appEntryString = appEntry.entryPrint();
        if (appEntry.getServicesNum() > 0)
            // better method
            appEntryString += "\nServices List:\n" + appEntry.printServiceNames();
        if (appEntry.getReceiversNum() > 0)
            appEntryString += "\nReceivers List:\n" + appEntry.printReceiverNames();
        dialogBuilder.setMessage(appEntryString);
        dialogBuilder.setCancelable(false);
        dialogBuilder
             .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "Dismiss app info popup");
                }
        });
        AlertDialog appInfoDialog = dialogBuilder.create();
        appInfoDialog.show();
    }

    //TODO make whole thing dialogs
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

    //TODO make a dialog
    private void displaySdkList() {
        // current list (in /raw) of NUHF/ACR SDK package names
        if (backgroundChecker != null) {
            //String sdkNames = backgroundChecker.displayAudioSdkNames();
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(getResources().getString(R.string.sdk_names_list));
            dialogBuilder.setMessage(backgroundChecker.displayAudioSdkNames());
            dialogBuilder.setCancelable(true);
            dialogBuilder
                    .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG, "Dismiss sdk list popup");
                        }
                    });
            AlertDialog alertDialog = dialogBuilder.create();
            alertDialog.show();
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
            spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
        }
        else {
            spannableText.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
        }
    }
}
