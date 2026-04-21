package com.garageledger.data.export

import com.garageledger.domain.model.StatisticsChart
import com.garageledger.domain.model.StatisticsChartStyle
import com.garageledger.domain.model.StatisticsDashboard
import kotlin.math.max

class StatisticsHtmlExporter {
    fun export(dashboard: StatisticsDashboard): String = buildString {
        appendLine("<!DOCTYPE html>")
        appendLine("<html lang=\"en\">")
        appendLine("<head>")
        appendLine("<meta charset=\"utf-8\" />")
        appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />")
        appendLine("<title>${escape(dashboard.scopeLabel)} Statistics</title>")
        appendLine("<style>")
        appendLine(
            """
            body { font-family: "Segoe UI", Arial, sans-serif; background: #f7f4ec; color: #1f2a30; margin: 0; }
            main { max-width: 1040px; margin: 0 auto; padding: 24px; }
            h1, h2, h3 { margin: 0 0 12px; }
            .hero { background: linear-gradient(135deg, #203642, #54707d); color: #f7f4ec; border-radius: 18px; padding: 24px; }
            .hero p { margin: 6px 0 0; color: #d9e5eb; }
            .section { background: #ffffff; border-radius: 18px; margin-top: 18px; padding: 20px; box-shadow: 0 8px 22px rgba(32, 54, 66, 0.08); }
            .chips { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 14px; }
            .chip { background: #eef3f7; border-radius: 12px; padding: 10px 12px; min-width: 130px; }
            .chip .label { font-size: 12px; color: #54636c; display: block; margin-bottom: 4px; }
            .chip .value { font-weight: 700; font-size: 16px; }
            table { width: 100%; border-collapse: collapse; margin-top: 14px; }
            th, td { text-align: left; padding: 10px 8px; border-bottom: 1px solid #dbe4e8; vertical-align: top; }
            th { color: #41515b; font-size: 13px; }
            .chart { margin-top: 16px; padding: 14px; background: #f8fbfd; border-radius: 14px; border: 1px solid #d7e3e8; }
            .chart-meta { color: #5a6a73; margin-top: 8px; }
            svg { width: 100%; height: auto; display: block; }
            .muted { color: #5a6a73; }
            """.trimIndent(),
        )
        appendLine("</style>")
        appendLine("</head>")
        appendLine("<body>")
        appendLine("<main>")
        appendLine("<section class=\"hero\">")
        appendLine("<h1>${escape(dashboard.scopeLabel)} Statistics &amp; Charts</h1>")
        appendLine("<p>${escape(dashboard.periodDescription)}</p>")
        appendLine("<p>${escape(dashboard.preferences.distanceUnit.storageValue)} distance | ${escape(dashboard.preferences.volumeUnit.storageValue)} volume | ${escape(dashboard.preferences.fuelEfficiencyUnit.storageValue)} efficiency | ${escape(dashboard.preferences.currencySymbol)} currency</p>")
        appendLine("</section>")

        appendSection("Overall Summary") {
            appendChips(
                listOf(
                    "Vehicles" to dashboard.overall.vehicleCount.toString(),
                    "Records" to dashboard.overall.totalRecordCount.toString(),
                    "Fuel-Ups" to dashboard.overall.fillUpCount.toString(),
                    "Services" to dashboard.overall.serviceCount.toString(),
                    "Expenses" to dashboard.overall.expenseCount.toString(),
                    "Trips" to dashboard.overall.tripCount.toString(),
                    "Operating Cost" to dashboard.overall.totalOperatingCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                ),
            )
        }

        appendSection("Fill-Up Summary") {
            appendChips(
                listOf(
                    "Count" to dashboard.fillUps.count.toString(),
                    "Volume" to "${dashboard.fillUps.totalVolume.toDisplay()} ${dashboard.preferences.volumeUnit.storageValue}",
                    "Fuel Cost" to dashboard.fillUps.totalCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                    "Distance" to "${dashboard.fillUps.totalDistance.toDisplay()} ${dashboard.preferences.distanceUnit.storageValue}",
                    "Avg Efficiency" to dashboard.fillUps.averageFuelEfficiency.toDisplayWithUnit(dashboard.preferences.fuelEfficiencyUnit.storageValue),
                    "Last Efficiency" to dashboard.fillUps.lastFuelEfficiency.toDisplayWithUnit(dashboard.preferences.fuelEfficiencyUnit.storageValue),
                    "Avg Price" to dashboard.fillUps.averagePricePerUnit.toDisplayWithUnit("${dashboard.preferences.currencySymbol}/${dashboard.preferences.volumeUnit.storageValue}"),
                    "Last Price" to dashboard.fillUps.lastPricePerUnit.toDisplayWithUnit("${dashboard.preferences.currencySymbol}/${dashboard.preferences.volumeUnit.storageValue}"),
                ),
            )
        }

        appendSection("Service Summary") {
            appendChips(
                listOf(
                    "Count" to dashboard.services.count.toString(),
                    "Total Cost" to dashboard.services.totalCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                    "Average Cost" to dashboard.services.averageCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                ),
            )
        }

        appendSection("Expense Summary") {
            appendChips(
                listOf(
                    "Count" to dashboard.expenses.count.toString(),
                    "Total Cost" to dashboard.expenses.totalCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                    "Average Cost" to dashboard.expenses.averageCost.toDisplay(currency = dashboard.preferences.currencySymbol),
                ),
            )
        }

        appendSection("Trip Summary") {
            appendChips(
                listOf(
                    "Count" to dashboard.trips.count.toString(),
                    "Distance" to "${dashboard.trips.totalDistance.toDisplay()} ${dashboard.preferences.distanceUnit.storageValue}",
                    "Average Distance" to dashboard.trips.averageDistance.toDisplayWithUnit(dashboard.preferences.distanceUnit.storageValue),
                    "Reimbursement" to dashboard.trips.totalReimbursement.toDisplay(currency = dashboard.preferences.currencySymbol),
                    "Tax Deduction" to dashboard.trips.totalTaxDeduction.toDisplay(currency = dashboard.preferences.currencySymbol),
                    "Paid Trips" to dashboard.trips.paidCount.toString(),
                    "Open Trips" to dashboard.trips.openCount.toString(),
                ),
            )
        }

        dashboard.charts.forEach { chart ->
            appendSection(chart.title) {
                appendLine("<div class=\"chart\">")
                if (chart.points.isEmpty()) {
                    appendLine("<p class=\"muted\">No records in the current scope.</p>")
                } else {
                    appendLine(renderChartSvg(chart))
                    appendLine("<p class=\"chart-meta\">${chart.points.size} points | ${escape(chart.points.first().label)} to ${escape(chart.points.last().label)} | Unit: ${escape(chart.unitLabel)}</p>")
                    appendLine("<table>")
                    appendLine("<thead><tr><th>Recorded At</th><th>Vehicle</th><th>Label</th><th>Value</th></tr></thead>")
                    appendLine("<tbody>")
                    chart.points.forEach { point ->
                        appendLine(
                            "<tr><td>${escape(point.recordedAt.toString())}</td><td>${escape(point.vehicleName)}</td><td>${escape(point.label)}</td><td>${escape(point.value.toDisplay())}</td></tr>",
                        )
                    }
                    appendLine("</tbody></table>")
                }
                appendLine("</div>")
            }
        }

        appendLine("</main>")
        appendLine("</body>")
        appendLine("</html>")
    }

