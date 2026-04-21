param(
    [switch]$IncludeSampleFixtures,
    [string]$AbpPath = "D:\Android\Note 20\Tonys SD Card\Backup\aCar-052817-0722.abp",
    [string]$CsvPath = "D:\Android\Note 2\Backups\SD Backup 10-6-18\zonewalker-acar\aCar-records-053017-2253.csv"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

& .\gradlew.bat --stop | Out-Null

$tasks = @(
    ":app:compileDebugKotlin",
    "testDebugUnitTest",
    ":app:lintDebug",
    ":app:compileDebugAndroidTestKotlin",
    ":app:assembleDebug"
)

foreach ($task in $tasks) {
    & .\gradlew.bat $task
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle task failed: $task"
    }
}

if ($IncludeSampleFixtures) {
    & .\scripts\run-sample-fixture-regression.ps1 -AbpPath $AbpPath -CsvPath $CsvPath
    if ($LASTEXITCODE -ne 0) {
        throw "Sample fixture regression failed."
    }
}
