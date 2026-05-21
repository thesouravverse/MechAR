# MechAR - install latest APK on attached phone
[CmdletBinding()]
param(
    [string]$Downloads   = "$env:USERPROFILE\Downloads",
    [string]$ProjectRoot = "c:\Users\i64966\Desktop\Experiments\My_apps\MechAR",
    [string]$PackageName = "com.thesouravverse.mechar.debug",
    [string]$ZipPattern  = "MechAR-debug-apk*.zip"
)

$ErrorActionPreference = 'Stop'
function Step($msg) { Write-Host "[>] $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "[OK] $msg" -ForegroundColor Green }
function Warn($msg) { Write-Host "[!] $msg" -ForegroundColor Yellow }

# 1. adb on PATH
$env:Path = "$env:USERPROFILE\platform-tools;" + $env:Path
$adbCmd = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adbCmd) { throw "adb not found. Expected at $env:USERPROFILE\platform-tools\adb.exe" }

# 2. find latest zip in Downloads
Step "Looking for $ZipPattern in $Downloads"
$zip = Get-ChildItem -Path $Downloads -Filter $ZipPattern -File -ErrorAction SilentlyContinue |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $zip) { throw "No file matching $ZipPattern found in $Downloads. Download the artifact from GitHub Actions first." }
$ageMin = [int]((Get-Date) - $zip.LastWriteTime).TotalMinutes
$sizeMB = [math]::Round($zip.Length / 1MB, 2)
Ok ("Found: " + $zip.Name + "  (" + $sizeMB + " MB, " + $ageMin + " min old)")

# 3. compute next version folder
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
$destDir = Join-Path $artifactRoot ("MechAR-debug-apk_v" + $verPad)
New-Item -ItemType Directory -Force -Path $destDir | Out-Null
Step "Staging into $destDir"

# 4. move zip + extract
$destZip = Join-Path $destDir $zip.Name
Move-Item $zip.FullName $destZip -Force
Expand-Archive -Path $destZip -DestinationPath $destDir -Force
$apk = Get-ChildItem -Path $destDir -Filter "*.apk" -Recurse | Select-Object -First 1
if (-not $apk) { throw "No .apk found inside $destZip" }
$apkMB = [math]::Round($apk.Length / 1MB, 2)
Ok ("Extracted: " + $apk.Name + "  (" + $apkMB + " MB)")

# 5. check device
Step "Checking attached device"
$devLines = @((& adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
if (-not $devLines -or $devLines.Count -eq 0) { throw "No device attached. Plug phone in + enable USB debugging." }
$serial = ($devLines[0] -split "\s+")[0]
$model  = (& adb -s $serial shell getprop ro.product.model).Trim()
Ok ("Device: " + $model + " (" + $serial + ")")

# 6. uninstall old
Step "Uninstalling old $PackageName (if present)"
& adb -s $serial uninstall $PackageName 2>&1 | Out-Null

# 7. install
Step "Installing APK..."
$installOut = & adb -s $serial install -r $apk.FullName 2>&1
$installOut | ForEach-Object { Write-Host "    $_" }
if ($LASTEXITCODE -ne 0 -or ($installOut -join " ") -notmatch "Success") {
    throw "adb install failed."
}
Ok "Installed."

# 8. launch
Step "Launching app"
& adb -s $serial shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 | Out-Null
Ok "Launched. Point camera at floor, tap to deploy mech."

Write-Host ""
Write-Host "=== SHIPPED ===" -ForegroundColor Magenta
Write-Host ("  Version : v" + $verPad)
Write-Host ("  Device  : " + $model)
Write-Host ("  Package : " + $PackageName)
