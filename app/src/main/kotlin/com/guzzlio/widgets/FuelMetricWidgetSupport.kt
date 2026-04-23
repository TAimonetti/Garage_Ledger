package com.guzzlio.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.StringRes
import com.guzzlio.GuzzlioApplication
import com.guzzlio.MainActivity
import com.guzzlio.R
import com.guzzlio.domain.model.FuelWidgetItem
import com.guzzlio.domain.model.FuelWidgetMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal object FuelMetricWidgetSupport {
    fun refreshAll(
        context: Context,
        providerClass: Class<*>,
        metric: FuelWidgetMetric,
        refreshAction: String,
        @StringRes titleRes: Int,
        @StringRes emptyTitleRes: Int,
        @StringRes emptySubtitleRes: Int,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        if (ids.isNotEmpty()) {
            refreshAsync(
                context = context.applicationContext,
                manager = manager,
                ids = ids,
                providerClass = providerClass,
                metric = metric,
                refreshAction = refreshAction,
                titleRes = titleRes,
                emptyTitleRes = emptyTitleRes,
                emptySubtitleRes = emptySubtitleRes,
            )
        }
    }

    private fun refreshAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
        providerClass: Class<*>,
        metric: FuelWidgetMetric,
        refreshAction: String,
        @StringRes titleRes: Int,
        @StringRes emptyTitleRes: Int,
        @StringRes emptySubtitleRes: Int,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val container = (context as GuzzlioApplication).container
            val items = container.repository.getFuelWidgetItems(metric = metric, limit = 3)
            ids.forEach { appWidgetId ->
                manager.updateAppWidget(
                    appWidgetId,
                    buildRemoteViews(
                        context = context,
                        appWidgetId = appWidgetId,
                        providerClass = providerClass,
                        refreshAction = refreshAction,
                        titleRes = titleRes,
                        emptyTitleRes = emptyTitleRes,
                        emptySubtitleRes = emptySubtitleRes,
                        items = items,
                    ),
                )
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        providerClass: Class<*>,
        refreshAction: String,
        @StringRes titleRes: Int,
        @StringRes emptyTitleRes: Int,
        @StringRes emptySubtitleRes: Int,
        items: List<FuelWidgetItem>,
    ): RemoteViews = RemoteViews(context.packageName, R.layout.widget_fuel_metric).apply {
        setTextViewText(R.id.widget_title, context.getString(titleRes))
        setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                Intent(context, providerClass).setAction(refreshAction),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        setOnClickPendingIntent(
            R.id.widget_open_app,
            PendingIntent.getActivity(
                context,
                appWidgetId + 2000,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        val titleIds = listOf(R.id.metric_title_1, R.id.metric_title_2, R.id.metric_title_3)
        val subtitleIds = listOf(R.id.metric_subtitle_1, R.id.metric_subtitle_2, R.id.metric_subtitle_3)
        titleIds.zip(subtitleIds).forEachIndexed { index, (titleId, subtitleId) ->
            val item = items.getOrNull(index)
            if (item == null) {
                if (index == 0) {
                    setTextViewText(titleId, context.getString(emptyTitleRes))
                    setTextViewText(subtitleId, context.getString(emptySubtitleRes))
                } else {
                    setTextViewText(titleId, "")
                    setTextViewText(subtitleId, "")
                }
            } else {
                setTextViewText(titleId, FuelWidgetFormatter.title(item))
                setTextViewText(subtitleId, FuelWidgetFormatter.subtitle(item))
            }
        }
    }
}
