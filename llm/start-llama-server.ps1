param(
    [string]$LlamaServerExe = "$PSScriptRoot\bin\llama-server.exe",
    [string]$ModelPath = "$PSScriptRoot\Qwen2.5-3B-Instruct-Q4_K_M.gguf",
    [int]$Port = 8080,
    [int]$ContextSize = 4096,
    [int]$Threads = 8
)

if (-not (Test-Path $LlamaServerExe)) {
    Write-Error "Не найден llama-server.exe: $LlamaServerExe"
    exit 1
}

if (-not (Test-Path $ModelPath)) {
    Write-Error "Не найдена модель: $ModelPath"
    exit 1
}

& $LlamaServerExe `
    --host 0.0.0.0 `
    --port $Port `
    --model $ModelPath `
    --ctx-size $ContextSize `
    --threads $Threads `
    --n-predict 512
