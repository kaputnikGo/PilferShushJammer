package cityfreqs.com.pilfershushjammer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import jammer.pilfershush.cityfreqs.com.pilfershushjammer.R;

public class PSJammerService extends Service {
    public static final String TAG = "PSJammer_service";

    private PassiveJammer passiveJammer;
    private AudioSettings audioSettings;

    public static final String START_PASSIVE_ACTION = "PSJammerService.action.startpassive";
    public static final String STOP_PASSIVE_ACTION = "PSJammerService.action.stoppassive";


    final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(START_PASSIVE_ACTION)) {
            processStartService();
        }
        else if (intent.getAction().equals(STOP_PASSIVE_ACTION)) {
            processStopService();
        }
        return START_STICKY;
    }

    private void processStartService() {
        //TODO check this huh
        //
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);


        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher_notify)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher))
                .setContentTitle("passive jamming running")
                .setTicker("passive jamming running")
                .setContentText("Tap to return to app")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void processStopService() {
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        processStopService();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // only for bound services
        return null;
    }
}
