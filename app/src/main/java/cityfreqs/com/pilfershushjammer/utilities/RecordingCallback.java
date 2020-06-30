package cityfreqs.com.pilfershushjammer.utilities;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioRecordingConfiguration;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.util.List;

import cityfreqs.com.pilfershushjammer.R;


@RequiresApi(api = Build.VERSION_CODES.Q)
public class RecordingCallback extends AudioManager.AudioRecordingCallback {
    private final String TAG = "PSJammer-RecordCallback";
    private boolean isSilenced = false;
    private Context context;

    public RecordingCallback(Context context) {
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
        super.onRecordingConfigChanged(configs);

        if (configs.size() != 0) {
            for (int i = 0; i < configs.size(); i++) {
                AudioRecordingConfiguration config = configs.get(i);
                isSilenced = config.isClientSilenced();
            }
            displayWarning(isSilenced);
            // maybe get audioSource for added info about possible type of app takeover of mic
        }
        else {
            Log.d(TAG, "No audioConfig found");
        }
    }

    private void displayWarning(boolean isSilenced) {
        CharSequence warningText;
        if (isSilenced) {
            warningText = context.getResources().getString(R.string.recording_callback_silenced);
            Log.d(TAG, "client is silenced");
        }
        else {
            warningText = context.getResources().getString(R.string.recording_callback_not_silenced);
            Log.d(TAG, "client is NOT silenced");
        }
        Toast toast = Toast.makeText(context, warningText, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.show();
    }
}
