package com.guzzlio.data.importer

import com.guzzlio.core.model.DistanceUnit
import com.guzzlio.core.model.FuelEfficiencyAssignmentMethod
import com.guzzlio.core.model.FuelEfficiencyUnit
import com.guzzlio.core.model.VolumeUnit
import com.guzzlio.domain.model.AppPreferenceSnapshot
import com.guzzlio.domain.model.ImportIssue
import com.guzzlio.domain.model.OptionalFieldToggle
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

internal fun parseLooseDouble(raw: String?): Double? {
    val normalized = raw.orEmpty()
        .trim()
        .replace(",", "")
        .replace(Regex("[^0-9.\\-]"), "")
    return normalized.takeIf { it.isNotBlank() }?.toDoubleOrNull()
}

internal fun parseLooseInt(raw: String?): Int? = parseLooseDouble(raw)?.toInt()

internal fun parseYesNo(raw: String?): Boolean = when (raw?.trim()?.lowercase(Locale.US)) {
    "yes", "true", "1", "y" -> true
    else -> false
}

internal fun splitCommaList(raw: String?): List<String> = raw
    .orEmpty()
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

internal fun parseDate(raw: String?, pattern: String): LocalDate? = raw
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?.let { LocalDate.parse(it, DateTimeFormatter.ofPattern(pattern, Locale.US)) }

internal fun parseDateTime(
    date: String?,
    time: String?,
    datePattern: String,
    timePattern: String,
): LocalDateTime? {
    val left = date?.trim().orEmpty()
    val right = time?.trim().orEmpty()
    if (left.isBlank()) return null
    val formatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("$datePattern $timePattern")
        .toFormatter(Locale.US)
    return LocalDateTime.parse("$left $right", formatter)
}

internal fun parseBackupDateTime(raw: String?): LocalDateTime? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    val candidates = listOf("MM/dd/yyyy - HH:mm", "MM/dd/yyyy - hh:mm a")
    return candidates.firstNotNullOfOrNull { pattern ->
        runCatching { LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern, Locale.US)) }.getOrNull()
    }
}

internal fun readXml(inputStream: InputStream): Document {
    val factory = DocumentBuilderFactory.newInstance()
    factory.isNamespaceAware = false
    factory.isIgnoringComments = true
    factory.isCoalescing = true
    return factory.newDocumentBuilder().parse(inputStream)
}

internal fun readProperties(inputStream: InputStream): Map<String, String> {
    val properties = Properties()
    InputStreamReader(inputStream).use(properties::load)
    return properties.entries.associate { (key, value) -> key.toString() to value.toString() }
}

internal fun Document.rootElement(): Element = documentElement

internal fun Element.childElements(name: String): List<Element> = childNodes.asElementList().filter { it.tagName == name }

internal fun Element.firstChildElement(name: String): Element? = childNodes.asElementList().firstOrNull { it.tagName == name }

internal fun Element.childText(name: String): String = firstChildElement(name)?.textContent?.trim().orEmpty()

internal fun NodeList.asElementList(): List<Element> = buildList {
    for (index in 0 until length) {
        val node = item(index)
        if (node.nodeType == Node.ELEMENT_NODE) {
            add(node as Element)
        }
    }
}

internal fun preferencesFromMap(map: Map<String, String>): AppPreferenceSnapshot {
    val visibleFields = buildSet {
        if (map["acar.visible.vehicle-license-plate"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_LICENSE_PLATE)
        if (map["acar.visible.vehicle-vin"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_VIN)
        if (map["acar.visible.vehicle-insurance-policy"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_INSURANCE_POLICY)
        if (map["acar.visible.vehicle-body-style"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_BODY_STYLE)
        if (map["acar.visible.color"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_COLOR)
        if (map["acar.visible.vehicle-engine-displacement"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_ENGINE_DISPLACEMENT)
        if (map["acar.visible.vehicle-fuel-tank-capacity"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_FUEL_TANK_CAPACITY)
        if (map["acar.visible.vehicle-purchase-info"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_PURCHASE_INFO)
        if (map["acar.visible.vehicle-selling-info"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.VEHICLE_SELLING_INFO)
        if (map["acar.visible.payment-type"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.PAYMENT_TYPE)
        if (map["acar.visible.fuel-type"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.FUEL_TYPE)
        if (map["acar.visible.fuel-additive"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.FUEL_ADDITIVE)
        if (map["acar.visible.fueling-station"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.FUELING_STATION)
        if (map["acar.visible.service-center"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.SERVICE_CENTER)
        if (map["acar.visible.expense-center"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.EXPENSE_CENTER)
        if (map["acar.visible.tags"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TAGS)
        if (map["acar.visible.notes"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.NOTES)
        if (map["acar.visible.average-speed"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.AVERAGE_SPEED)
        if (map["acar.visible.driving-mode"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.DRIVING_MODE)
        if (map["acar.visible.driving-condition"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.DRIVING_CONDITION)
        if (map["acar.visible.trip-purpose"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TRIP_PURPOSE)
        if (map["acar.visible.trip-client"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TRIP_CLIENT)
        if (map["acar.visible.trip-location"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TRIP_LOCATION)
        if (map["acar.visible.trip-tax-deduction"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TRIP_TAX_DEDUCTION)
        if (map["acar.visible.trip-reimbursement"]?.toBooleanStrictOrNull() != false) add(OptionalFieldToggle.TRIP_REIMBURSEMENT)
    }

    return AppPreferenceSnapshot(
        distanceUnit = DistanceUnit.fromStorage(map["acar.distance-unit"]),
        volumeUnit = VolumeUnit.fromStorage(map["acar.volume-unit"]),
        fuelEfficiencyUnit = FuelEfficiencyUnit.fromStorage(map["acar.fuel-efficiency-unit"]),
        currencySymbol = map["acar.currency"].orEmpty().ifBlank { "$" },
        localeTag = map["acar.locale"].orEmpty().ifBlank { "system" },
        fullDateFormat = map["acar.date-format.full"].orEmpty().ifBlank { "MMM dd, yyyy" },
        compactDateFormat = map["acar.date-format.compact"].orEmpty().ifBlank { "MM/dd/yy" },
        browseSortDescending = map["acar.browse-records.sort-order"]?.equals("desc", ignoreCase = true) != false,
        fuelEfficiencyAssignmentMethod = FuelEfficiencyAssignmentMethod.fromStorage(map["acar.fuel-efficiency-calculation-method"]),
        reminderTimeAlertPercent = map["acar.due-time-alert.beginning-percentage"]?.toIntOrNull() ?: 10,
        reminderDistanceAlertPercent = map["acar.due-distance-alert.beginning-percentage"]?.toIntOrNull() ?: 10,
        backupFrequencyHours = map["acar.automatic-backup.frequency"]?.toIntOrNull() ?: 720,
        backupHistoryCount = map["acar.automatic-backup.history-count"]?.toIntOrNull() ?: 10,
        useLocation = map["acar.use-geographical-location"]?.toBooleanStrictOrNull() != false,
        notificationsEnabled = map["acar.status-bar.notifications"]?.toBooleanStrictOrNull() == true,
        notificationLedEnabled = map["acar.status-bar.notification-led"]?.toBooleanStrictOrNull() != false,
        visibleFields = visibleFields,
    )
}

internal fun issue(
    message: String,
    section: String,
    rowNumber: Int? = null,
    severity: ImportIssue.Severity = ImportIssue.Severity.WARNING,
): ImportIssue = ImportIssue(severity = severity, message = message, section = section, rowNumber = rowNumber)
