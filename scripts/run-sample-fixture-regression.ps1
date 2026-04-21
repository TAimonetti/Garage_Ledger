param(
    [string]$AbpPath = "D:\Android\Note 20\Tonys SD Card\Backup\aCar-052817-0722.abp",
    [string]$CsvPath = "D:\Android\Note 2\Backups\SD Backup 10-6-18\zonewalker-acar\aCar-records-053017-2253.csv"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

if (-not (Test-Path $AbpPath)) {
    throw "ABP fixture not found: $AbpPath"
}

if (-not (Test-Path $CsvPath)) {
    throw "CSV fixture not found: $CsvPath"
}

& .\gradlew.bat `
    "-DgarageLedger.fixture.abp=$AbpPath" `
    "-DgarageLedger.fixture.csv=$CsvPath" `
    testDebugUnitTest `
    --tests "com.garageledger.data.importer.AcarImportRegressionTest"
