package com.udacity.stockhawk.widget;

import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.sync.QuoteSyncJob;
import com.udacity.stockhawk.ui.DetailsActivity;
import com.udacity.stockhawk.ui.MainActivity;

/**
 * Widget provider for collection widget which contains all selected
 * stock symbols and their values.
 */
public class QuotesWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Update each of the app widgets with the remote adapter
        for (int appWidgetId : appWidgetIds) {

            // Instantiate the RemoteViews object for the app widget layout.
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_quotes);

            // Setup app name click behaviour
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            rv.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Setup the RemoteViews object to use a RemoteViews adapter.
            // This adapter connects to a RemoteViewsService through the specified intent
            // and populate the data.
            Intent intentList = new Intent(context, QuotesWidgetRemoteViewsService.class);
            rv.setRemoteAdapter(R.id.widget_list, intentList);

            // Setup list item click behaviour
            Intent clickTemplate = new Intent(context, DetailsActivity.class);
            PendingIntent pendingIntentTemplate = TaskStackBuilder.create(context)
                    .addNextIntentWithParentStack(clickTemplate)
                    .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setPendingIntentTemplate(R.id.widget_list, pendingIntentTemplate);

            // The empty view is displayed when the collection has no items.
            // It should be in the same layout used to instantiate the RemoteViews object above.
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            // Update current widget
            appWidgetManager.updateAppWidget(appWidgetId, rv);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (QuoteSyncJob.ACTION_DATA_UPDATED.equals(intent.getAction())) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetsIds = appWidgetManager.getAppWidgetIds(
                    new ComponentName(context, getClass()));
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetsIds, R.id.widget_list);
        }
    }
}