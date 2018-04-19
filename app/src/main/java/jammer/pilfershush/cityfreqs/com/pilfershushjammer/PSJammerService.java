package jammer.pilfershush.cityfreqs.com.pilfershushjammer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import static jammer.pilfershush.cityfreqs.com.pilfershushjammer.MainActivity.START_PASSIVE_ACTION;
import static jammer.pilfershush.cityfreqs.com.pilfershushjammer.MainActivity.STOP_PASSIVE_ACTION;

public class PSJammerService extends Service {
    public static final String TAG = "PSJammer_service";

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

    void processStartService() {
        //TODO check this huh

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round);
        //
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(icon)
                .setContentTitle("PilferShush Jammer")
                .setTicker("PilferShush Jammer")
                .setContentText("Passive")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);
        Notification notification = builder.build();

        startForeground(NOTIFICATION_ID, notification);
    }

    void processStopService() {
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // only for bound services
        return null;
    }
}
