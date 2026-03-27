param(
    [string]$Adb = "adb",
    [string]$ModelPath = "$PSScriptRoot\Qwen2.5-3B-Instruct-Q4_K_M.gguf",
    [string]$PackageName = "com.danichapps.simpleagent"
)

if (-not (Test-Path $ModelPath)) {
    Write-Error "Model file not found: $ModelPath"
    exit 1
}

$deviceDir = "/sdcard/Android/data/$PackageName/files/llm"

& $Adb shell "mkdir -p $deviceDir"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create target directory on device: $deviceDir"
    exit 1
}

& $Adb push $ModelPath "$deviceDir/"
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to push model to device"
    exit 1
}

Write-Host "Model pushed to $deviceDir/"
