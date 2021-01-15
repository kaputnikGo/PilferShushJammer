package cityfreqs.com.pilfershushjammer.assist;

import android.service.voice.VoiceInteractionService;
import android.util.Log;

public class AssistJammerService extends VoiceInteractionService {
    private static final String TAG = "PSJAM_AssistService";
    //
    @Override
    public void onReady() {
        super.onReady();
        Log.i(TAG, "Creating " + this);
    }
}
