# MechAR — install latest APK on attached phone
#
# Workflow:
#   1. You download the artifact zip from GitHub Actions to ~/Downloads
#   2. You say the keyword "ship" in chat
#   3. This script auto-finds the latest MechAR zip, versions it into
#      MechAR/artifact/, extracts, uninstalls old build, installs new,
#      and launches the app on the connected device.

[CmdletBinding()]
param(
    [string]$Downloads   = "$env:USERPROFILE\Downloads",
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$PackageName = "com.thesouravverse.mechar.debug",
    [string]$ZipPattern  = "MechAR-debug-apk*.zip"
)

$ErrorActionPreference = 'Stop'
function Step($msg) { Write-Host "▶ $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "✓ $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "! $msg" -ForegroundColor Yellow }

# ---- 1. adb on PATH ----------------------------------------------------------
$env:Path = "$env:USERPROFILE\platform-tools;" + $env:Path
$adb = (Get-Command adb -ErrorAction SilentlyContinue)?.Source
if (-not $adb) { throw "adb not found. Expected at $env:USERPROFILE\platform-tools\adb.exe" }

# ---- 2. find latest zip in Downloads ----------------------------------------
Step "Looking for $ZipPattern in $Downloads"
$zip = Get-ChildItem -Path $Downloads -Filter $ZipPattern -File -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending |
       Select-Object -First 1
if (-not $zip) {
    throw "No $ZipPattern in $Downloads. Download the artifact from GitHub Actions first."
}
$ageMin = [math]::Round(((Get-Date) - $zip.LastWriteTime).TotalMinutes, 1)
Ok "$($zip.Name)  ($([math]::Round($zip.Length/1MB,2)) MB, $ageMin min old)"

# ---- 3. compute next version folder under artifact/ -------------------------
$artifactRoot = Join-Path $ProjectRoot "artifact"
New-Item -ItemType Directory -Force -Path $artifactRoot | Out-Null
$existing = Get-ChildItem $artifactRoot -Directory -Filter "MechAR-debug-apk_v*" -ErrorAction SilentlyContinue
$maxVer = 0
if ($existing) {
    $maxVer = ($existing | ForEach-Object {
        [int]([regex]::Match($_.Name, '_v(\d+)$').Groups[1].Value)
    } | Measure-Object -Maximum).Maximum
}
$nextVer = $maxVer + 1
$verPad  = "{0:D2}" -f $nextVer
$destDir = Join-Path $artifactRoot "MechAR-debug-apk_v$verPad"
Step "Versioning into MechAR-debug-apk_v$verPad"

# ---- 4. move + extract zip ---------------------------------------------------
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
$movedZip = Join-Path $destDir $zip.Name
Move-Item -Path $zip.FullName -Destination $movedZip -Force
Expand-Archive -Path $movedZip -DestinationPath $destDir -Force
$apk = Get-ChildItem -Path $destDir -Filter *.apk -Recurse | Select-Object -First 1
if (-not $apk) { throw "No .apk found inside extracted archive at $destDir" }
Ok "extracted: $($apk.Name)  ($([math]::Round($apk.Length/1MB,2)) MB)"

# ---- 5. device check ---------------------------------------------------------
Step "Checking attached device"
$devLines = adb devices | Select-String -Pattern "\sdevice$"
if (-not $devLines) { throw "No Android device attached (USB debugging enabled?)" }
$serial = ($devLines.Line.Trim() -split "\s+")[0]
$model  = adb -s $serial shell getprop ro.product.model
Ok "device: $model ($serial)"

# ---- 6. uninstall old build (ignore failure) --------------------------------
Step "Uninstalling old $PackageName (if present)"
$null = adb -s $serial uninstall $PackageName 2>&1
Ok "uninstall step done"

# ---- 7. install new ----------------------------------------------------------
Step "Installing APK ..."
$install = adb -s $serial install -r $apk.FullName 2>&1
$install | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0 -or ($install -notmatch 'Success')) {
    throw "adb install failed"
}
Ok "installed"

# ---- 8. launch ---------------------------------------------------------------
Step "Launching $PackageName"
$null = adb -s $serial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1
Ok "launch sent — watch your phone screen"

Write-Host ""
Write-Host "════════════════════════════════════════════════════"
Write-Host "  Installed MechAR v$verPad on $model"
Write-Host "  Artifact: $destDir"
Write-Host "════════════════════════════════════════════════════"
