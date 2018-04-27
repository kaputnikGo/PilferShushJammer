package cityfreqs.com.pilfershushjammer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

public class PSJammerService extends Service {
    public static final String TAG = "PSJammer_service";

    //TODO active jamming as a service - use audio output whitenoise and hpf/bpf

    public static final String START_SERVICE_ACTION = "PSJammerService.action.start";
    public static final String STOP_SERVICE_ACTION = "PSJammerService.action.stop";

    final int NOTIFICATION_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent.getAction().equals(START_SERVICE_ACTION)) {
            processStartService();
        }
        else if (intent.getAction().equals(STOP_SERVICE_ACTION)) {
            processStopService();
        }
        return START_STICKY;
    }

    private void processStartService() {
        //
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);


        NotificationManager nm = (NotificationManager) getApplicationContext()
                .getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder builder = new Notification.Builder(getApplicationContext());

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.ic_stat_logo_notify_jammer)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher))
                .setContentTitle("jamming service running")
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
