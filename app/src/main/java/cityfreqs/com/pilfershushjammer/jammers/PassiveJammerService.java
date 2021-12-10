package cityfreqs.com.pilfershushjammer.jammers;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import cityfreqs.com.pilfershushjammer.MainActivity;
import cityfreqs.com.pilfershushjammer.R;


public class PassiveJammerService extends Service {
    private static final String TAG = "PSJAM_Service";

    public static final String ACTION_START_PASSIVE = "cityfreqs.com.pilfershushjammer.action.START_PASSIVE";
    public static final String ACTION_STOP_PASSIVE = "cityfreqs.com.pilfershushjammer.action.STOP_PASSIVE";
    public static final String ACTION_WIDGET_PASSIVE = "cityfreqs.com.pilfershushjammer.action.WIDGET_PASSIVE";

    private static final String CHANNEL_ID = "PilferShush";
    private static final String CHANNEL_NAME = "Passive Jammer";
    private static final int NOTIFY_ID = 11;

    private PassiveJammer passiveJammer;
    private Bundle audioBundle;
    private NotificationCompat.Builder notifyPassiveBuilder;
    private int retriggerCounter;

    public PassiveJammerService() {
        //default, for the manifest
    }

    @Override
    public void onCreate() {
        super.onCreate();
        retriggerCounter = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // possible 8.x req for startForeground(NOTIFY_ID, createNotification()); here
        registerReceiver(notifyStopReceiver, new IntentFilter("notifyStopPassive"));
        registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        registerReceiver(retriggerPassiveReceiver, new IntentFilter("RETRIGGER_PASSIVE"));

        audioBundle = new Bundle();
        if (intent != null) {
            audioBundle = intent.getExtras();
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ACTION_START_PASSIVE:
                        createNotification();
                        startPassiveService();
                        break;
                    case ACTION_STOP_PASSIVE:
                        stopPassiveService();
                        break;
                    case ACTION_WIDGET_PASSIVE:
                        widgetPassive();
                        break;
                }
            }
        }
        // 8.x needs return START_STICKY; ?
        return super.onStartCommand(intent, flags, startId);
    }

    //TODO check Binder binds as cause of getPortId exceptions
    // occurs with service in background auto-restarting PassiveJammer.java
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent,0);

        Intent notifyStopIntent = new Intent("notifyStopPassive");
        PendingIntent notifyStopPendingIntent = PendingIntent.getBroadcast(this,
                0, notifyStopIntent, 0);

        NotificationManagerCompat notifyManager = NotificationManagerCompat.from(this);
        NotificationChannelCompat channel = new NotificationChannelCompat.Builder(
                CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(CHANNEL_NAME)
                .setDescription(getString(R.string.service_state_1))
                .build();
        notifyManager.createNotificationChannel(channel);

        notifyPassiveBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET);

        notifyPassiveBuilder.setSmallIcon(R.mipmap.ic_stat_logo_notify_jammer)
                .setContentTitle(getResources().getString(R.string.app_status_10))
                .setContentText(getResources().getString(R.string.app_status_12))
                .setContentIntent(pendingIntent)
                .addAction(R.mipmap.ic_stat_logo_notify_jammer, getString(R.string.notify_stop_button), notifyStopPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);

    }

    private void startPassiveService() {
        passiveJammer = new PassiveJammer(getApplicationContext(), audioBundle, retriggerCounter);
        if (passiveJammer.startPassiveJammer()) {
            if (!passiveJammer.runPassiveJammer()) {
                // has record state errors
                stopPassiveService();
                return;
            }
            notifyFragment("true");
            // allow only up to 2 retrigger toasts, else excessive
            if (retriggerCounter <= 3) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.service_state_4),
                        Toast.LENGTH_SHORT).show();
            }

            Notification notification = notifyPassiveBuilder.build();
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
            // API 30 R: startForeground(notification, FOREGROUND_SERVICE_TYPE_MICROPHONE);
            // + manifest dec <service android:foregroundServiceType="microphone" />
            startForeground(NOTIFY_ID, notification);
        }
    }

    private void excessiveRestart() {
        // possible fight loop in Android 10 with user focus app gaining mic access,
        // usecase example: googlevoicesearch/omnibox
        // assumption is that as app has focus, then user wants mic to be used here
        // and PSJAM retrigger toasts will be annoying and unwanted.
        // is separate function in case of use for it, or better notify to user etc
        Toast.makeText(getApplicationContext(),
                "Excessive retriggers means jammer stopping now.",
                Toast.LENGTH_SHORT).show();
        stopPassiveService();
    }

    private void stopPassiveService() {
        if (passiveJammer != null) {
            passiveJammer.stopPassiveJammer();
            // allow only up to 2 retrigger toasts, else excessive
            if (retriggerCounter <= 3) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.service_state_5),
                        Toast.LENGTH_SHORT).show();
            }
        }
        //
        unregisterReceiver(headsetReceiver);
        notifyFragment("false");
        stopForeground(true);
        stopSelf();
        unregisterReceiver(notifyStopReceiver);
        unregisterReceiver(retriggerPassiveReceiver);
    }

    private void retriggerPassive() {
        // catch any hotplug change in mic hardware
        // or other changes ie Android 10 concurrent audio
        // stop if currently running passive
        if (passiveJammer != null) {
            passiveJammer.stopPassiveJammer();
            Toast.makeText(getApplicationContext(),
                    getResources().getString(R.string.service_state_5),
                    Toast.LENGTH_SHORT).show();
        }
        notifyFragment("false");
        stopForeground(true);
        // then restart
        // check for retrigger count is excessive
        // excessive counts approx 20-40 are nuts, mute them, testing count of 3
        retriggerCounter++;
        if (retriggerCounter >= 3) {
            Log.d(TAG, "excessiveRestart called at counter: " + retriggerCounter);
            excessiveRestart();
        }
        else {
            startPassiveService();
            Log.d(TAG, "retriggerPassive called at counter: " + retriggerCounter);
        }
    }

    private void widgetPassive() {
        // called whether service is running or not!
        // cannot run without audioBundle, so run activity
        Log.d(TAG, "widgetPassive called");
        Intent widgetIntent = new Intent(this, MainActivity.class);
        widgetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(widgetIntent);
    }

    private void notifyFragment(String running) {
        Log.d(TAG, "jammer running: " + running);
        Intent intent = new Intent("passive_running");
        // You can also include some extra data.
        intent.putExtra("message", running);
        sendBroadcast(intent);
        //
    }

    private final BroadcastReceiver notifyStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("notifyStopPassive")) {
                    stopPassiveService();
                }
            }
        }
    };

    private final BroadcastReceiver retriggerPassiveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("RETRIGGER_PASSIVE")) {
                    retriggerPassive();
                }
            }
        }
    };

    private final BroadcastReceiver headsetReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        Log.d(TAG, "Headset not present.");
                        retriggerPassive();
                        break;
                    case 1:
                        Log.d(TAG, "Headset present.");
                        retriggerPassive();
                        break;
                    default:
                        Log.d(TAG, "Headset state unknown.");
                }
            }
        }
    };
}