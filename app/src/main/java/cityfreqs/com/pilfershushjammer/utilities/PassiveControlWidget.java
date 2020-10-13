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
import static cityfreqs.com.pilfershushjammer.jammers.PassiveJammerService.ACTION_WIDGET_PASSIVE;

public class PassiveControlWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // called on install of widget to homescreen, unless have a widget configuration activity
        for (int appWidgetId : appWidgetIds) {
            Intent buttonIntent = new Intent(context, PassiveJammerService.class);
            buttonIntent.setAction(ACTION_WIDGET_PASSIVE);
            PendingIntent pendingButtonIntent = PendingIntent.getService(context, 0, buttonIntent, 0);

            // wrap the whole widget layout in a view to capture button and text touch
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.passive_control_widget);
            views.setOnClickPendingIntent(R.id.passive_control_button, pendingButtonIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);

            //TODO check and start the Passive service to account for Android 11
            // can't create widget on screen if service already running...
            Log.d("PS_WIDGET", "press the button and update!");

            /*
            Intent mainIntent = new Intent(context, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);
             */


            ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            assert manager != null;
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (PassiveJammerService.class.getName().equals(service.service.getClassName())) {
                    Toast.makeText(context, "service already running", Toast.LENGTH_SHORT).show();
                    Log.d("PS_WIDGET", "service running");
                }
                else {
                    // Sam4 said service NOT started when it was, still worked though
                    Toast.makeText(context, "service NOT started", Toast.LENGTH_SHORT).show();
                    Log.d("PS_WIDGET", "service NOT running");
                }
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // not needed?
        if (intent != null) {
            if (intent.getAction().equals(ACTION_WIDGET_PASSIVE)) {
                Log.d("PS_WIDGET", "onReceive reached");
            }
            else {
                Log.d("PS_WIDGET", "onReceive action not WIDGET_PASSIVE!");
            }
        }
    }

    @Override
    public void onEnabled(Context context) {
        // nothing yet
        // called when widget created for first time
        Log.d("PS_WIDGET", "onEnabled reached");
    }

    @Override
    public void onDisabled(Context context) {
        // nothing yet
    }
}

