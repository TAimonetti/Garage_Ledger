package com.garageledger.shortcuts

import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.garageledger.MainActivity
import com.garageledger.domain.model.RecordFamily

enum class QuickActionTarget(
    val routeSegment: String,
    val family: RecordFamily,
    val shortLabel: String,
    val longLabel: String,
    val iconResId: Int,
) {
    FUEL_UP(
        routeSegment = "fuelup",
        family = RecordFamily.FILL_UP,
        shortLabel = "New Fuel-Up",
        longLabel = "Add a new fuel-up",
        iconResId = android.R.drawable.ic_menu_add,
    ),
    SERVICE(
        routeSegment = "service",
        family = RecordFamily.SERVICE,
        shortLabel = "New Service",
        longLabel = "Add a new service record",
        iconResId = android.R.drawable.ic_menu_manage,
    ),
    EXPENSE(
        routeSegment = "expense",
        family = RecordFamily.EXPENSE,
        shortLabel = "New Expense",
        longLabel = "Add a new expense record",
        iconResId = android.R.drawable.ic_menu_save,
    ),
    TRIP(
        routeSegment = "trip",
        family = RecordFamily.TRIP,
        shortLabel = "New Trip",
        longLabel = "Add a new trip record",
        iconResId = android.R.drawable.ic_menu_directions,
    ),
    ;

    fun route(): String = "quickadd/$routeSegment"

    fun editorRoute(vehicleId: Long): String = when (family) {
        RecordFamily.FILL_UP -> "fuelup/$vehicleId/-1"
        RecordFamily.SERVICE -> "service/$vehicleId/-1"
        RecordFamily.EXPENSE -> "expense/$vehicleId/-1"
        RecordFamily.TRIP -> "trip/$vehicleId/-1"
    }

    companion object {
        fun fromRouteSegment(value: String?): QuickActionTarget? = entries.firstOrNull { it.routeSegment.equals(value, ignoreCase = true) }
    }
}

data class LaunchRequest(
    val route: String,
    val token: Long = System.nanoTime(),
)

object QuickActionShortcutManager {
    const val EXTRA_QUICK_ACTION: String = "quick_action_target"

    fun syncDynamicShortcuts(context: android.content.Context) {
        val shortcuts = QuickActionTarget.entries.map { target ->
            ShortcutInfoCompat.Builder(context, target.routeSegment)
                .setShortLabel(target.shortLabel)
                .setLongLabel(target.longLabel)
                .setIcon(IconCompat.createWithResource(context, target.iconResId))
                .setIntent(launchIntent(context, target))
                .build()
        }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }

    fun requestPinnedShortcut(context: android.content.Context, target: QuickActionTarget): Boolean {
        val shortcut = ShortcutInfoCompat.Builder(context, target.routeSegment)
            .setShortLabel(target.shortLabel)
            .setLongLabel(target.longLabel)
            .setIcon(IconCompat.createWithResource(context, target.iconResId))
            .setIntent(launchIntent(context, target))
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    fun launchRequest(intent: Intent?): LaunchRequest? {
        val segment = intent?.getStringExtra(EXTRA_QUICK_ACTION) ?: return null
        val target = QuickActionTarget.fromRouteSegment(segment) ?: return null
        return LaunchRequest(route = target.route())
    }

    fun launchIntent(context: android.content.Context, target: QuickActionTarget): Intent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        putExtra(EXTRA_QUICK_ACTION, target.routeSegment)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
