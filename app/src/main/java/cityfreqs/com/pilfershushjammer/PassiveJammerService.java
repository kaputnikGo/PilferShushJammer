package cityfreqs.com.pilfershushjammer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PassiveJammerService extends Service {

    // Intent actions to be handled here for control of mediaplayer
    public static final String TAG = "Pilfershush_Passive_Service";
    public static final String ACTION_START = "cityfreqs.com.pilfershushjammer.action.START";
    public static final String ACTION_STOP = "cityfreqs.com.pilfershushjammer.action.STOP";

    private static final int NOTIFY_PASSIVE_SERVICE_ID = 110;
    private static final String CHANNEL_ID = "PS";
    private static final String CHANNEL_NAME = "PilferShush";

    private PassiveJammer passiveJammer;

    private NotificationCompat.Builder notifyPassiveBuilder;


    public PassiveJammerService() {
        //default, for the manifest
    }

    public void loadPassiveService(PassiveJammer passiveJammer) {
        this.passiveJammer = passiveJammer;
    }

    /*

        MAIN SERVICE FUNCTIONS

    */

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
        if (action != null) {
            if (action.equals(ACTION_START)) {
                createNotification();
                startForeground(NOTIFY_PASSIVE_SERVICE_ID, notifyPassiveBuilder.build());
                startJammer();
            }
            else if (action.equals(ACTION_STOP)) {
                stopJammer();
                stopForeground(true);
                stopSelf();
            }
        }
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        // If we get killed, after returning from here, restart
        return START_STICKY;

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "PS_Jammer service stopped", Toast.LENGTH_SHORT).show();
    }


    private void createNotification() {
        //todo make and use a notifier class
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
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false);

    }

    //todo
    private void startJammer() {
        if (passiveJammer != null) {
            if (passiveJammer.startPassiveJammer()) {
                if (!passiveJammer.runPassiveJammer()) {
                    // has record state errors
                    stopJammer();
                }
                else {
                    //entryLogger(getResources().getString(R.string.main_scanner_3), true);
                    //notifyManager.notify(NOTIFY_PASSIVE_ID, notifyPassiveBuilder.build());
                }
            }
        }

        //
        Notification notification = notifyPassiveBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        startForeground(1, notification);
    }

    //todo
    private void stopJammer() {
        if (passiveJammer != null) {
            passiveJammer.stopPassiveJammer();
        }
        //
        stopForeground(true);
        stopSelf();
    }

}
