<#
.SYNOPSIS
    Build, install, and run News Feed Simulator on a physical Android device via USB Debugging.

.DESCRIPTION
    Menjalankan alur lengkap Tasks Practical 2 PAM ITERA pada perangkat fisik:
      1. Memeriksa perangkat terhubung & USB Debugging diizinkan
      2. Menjalankan unit test (opsional, sebagai bukti 5 fitur Flow/Coroutines)
      3. Build + install APK debug
      4. Menjalankan aplikasi
      5. Menampilkan log tag "NewsApp" untuk proof of Flow, filter, map, StateFlow, async

.EXAMPLE
    .\install-usb.ps1
    .\install-usb.ps1 -SkipTests
    .\install-usb.ps1 -Uninstall
#>

param(
    [switch]$SkipTests,
    [switch]$Uninstall
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$Package   = 'com.example.pamtugas2'
$LogTag    = 'NewsApp'
$ApkPath   = 'app\build\outputs\apk\debug\app-debug.apk'

function Write-Step($text) { Write-Host "`n==> $text" -ForegroundColor Cyan }
function Write-Ok($text)   { Write-Host "    OK: $text" -ForegroundColor Green }
function Write-Warn2($text){ Write-Host "    !! $text" -ForegroundColor Yellow }
function Fail($text)      { Write-Host "`nGAGAL: $text" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------------------
# 1. Pastikan adb tersedia
# ---------------------------------------------------------------------------
Write-Step "Mencari Android Debug Bridge (adb)..."

$Adb = $null
$sdkDir = $null
if (Test-Path 'local.properties') {
    $line = Get-Content 'local.properties' | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
    if ($line) { $sdkDir = ($line -replace '^sdk\.dir=', '') -replace '\\:', ':' -replace '\\\\', '\' }
}

# Urutan pencarian: SDK dari local.properties -> ANDROID_HOME -> lokasi default
$candidates = @(
    (Join-Path $sdkDir 'platform-tools\adb.exe'),
    $(if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }),
    $(if ($env:ANDROID_SDK_ROOT) { Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' }),
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
) | Where-Object { $_ }

foreach ($c in $candidates) {
    if (Test-Path $c) { $Adb = $c; break }
}

if (-not $Adb) {
    Fail "adb.exe tidak ditemukan. Buka Android Studio > SDK Manager > SDK Tools, lalu centang 'Android SDK Platform-Tools'."
}
Write-Ok "adb: $Adb"

# Pastikan server adb berjalan
& $Adb start-server | Out-Null

# ---------------------------------------------------------------------------
# 2. Periksa perangkat & status USB Debugging
# ---------------------------------------------------------------------------
Write-Step "Memeriksa perangkat USB..."

$lines = & $Adb devices
$devices = $lines | Select-String -Pattern '^(\S+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value }

if (-not $devices) {
    Write-Host ""
    $lines | ForEach-Object { Write-Host "    $_" }
    Fail "Tidak ada perangkat 'device' (siap). Cek: (1) kabel USB, (2) Developer options > USB debugging ON, (3) terima dialog 'Allow USB debugging' di HP."
}

foreach ($d in $devices) {
    $model = (& $Adb -s $d shell getprop ro.product.model) -join '' -replace "`r", ''
    $rel   = (& $Adb -s $d shell getprop ro.build.version.release) -join '' -replace "`r", ''
    $sdk   = (& $Adb -s $d shell getprop ro.build.version.sdk) -join '' -replace "`r", ''
    Write-Ok "$model  |  Android $rel (API $sdk)  |  serial $d"
}
if ($devices.Count -gt 1) {
    Write-Warn2 "Ada $($devices.Count) perangkat. Script memakai yang pertama: $($devices[0])"
}
$Serial = $devices[0]

# ---------------------------------------------------------------------------
# 3. (Opsional) Uninstall
# ---------------------------------------------------------------------------
if ($Uninstall) {
    Write-Step "Menghapus instalasi lama..."
    & $Adb -s $Serial uninstall $Package 2>&1 | ForEach-Object { Write-Host "    $_" }
    Write-Ok "Selesai uninstall."
    exit 0
}

# ---------------------------------------------------------------------------
# 4. Unit test (bukti 5 fitur)
# ---------------------------------------------------------------------------
if (-not $SkipTests) {
    Write-Step "Menjalankan unit test (5 fitur Flow/Coroutines)..."
    & .\gradlew.bat :app:testDebugUnitTest --console=plain
    if ($LASTEXITCODE -ne 0) { Fail "Unit test gagal. Lihat detail di atas." }
    Write-Ok "Semua unit test lulus."
}

# ---------------------------------------------------------------------------
# 5. Build + Install APK
# ---------------------------------------------------------------------------
Write-Step "Build & install APK debug..."
& .\gradlew.bat :app:installDebug --console=plain
if ($LASTEXITCODE -ne 0) { Fail "Install gagal. Pastikan USB debugging sudah diizinkan." }
Write-Ok "APK terpasang di $Serial"

# ---------------------------------------------------------------------------
# 6. Jalankan aplikasi
# ---------------------------------------------------------------------------
Write-Step "Menjalankan aplikasi..."

# Beberapa HP (Infinix/Transsion, Xiaomi, dll.) menyetel global
# "persist.log.tag=I" sehingga seluruh log level DEBUG diblokir sistem.
# Kita paksa tag NewsApp ke level DEBUG agar bukti alur data selalu terlihat.
$null = & $Adb -s $Serial shell setprop log.tag.NewsApp D
Write-Ok "setprop log.tag.NewsApp=D (agar log debug tidak difilter OEM)"

& $Adb -s $Serial shell am force-stop $Package
& $Adb -s $Serial logcat -c
& $Adb -s $Serial shell am start -n "$Package/.MainActivity" | Out-Null
Write-Ok "Aplikasi launched. Buka 'News Feed' di layar HP."

# ---------------------------------------------------------------------------
# 7. Stream logcat
# ---------------------------------------------------------------------------
Write-Step "Menampilkan log '$LogTag' (bukti Aliran Data). Tekan Ctrl+C untuk berhenti."
Write-Host "    fit1 Flow   : pesan 'BERITA MASUK' tiap 2 detik" -ForegroundColor DarkGray
Write-Host "    fit2 filter : kategori 'Teknologi' disaring di logcat" -ForegroundColor DarkGray
Write-Host "    fit3 map    : judul terformat '[KATEGORI] ...'" -ForegroundColor DarkGray
Write-Host "    fit4 state  : pesan 'JUMLAH DIBACA'" -ForegroundColor DarkGray
Write-Host "    fit5 async  : pesan 'BUTIRAN LENGKAP' setelah delay 1 detik`n" -ForegroundColor DarkGray

& $Adb -s $Serial logcat -s "$LogTag`:D" '*:S'
