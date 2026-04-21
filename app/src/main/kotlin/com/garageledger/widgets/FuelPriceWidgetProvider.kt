package com.garageledger.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.garageledger.R
import com.garageledger.domain.model.FuelWidgetMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FuelPriceWidgetProvider : AppWidgetProvider() {
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
        const val ACTION_REFRESH: String = "com.garageledger.widgets.FUEL_PRICE_REFRESH"

        fun refreshAll(context: Context) {
            FuelMetricWidgetSupport.refreshAll(
                context = context,
                providerClass = FuelPriceWidgetProvider::class.java,
                metric = FuelWidgetMetric.FUEL_PRICE,
                refreshAction = ACTION_REFRESH,
                titleRes = R.string.widget_fuel_price_title,
                emptyTitleRes = R.string.widget_fuel_price_empty_title,
                emptySubtitleRes = R.string.widget_fuel_price_empty_subtitle,
            )
        }
    }
}
