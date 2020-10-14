package cityfreqs.com.pilfershushjammer.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.utilities.AppEntry;

public class AppEntryDialog {
    private Context context;
    private View dialogView;
    private TextView multiTextView;
    private AlertDialog.Builder builder;
    AppEntry appEntry;


    @SuppressLint("InflateParams")
    public void initAppEntryDialog(Context context, LayoutInflater inflater, AppEntry appEntry) {
        this.context = context;
        this.appEntry = appEntry;

        builder = new AlertDialog.Builder(context);
        //
        dialogView = inflater.inflate(R.layout.app_entry_dialog, null);
        multiTextView = dialogView.findViewById(R.id.app_entry_title);
        multiTextView.setText(appEntry.getActivityName());
        multiTextView.setTextColor(Color.YELLOW);
        multiTextView = dialogView.findViewById(R.id.app_entry_package);
        multiTextView.setText(appEntry.getPackageName());
        ImageView iconView = dialogView.findViewById(R.id.app_entry_icon);
        //TODO check for API 22 +
        iconView.setImageDrawable(appEntry.getAppIcon());

        setCaution();

        setFeatures();
        // optional check and sets
        if (appEntry.getServicesNum() > 0) {
            if (appEntry.getAudioSdk())
                setServiceSDK();
            setServices();
        }
        if (appEntry.getReceiversNum() > 0) {
            if (appEntry.getAudioSdk())
                setReceiverSDK();
            setReceivers();
        }

        builder.setView(dialogView);
    }

    private void setCaution() {
        multiTextView = dialogView.findViewById(R.id.app_entry_caution);
        multiTextView.setTextColor(Color.YELLOW);
        if (appEntry.checkForCaution()) {
            // too wordy?
            multiTextView.setText(context.getResources().getString(R.string.userapp_scan_15));
        }
        else {
            multiTextView.setText(context.getResources().getString(R.string.userapp_scan_16));
        }
    }

    private void setFeatures() {
        // list of RECORD: boolean, ... , Accessibility: boolean
        // style them with colour for true?
        multiTextView = dialogView.findViewById(R.id.app_entry_features);
        multiTextView.setText(appEntry.getEntryFeatures());
    }

    private void setServiceSDK() {
        multiTextView = dialogView.findViewById(R.id.app_entry_service_sdk);
        // set heading
        multiTextView.setText(context.getResources().getString(R.string.app_entry_text_1));
        multiTextView.append("\n");
        // start, add text, end
        int start = multiTextView.getText().length();
        multiTextView.append(appEntry.getServiceWithSDK());
        int end = multiTextView.getText().length();

        Spannable spannableText = (Spannable) multiTextView.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
    }

    private void setServices() {
        multiTextView = dialogView.findViewById(R.id.app_entry_services_title);
        multiTextView.setText(context.getResources().getString(R.string.app_entry_text_2));
        multiTextView.setTextColor(Color.YELLOW);
        multiTextView = dialogView.findViewById(R.id.app_entry_services);
        multiTextView.setText(appEntry.printServiceNames());
    }

    private void setReceiverSDK() {
        multiTextView = dialogView.findViewById(R.id.app_entry_receiver_sdk);
        // set heading
        multiTextView.setText(context.getResources().getString(R.string.app_entry_text_3));
        multiTextView.append("\n");
        // start, add text, end
        int start = multiTextView.getText().length();
        multiTextView.append(appEntry.getReceiverWithSDK());
        int end = multiTextView.getText().length();

        Spannable spannableText = (Spannable) multiTextView.getText();
        spannableText.setSpan(new ForegroundColorSpan(Color.YELLOW), start, end, 0);
    }

    private void setReceivers() {
        multiTextView = dialogView.findViewById(R.id.app_entry_receivers_title);
        multiTextView.setText(context.getResources().getString(R.string.app_entry_text_4));
        multiTextView.setTextColor(Color.YELLOW);
        multiTextView = dialogView.findViewById(R.id.app_entry_receivers);
        multiTextView.setText(appEntry.printReceiverNames());
    }


    public Dialog buildDialog() {
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.dialog_button_okay, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // OK
            }
        });

        // create dialog and return it
        return builder.create();
    }
}
