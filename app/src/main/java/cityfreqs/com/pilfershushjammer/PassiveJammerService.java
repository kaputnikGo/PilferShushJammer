package cityfreqs.com.pilfershushjammer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PassiveJammerService extends Service {

    public static final String ACTION_START_PASSIVE = "cityfreqs.com.pilfershushjammer.action.START";
    public static final String ACTION_STOP_PASSIVE = "cityfreqs.com.pilfershushjammer.action.STOP";

    private static final int NOTIFY_PASSIVE_SERVICE_ID = 110;
    private static final String CHANNEL_ID = "PS";
    private static final String CHANNEL_NAME = "PilferShush";

    private PassiveJammer passiveJammer;
    private AudioSettings audioSettings;

    private NotificationCompat.Builder notifyPassiveBuilder;


    public PassiveJammerService() {
        //default, for the manifest
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // called when app removed from running apps list
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                createAudioSettings(extras);
            }

            String action = intent.getAction();
            if (action != null) {
                if (action.equals(ACTION_START_PASSIVE)) {
                    createNotification();
                    startJammerService();
                    Toast.makeText(getApplicationContext(), "Passive Jammer service started.",
                            Toast.LENGTH_SHORT).show();
                } else if (action.equals(ACTION_STOP_PASSIVE)) {
                    stopJammerService();
                    Toast.makeText(getApplicationContext(), "Passive Jammer service stopped.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //
    }

    private void createAudioSettings(Bundle extras) {
        audioSettings = new AudioSettings();
        audioSettings.setBasicAudioInSettings(
                extras.getInt("sampleRate"),
                extras.getInt("bufferSize"),
                extras.getInt("encoding"),
                extras.getInt("channelConfig")
        );
        audioSettings.setAudioSource(extras.getInt("audioSource"));
    }

    private void createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent,0);

        NotificationManager notifyManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("PilferShush Jammer notifications");
            notifyManager.createNotificationChannel(channel);
        }

        notifyPassiveBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);

        notifyPassiveBuilder.setSmallIcon(R.mipmap.ic_stat_logo_notify_jammer)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getResources().getString(R.string.app_status_10))
                .setContentText(getResources().getString(R.string.app_status_12))
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);

    }


    private void startJammerService() {
        passiveJammer = new PassiveJammer(getApplicationContext(), audioSettings);
        if (passiveJammer.startPassiveJammer()) {
            if (!passiveJammer.runPassiveJammer()) {
                // has record state errors
                stopJammerService();
            }
        }
        //
        Notification notification = notifyPassiveBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        startForeground(1, notification);
    }

    private void stopJammerService() {
        if (passiveJammer != null) {
            passiveJammer.stopPassiveJammer();
        }
        //
        stopForeground(true);
        stopSelf();
    }

}
