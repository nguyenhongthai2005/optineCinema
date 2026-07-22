# Safe template for local development. Copy this file to run-local.ps1.
# Replace placeholder values only in run-local.ps1 and never commit that local file.

Set-Location $PSScriptRoot

$env:GOOGLE_CLIENT_ID="your_google_client_id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET="your_google_client_secret"
$env:APP_FRONTEND_URL="http://localhost:5173"

# ======================================================
# ĐIỀN EMAIL VÀ APP PASSWORD CỦA BẠN VÀO ĐÂY:
$env:MAIL_USERNAME="your_mail@gmail.com"
$env:MAIL_PASSWORD="your_gmail_app_password"
$env:MAIL_FROM="your_mail@gmail.com"
# ======================================================

$env:VIETQR_BANK_ID="MB"
$env:VIETQR_ACCOUNT_NO="your_bank_account_no"
$env:VIETQR_ACCOUNT_NAME="YOUR_ACCOUNT_NAME"
$env:VIETQR_TEMPLATE="compact2"

$env:VIETQR_AUTO_CONFIRM_ENABLED="true"
$env:VIETQR_DEMO_AMOUNT_ENABLED="true"
$env:VIETQR_DEMO_AMOUNT_RATE="0.1"
$env:VIETQR_DEMO_ROUND_TO="1000"
$env:VIETQR_DEMO_MIN_AMOUNT="1000"

$env:ATTENDANCE_LOCATION_CHECK_ENABLED="true"
$env:ATTENDANCE_LOCATION_NAME="Opticine Cinema"
$env:ATTENDANCE_LATITUDE="17.467256"
$env:ATTENDANCE_LONGITUDE="106.587302"
$env:ATTENDANCE_RADIUS_METERS="5000"
$env:ATTENDANCE_MAX_ACCURACY_METERS="5000"
$env:ATTENDANCE_STRICT_ACCURACY_CHECK="false"
$env:ATTENDANCE_EARLY_CHECKIN_MINUTES="15"
$env:ATTENDANCE_LATE_GRACE_MINUTES="5"

if (Test-Path ".\mvnw.cmd") {
    & .\mvnw.cmd spring-boot:run
} else {
    & mvn spring-boot:run
}
