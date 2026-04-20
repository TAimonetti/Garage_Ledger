package com.garageledger.widgets

import android.content.Context

class GarageWidgetUpdater(
    context: Context,
) {
    private val appContext = context.applicationContext

    suspend fun refreshAll() {
        QuickAddWidgetProvider.refreshAll(appContext)
        ReminderWidgetProvider.refreshAll(appContext)
    }
}
