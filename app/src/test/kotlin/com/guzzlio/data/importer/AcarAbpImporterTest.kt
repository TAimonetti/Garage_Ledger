package com.guzzlio.data.importer

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Test

class AcarAbpImporterTest {
    private val importer = AcarAbpImporter()

    @Test
    fun import_supportsEventSubtypesCatalogsFromNewerBackups() {
        val archive = buildArchive(
            "event-subtypes.xml" to """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <event-subtypes>
                    <event-subtype id="1" type="service">
                        <name>Oil Change</name>
                        <notes>Engine oil service</notes>
                        <default-time-reminder-interval>3</default-time-reminder-interval>
                        <default-distance-reminder-interval>3000</default-distance-reminder-interval>
                    </event-subtype>
                    <event-subtype id="2" type="expense">
                        <name>Tolls</name>
                        <notes>Road fees</notes>
                    </event-subtype>
                </event-subtypes>
            """.trimIndent(),
            "fuel-types.xml" to "<fuel-types/>",
            "trip-types.xml" to "<trip-types/>",
            "vehicles.xml" to """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <vehicles>
                    <vehicle id="7">
                        <name>Corolla</name>
                        <active>true</active>
                    </vehicle>
                </vehicles>
            """.trimIndent(),
        )

        val imported = importer.import(ByteArrayInputStream(archive))

        assertThat(imported.serviceTypes).hasSize(1)
        assertThat(imported.serviceTypes.single().name).isEqualTo("Oil Change")
        assertThat(imported.serviceTypes.single().defaultTimeReminderMonths).isEqualTo(3)
        assertThat(imported.expenseTypes).hasSize(1)
        assertThat(imported.expenseTypes.single().name).isEqualTo("Tolls")
    }

    @Test
    fun import_warnsWhenBackupContainsFillupsButNoServiceHistoryOrReminders() {
        val archive = buildArchive(
            "fuel-types.xml" to "<fuel-types/>",
            "trip-types.xml" to "<trip-types/>",
            "vehicles.xml" to """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <vehicles>
                    <vehicle id="9">
                        <name>Tundra</name>
                        <active>true</active>
                        <fillup-records>
                            <fillup-record id="14">
                                <date>10/01/2018 - 08:29</date>
                                <odometer-reading>12345</odometer-reading>
                                <volume>15.5</volume>
                                <price-per-volume-unit>3.10</price-per-volume-unit>
                                <total-cost>48.05</total-cost>
                            </fillup-record>
                        </fillup-records>
                    </vehicle>
                </vehicles>
            """.trimIndent(),
        )

        val imported = importer.import(ByteArrayInputStream(archive))
        val messages = imported.issues.map { it.message }

        assertThat(imported.fillUpRecords).hasSize(1)
        assertThat(messages).contains(
            "This backup contains fill-up history but no service history. Some later aCar auto-backups omit service records; try an earlier manual or auto-backup if you expected service history.",
        )
        assertThat(messages).contains(
            "This backup contains fill-up history but no reminder rows. Guzzlio will seed reminder defaults from the service catalog after import.",
        )
    }

    @Test
    fun import_supportsNewerEventRecordsAndRemindersInsideVehiclesXml() {
        val archive = buildArchive(
            "event-subtypes.xml" to """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <event-subtypes>
                    <event-subtype id="28" type="service">
                        <name>Oil Change</name>
                        <notes>Engine oil service</notes>
                        <default-time-reminder-interval>3</default-time-reminder-interval>
                        <default-distance-reminder-interval>3000</default-distance-reminder-interval>
                    </event-subtype>
                </event-subtypes>
            """.trimIndent(),
            "fuel-types.xml" to "<fuel-types/>",
            "trip-types.xml" to "<trip-types/>",
            "vehicles.xml" to """
                <?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
                <vehicles>
                    <vehicle id="10">
                        <name>Corolla</name>
                        <active>true</active>
                        <reminders>
                            <reminder id="30" event-type="service" event-subtype-id="28">
                                <time-interval>3</time-interval>
                                <time-due>08/19/2018 - 21:59</time-due>
                                <time-unit>months</time-unit>
                                <distance-interval>3000.0</distance-interval>
                                <distance-due>116077.0</distance-due>
                            </reminder>
                        </reminders>
                        <event-records>
                            <event-record id="51">
                                <type>service</type>
                                <date>01/25/2020 - 19:23</date>
                                <notes>Oil only no filter</notes>
                                <odometer-reading>148610.0</odometer-reading>
                                <payment-type>Credit Card</payment-type>
                                <tags>oil,shop</tags>
                                <total-cost>28.0</total-cost>
                                <place-name>Quick Lube</place-name>
                                <place-full-address>123 Main St</place-full-address>
                                <place-longitude>-112.07</place-longitude>
                                <place-latitude>33.45</place-latitude>
                                <subtypes>
                                    <subtype id="28" />
                                </subtypes>
                            </event-record>
                        </event-records>
                    </vehicle>
                </vehicles>
            """.trimIndent(),
        )

        val imported = importer.import(ByteArrayInputStream(archive))

        assertThat(imported.serviceRecords).hasSize(1)
        assertThat(imported.serviceRecords.single().serviceTypeIds).containsExactly(28L)
        assertThat(imported.serviceRecords.single().serviceCenterName).isEqualTo("Quick Lube")
        assertThat(imported.serviceReminders).hasSize(1)
        assertThat(imported.serviceReminders.single().serviceTypeId).isEqualTo(28L)
        assertThat(imported.issues.map { it.message }).doesNotContain(
            "This backup contains fill-up history but no service history. Some later aCar auto-backups omit service records; try an earlier manual or auto-backup if you expected service history.",
        )
    }

    private fun buildArchive(vararg entries: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(StandardCharsets.UTF_8))
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }
}
