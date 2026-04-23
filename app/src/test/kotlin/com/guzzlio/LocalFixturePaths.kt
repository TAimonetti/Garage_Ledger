package com.guzzlio

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object LocalFixturePaths {
    private const val AbpProperty: String = "guzzlio.fixture.abp"
    private const val CsvProperty: String = "guzzlio.fixture.csv"
    private const val AbpEnv: String = "GARAGE_LEDGER_FIXTURE_ABP"
    private const val CsvEnv: String = "GARAGE_LEDGER_FIXTURE_CSV"

    private val defaultAbpCandidates: List<String> = listOf(
        "D:/Android/Note 20/Tonys SD Card/Backup/aCar-052817-0722.abp",
    )

    private val defaultCsvCandidates: List<String> = listOf(
        "D:/Android/Note 2/Backups/SD Backup 10-6-18/zonewalker-acar/aCar-records-053017-2253.csv",
    )

    val acarAbp: Path? = resolvePath(
        propertyName = AbpProperty,
        envName = AbpEnv,
        fallbackCandidates = defaultAbpCandidates,
    )

    val acarCsv: Path? = resolvePath(
        propertyName = CsvProperty,
        envName = CsvEnv,
        fallbackCandidates = defaultCsvCandidates,
    )

    fun describeMissingFixture(name: String, propertyName: String, envName: String): String =
        "Missing $name fixture. Set -D$propertyName=<path> or $envName."

    private fun resolvePath(
        propertyName: String,
        envName: String,
        fallbackCandidates: List<String>,
    ): Path? {
        val configured = System.getProperty(propertyName)
            ?.takeIf(String::isNotBlank)
            ?: System.getenv(envName)?.takeIf(String::isNotBlank)
        if (configured != null) {
            val resolved = Paths.get(configured)
            return resolved.takeIf(Files::exists)
        }
        return fallbackCandidates
            .asSequence()
            .map(Paths::get)
            .firstOrNull(Files::exists)
    }
}
