package com.guzzlio.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.guzzlio.MainActivity
import com.guzzlio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderWidgetProvider : AppWidgetProvider() {
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
        const val ACTION_REFRESH: String = "com.guzzlio.widgets.REMINDER_REFRESH"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ReminderWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                refreshAsync(context, manager, ids)
            }
        }

        private fun refreshAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                val container = (appContext as com.guzzlio.GuzzlioApplication).container
                val reminders = container.repository.getUpcomingReminderWidgets(limit = 3)
                ids.forEach { appWidgetId ->
                    manager.updateAppWidget(appWidgetId, buildRemoteViews(appContext, appWidgetId, reminders))
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            appWidgetId: Int,
            reminders: List<com.guzzlio.domain.model.ReminderWidgetItem>,
        ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_reminders).apply {
            setTextViewText(R.id.widget_title, context.getString(R.string.widget_reminders_title))
            setOnClickPendingIntent(
                R.id.widget_refresh,
                PendingIntent.getBroadcast(
                    context,
                    appWidgetId,
                    Intent(context, ReminderWidgetProvider::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            setOnClickPendingIntent(
                R.id.widget_open_app,
                PendingIntent.getActivity(
                    context,
                    appWidgetId + 1000,
                    Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )

            val titleIds = listOf(R.id.reminder_title_1, R.id.reminder_title_2, R.id.reminder_title_3)
            val subtitleIds = listOf(R.id.reminder_subtitle_1, R.id.reminder_subtitle_2, R.id.reminder_subtitle_3)
            titleIds.zip(subtitleIds).forEachIndexed { index, (titleId, subtitleId) ->
                val item = reminders.getOrNull(index)
                if (item == null) {
                    if (index == 0) {
                        setTextViewText(titleId, context.getString(R.string.widget_reminders_empty_title))
                        setTextViewText(subtitleId, context.getString(R.string.widget_reminders_empty_subtitle))
                    } else {
                        setTextViewText(titleId, "")
                        setTextViewText(subtitleId, "")
                    }
                } else {
                    setTextViewText(titleId, ReminderWidgetFormatter.title(item))
                    setTextViewText(subtitleId, ReminderWidgetFormatter.subtitle(item))
                }
            }
        }
    }
}
