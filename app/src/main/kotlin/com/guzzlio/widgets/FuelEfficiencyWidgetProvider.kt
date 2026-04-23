package com.guzzlio.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.guzzlio.R
import com.guzzlio.domain.model.FuelWidgetMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FuelEfficiencyWidgetProvider : AppWidgetProvider() {
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
        const val ACTION_REFRESH: String = "com.guzzlio.widgets.FUEL_EFFICIENCY_REFRESH"

        fun refreshAll(context: Context) {
            FuelMetricWidgetSupport.refreshAll(
                context = context,
                providerClass = FuelEfficiencyWidgetProvider::class.java,
                metric = FuelWidgetMetric.FUEL_EFFICIENCY,
                refreshAction = ACTION_REFRESH,
                titleRes = R.string.widget_fuel_efficiency_title,
                emptyTitleRes = R.string.widget_fuel_efficiency_empty_title,
                emptySubtitleRes = R.string.widget_fuel_efficiency_empty_subtitle,
            )
        }
    }
}
