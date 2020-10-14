package cityfreqs.com.pilfershushjammer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
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
    private static final String TAG = "PilferShush_Jammer-INSP";
    private boolean DEBUG;

    private Context context;

    private BackgroundChecker backgroundChecker;
    private TextView scannerText;

    private ImageButton appInspectButton;
    private ImageButton sdkListButton;
    private Dialog appInfoDialog;
    private AlertDialog.Builder dialogBuilder;

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

        recyclerView = view.findViewById(R.id.recyclerView);
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
        inspectorAdapter = new InspectorAdapter(mDataset);
        inspectorAdapter.setClickListener(this); // bind the listener
        recyclerView.setAdapter(inspectorAdapter);
        return view;
    }

    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "destroyView");
        inspectorAdapter.setClickListener(null); // remove the listener
    }

    // clickListener listening...
    @Override
    public void onClick(View view, int position) {
        displayAppInfoDialog(backgroundChecker.getOverrideScanAppEntry(position));
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
                userAppSummary();
            }
        });

        sdkListButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                displaySdkList();
            }
        });

        if (backgroundChecker.initChecker(context.getPackageManager())) {
            backgroundChecker.runChecker();
            backgroundChecker.checkAudioBeaconApps();
        }
        else {
            // has a problem getting SDK names from file, null or 0 length
            entryLogger(getResources().getString(R.string.userapp_scan_14), true);
        }
        entryLogger(getResources().getString(R.string.intro_6_3) + "\n", false);

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

        entryLogger("Selectable installed user app summary list below:" , false);
    }

    /**********************************************************************************************/

    // change to dialog
    private void userAppSummary() {
        if (backgroundChecker != null) {
            dialogBuilder = new AlertDialog.Builder(context);
            dialogBuilder.setTitle(getResources().getString(R.string.userapp_scan_13));
            String aboutScan = getResources().getString(R.string.userapp_scan_intro_1)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_2)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_3)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_4)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_5)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_6)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_7)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_8)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_9)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_10)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_11)
                    + "\n\n" + getResources().getString(R.string.userapp_scan_intro_12);

            dialogBuilder.setMessage(aboutScan);
            dialogBuilder.setCancelable(true);
            dialogBuilder
                    .setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Log.d(TAG, "Dismiss sdk list popup");
                        }
                    });
            appInfoDialog = dialogBuilder.create();
            appInfoDialog.show();
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at userApp summary.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void initDataset() {
        if (backgroundChecker != null) {
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
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at initDataset.");
            entryLogger("Background Checker not initialised.", true);
        }
    }

    private void displayAppInfoDialog(AppEntry appEntry) {
        AppEntryDialog appEntryDialog = new AppEntryDialog();
        // set appEntry basic info with styling
        appEntryDialog.initAppEntryDialog(context,
                this.getLayoutInflater(),
                appEntry);


        // build and display
        appInfoDialog = appEntryDialog.buildDialog();
        appInfoDialog.show();
    }

    private void displaySdkList() {
        // current list (in /raw) of NUHF/ACR SDK package names
        if (backgroundChecker != null) {
            //String sdkNames = backgroundChecker.displayAudioSdkNames();
            dialogBuilder = new AlertDialog.Builder(context);
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
            appInfoDialog = dialogBuilder.create();
            appInfoDialog.show();
        }
        else {
            if (DEBUG) Log.d(TAG, "backgroundChecker is NULL at displaySdkList.");
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
