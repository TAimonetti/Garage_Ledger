package com.guzzlio.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.guzzlio.GuzzlioApplication
import com.guzzlio.MainActivity
import com.guzzlio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PredictionWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                refreshAll(context)
                pendingResult.finish()
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_REFRESH: String = "com.guzzlio.widgets.PREDICTIONS_REFRESH"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PredictionWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                refreshAsync(context.applicationContext, manager, ids)
            }
        }

        private fun refreshAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
            CoroutineScope(Dispatchers.IO).launch {
                val container = (context as GuzzlioApplication).container
                val item = container.repository.getPredictionWidgets(limit = 1).firstOrNull()
                ids.forEach { appWidgetId ->
                    manager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetId, item))
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            item: com.guzzlio.domain.model.PredictionWidgetItem?,
        ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_fuel_metric).apply {
            setTextViewText(R.id.widget_title, context.getString(R.string.widget_predictions_title))
            setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    Intent(context, PredictionWidgetProvider::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            setOnClickPendingIntent(
                R.id.widget_open_app,
                PendingIntent.getActivity(
                    context,
                    appWidgetId + 3000,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            if (item == null) {
                setTextViewText(R.id.metric_title_1, context.getString(R.string.widget_predictions_empty_title))
                setTextViewText(R.id.metric_subtitle_1, context.getString(R.string.widget_predictions_empty_subtitle))
                setTextViewText(R.id.metric_title_2, "")
                setTextViewText(R.id.metric_subtitle_2, "")
                setTextViewText(R.id.metric_title_3, "")
                setTextViewText(R.id.metric_subtitle_3, "")
            } else {
                setTextViewText(R.id.metric_title_1, PredictionWidgetFormatter.vehicleTitle(item))
                setTextViewText(R.id.metric_subtitle_1, PredictionWidgetFormatter.nextFillUpSubtitle(item))
                setTextViewText(R.id.metric_title_2, context.getString(R.string.widget_predictions_range_title))
                setTextViewText(R.id.metric_subtitle_2, PredictionWidgetFormatter.rangeSubtitle(item))
                setTextViewText(R.id.metric_title_3, context.getString(R.string.widget_predictions_trip_cost_title))
                setTextViewText(R.id.metric_subtitle_3, PredictionWidgetFormatter.tripCostSubtitle(item))
            }
        }
    }
}
