param(
    [string]$Executable = "",
    [string]$DataPath = "",
    [string]$ImagePath = ""
)

$ErrorActionPreference = "Stop"

function Resolve-TesseractExecutable {
    param([string]$Configured)

    if ($Configured -and (Test-Path $Configured)) {
        return (Resolve-Path $Configured).Path
    }

    $command = Get-Command tesseract -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $default = Join-Path $env:ProgramFiles "Tesseract-OCR\tesseract.exe"
    if (Test-Path $default) {
        return $default
    }

    throw "Không tìm thấy tesseract.exe. Hãy cài Tesseract 5 hoặc truyền -Executable."
}

$exe = Resolve-TesseractExecutable -Configured $Executable
if (-not $DataPath) {
    $DataPath = Join-Path (Split-Path $exe -Parent) "tessdata"
}

Write-Host "Tesseract executable: $exe" -ForegroundColor Cyan
Write-Host "Tessdata directory:   $DataPath" -ForegroundColor Cyan

& $exe --version
if ($LASTEXITCODE -ne 0) {
    throw "Không chạy được Tesseract."
}

$langArgs = @()
if (Test-Path $DataPath) {
    $langArgs += @("--tessdata-dir", $DataPath)
}
$langArgs += "--list-langs"
$langs = & $exe @langArgs 2>&1
$langs | ForEach-Object { Write-Host $_ }

foreach ($required in @("vie", "eng")) {
    if (-not ($langs -contains $required)) {
        Write-Host "Thiếu $required.traineddata" -ForegroundColor Red
        Write-Host "Hãy cài bộ ngôn ngữ Vietnamese và English vào: $DataPath" -ForegroundColor Yellow
        exit 2
    }
}

Write-Host "Đã có đủ vie+eng. Tesseract local sẵn sàng." -ForegroundColor Green

if ($ImagePath) {
    if (-not (Test-Path $ImagePath)) {
        throw "Không tìm thấy ảnh: $ImagePath"
    }
    $outputBase = Join-Path $env:TEMP ("smartsplit-ocr-" + [guid]::NewGuid())
    $ocrArgs = @($ImagePath, $outputBase)
    if (Test-Path $DataPath) {
        $ocrArgs += @("--tessdata-dir", $DataPath)
    }
    $ocrArgs += @("-l", "vie+eng", "--oem", "1", "--psm", "6", "--dpi", "300")
    & $exe @ocrArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Tesseract không xử lý được ảnh thử."
    }
    $textFile = "$outputBase.txt"
    Write-Host "`n--- Văn bản nhận dạng ---" -ForegroundColor Cyan
    Get-Content $textFile -Encoding UTF8
    Remove-Item $textFile -Force -ErrorAction SilentlyContinue
}
