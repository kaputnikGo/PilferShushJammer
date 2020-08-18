package cityfreqs.com.pilfershushjammer.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent restartIntent = new Intent(context, cityfreqs.com.pilfershushjammer.MainActivity.class);
            restartIntent.setClassName("cityfreqs.com.pilfershushjammer", "cityfreqs.com.pilfershushjammer.MainActivity");
            restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(restartIntent);
            Toast.makeText(context, "PilferShush Jammer auto-restart at boot.", Toast.LENGTH_LONG).show();
        }
    }
}
