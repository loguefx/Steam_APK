# Build GameHub Open APK
# Requires Android SDK (install Android Studio or command-line tools)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot

# Find Android SDK
$sdkPath = $env:ANDROID_HOME
if (-not $sdkPath) { $sdkPath = $env:ANDROID_SDK_ROOT }
if (-not $sdkPath -and (Test-Path "$env:LOCALAPPDATA\Android\Sdk")) { $sdkPath = "$env:LOCALAPPDATA\Android\Sdk" }
if (-not $sdkPath -and (Test-Path "C:\Users\Logan\AppData\Local\Android\Sdk")) { $sdkPath = "C:\Users\Logan\AppData\Local\Android\Sdk" }

if (-not $sdkPath -or -not (Test-Path $sdkPath)) {
    Write-Host "Android SDK not found." -ForegroundColor Red
    Write-Host "  Set ANDROID_HOME to your SDK path, or install Android Studio (it installs the SDK)."
    Write-Host "  Then create or edit local.properties with: sdk.dir=C:\\path\\to\\Android\\Sdk"
    exit 1
}

$localProps = Join-Path $projectRoot "local.properties"
$sdkDir = $sdkPath -replace '\\', '\\'
Set-Content -Path $localProps -Value "sdk.dir=$sdkDir" -Encoding ASCII

Set-Location $projectRoot
& .\gradlew.bat assembleDebug --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Get-Item (Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk") -ErrorAction SilentlyContinue
if ($apk) { Write-Host "`nAPK: $($apk.FullName)" -ForegroundColor Green }
exit 0
