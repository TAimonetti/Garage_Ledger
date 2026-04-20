package com.garageledger.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.garageledger.R
import com.garageledger.shortcuts.QuickActionShortcutManager
import com.garageledger.shortcuts.QuickActionTarget

class QuickAddWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, QuickAddWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(context, manager, ids)
            }
        }

        fun updateWidgets(context: Context, manager: AppWidgetManager, ids: IntArray) {
            ids.forEach { appWidgetId ->
                manager.updateAppWidget(appWidgetId, buildRemoteViews(context, appWidgetId))
            }
        }

        private fun buildRemoteViews(context: Context, appWidgetId: Int): RemoteViews = RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
            setTextViewText(R.id.widget_title, context.getString(R.string.widget_quick_add_title))
            QuickActionTarget.entries.forEach { target ->
                setOnClickPendingIntent(
                    when (target) {
                        QuickActionTarget.FUEL_UP -> R.id.widget_action_fuel
                        QuickActionTarget.SERVICE -> R.id.widget_action_service
                        QuickActionTarget.EXPENSE -> R.id.widget_action_expense
                        QuickActionTarget.TRIP -> R.id.widget_action_trip
                    },
                    PendingIntent.getActivity(
                        context,
                        appWidgetId * 10 + target.ordinal,
                        QuickActionShortcutManager.launchIntent(context, target),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
        }
    }
}
