package cityfreqs.com.pilfershushjammer.utilities;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import cityfreqs.com.pilfershushjammer.R;
import cityfreqs.com.pilfershushjammer.jammers.PassiveJammerService;

import static android.content.Context.ACTIVITY_SERVICE;

public class PassiveControlWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // called on install of widget to homescreen
        for (int appWidgetId : appWidgetIds) {

            // TODO not this
            // Create an Intent to launch
            Intent buttonIntent = new Intent(context, PassiveJammerService.class);
            buttonIntent.setAction(PassiveJammerService.ACTION_WIDGET_PASSIVE);
            PendingIntent pendingButtonIntent = PendingIntent.getService(context, 0, buttonIntent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.passive_control_widget);
            views.setOnClickPendingIntent(R.id.passive_control_button, pendingButtonIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

            //TODO check and start the Passive service to account for Android 11
            Log.d("PS_WIDGET", "press the button and update!");

            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            assert manager != null;
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PassiveJammerService.class.getName().equals(service.service.getClassName())) {
                    //service running
                    Toast.makeText(context, "service already running", Toast.LENGTH_SHORT).show();
                    Log.d("PS_WIDGET", "service running");
                }
                else {
                    //context.startService(serviceIntent);
                    Toast.makeText(context, "service NOT started", Toast.LENGTH_SHORT).show();
                    Log.d("PS_WIDGET", "service NOT running");
                }
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