    private fun StringBuilder.appendSection(title: String, block: StringBuilder.() -> Unit) {
        appendLine("<section class=\"section\">")
        appendLine("<h2>${escape(title)}</h2>")
        block()
        appendLine("</section>")
    }

    private fun StringBuilder.appendChips(items: List<Pair<String, String>>) {
        appendLine("<div class=\"chips\">")
        items.forEach { (label, value) ->
            appendLine("<div class=\"chip\"><span class=\"label\">${escape(label)}</span><span class=\"value\">${escape(value)}</span></div>")
        }
        appendLine("</div>")
    }

    private fun renderChartSvg(chart: StatisticsChart): String {
        val width = 720f
        val height = 220f
        val left = 36f
        val right = width - 16f
        val top = 16f
        val bottom = height - 24f
        val min = chart.points.minOf { it.value }
        val maxValue = chart.points.maxOf { it.value }
        val range = max(0.0001, maxValue - min)

        fun xAt(index: Int): Float = if (chart.points.size <= 1) {
            (left + right) / 2f
        } else {
            left + ((right - left) * (index.toFloat() / (chart.points.lastIndex).toFloat()))
        }

        fun yAt(value: Double): Float = bottom - (((value - min) / range).toFloat() * (bottom - top))

        val grid = buildString {
            repeat(4) { index ->
                val fraction = index / 3f
                val y = bottom - ((bottom - top) * fraction)
                append("<line x1=\"$left\" y1=\"$y\" x2=\"$right\" y2=\"$y\" stroke=\"#d7e3e8\" stroke-width=\"1\" />")
            }
            append("<line x1=\"$left\" y1=\"$bottom\" x2=\"$right\" y2=\"$bottom\" stroke=\"#94a5af\" stroke-width=\"1.5\" />")
        }

        val series = when (chart.style) {
            StatisticsChartStyle.LINE -> {
                val points = chart.points.mapIndexed { index, point -> "${xAt(index)},${yAt(point.value)}" }.joinToString(" ")
                buildString {
                    append("<polyline fill=\"none\" stroke=\"#355c7d\" stroke-width=\"4\" stroke-linecap=\"round\" stroke-linejoin=\"round\" points=\"$points\" />")
                    chart.points.forEachIndexed { index, point ->
                        append("<circle cx=\"${xAt(index)}\" cy=\"${yAt(point.value)}\" r=\"3.5\" fill=\"#355c7d\" />")
                    }
                }
            }

            StatisticsChartStyle.BAR -> {
                val slotWidth = if (chart.points.size <= 1) right - left else (right - left) / chart.points.size
                val barWidth = minOf(slotWidth * 0.62f, 28f)
                buildString {
                    chart.points.forEachIndexed { index, point ->
                        val x = xAt(index) - (barWidth / 2f)
                        val y = yAt(point.value)
                        append("<rect x=\"$x\" y=\"$y\" width=\"$barWidth\" height=\"${bottom - y}\" rx=\"7\" ry=\"7\" fill=\"#2f7f66\" />")
                    }
                }
            }
        }

        return "<svg viewBox=\"0 0 $width $height\" role=\"img\" aria-label=\"${escape(chart.title)} chart\">$grid$series</svg>"
    }

    private fun escape(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun Double?.toDisplay(currency: String? = null): String = when (this) {
        null -> "n/a"
        else -> {
            val formatted = if (this % 1.0 == 0.0) toInt().toString() else "%,.4f".format(this).replace(",", "")
            currency?.let { "$it$formatted" } ?: formatted
        }
    }

    private fun Double?.toDisplayWithUnit(unit: String): String = when (this) {
        null -> "n/a"
        else -> "${toDisplay()} $unit"
    }
}
