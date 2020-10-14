package cityfreqs.com.pilfershushjammer.utilities;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.List;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.jammers.PassiveJammerService;

import static android.content.Context.ACTIVITY_SERVICE;


@RequiresApi(api = Build.VERSION_CODES.Q)
public class RecordingCallback extends AudioManager.AudioRecordingCallback {
    private final String TAG = "PSJammer-RecordCallback";
    private boolean isSilenced = false;
    private final Context context;
    private Toast toast;

    public RecordingCallback(Context context) {
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
        super.onRecordingConfigChanged(configs);

        try {
            assert configs != null;
            if (configs.size() != 0) {
                for (int i = 0; i < configs.size(); i++) {
                    AudioRecordingConfiguration config = configs.get(i);
                    isSilenced = config.isClientSilenced();
                }
                displayWarning(isSilenced);
                // maybe get audioSource for added info about possible type of app takeover of mic
            } else {
                Log.d(TAG, "No audioConfig found");
            }
        } catch (IllegalStateException ex) {
            //TODO
            // not being caught as of yet, is a W/Binder ex.
            // Unable to retrieve AudioRecord pointer for getId()
            // with a call from AudioRecordingMonitorImpl.getMyConfig(AudioRecordingMonitorImpl.java:233)
           Log.d(TAG, "IllegalStateException for AudioRecord.getId().");
        }
    }

    private void displayWarning(boolean isSilenced) {
        CharSequence warningText;
        if (toast != null) {
            // cancel previous toasts if quick changes detected
            toast.cancel();
        }

        if (isSilenced) {
            warningText = context.getResources().getString(R.string.recording_callback_silenced);
            triggerServiceRunning();
            Log.d(TAG, "client is silenced");

        }
        else {
            warningText = context.getResources().getString(R.string.recording_callback_not_silenced);
            Log.d(TAG, "client is NOT silenced");
        }
        toast = Toast.makeText(context, warningText, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }

    // As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
    // For backwards compatibility, it will still return the caller's own services.
    private void triggerServiceRunning() {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PassiveJammerService.class.getName().equals(service.service.getClassName())) {
                Intent retriggerIntent = new Intent("RETRIGGER_PASSIVE");
                try {
                    context.sendBroadcast(retriggerIntent);
                    Log.d(TAG, "retriggerIntent to Service send.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.d(TAG, "Exception in retriggerIntent to Service send.");
                }
                Log.d(TAG, "Passive Service found and running.");
            }
            else {
                Log.d(TAG, "Passive Service not found or running.");
            }
        }
    }
}
