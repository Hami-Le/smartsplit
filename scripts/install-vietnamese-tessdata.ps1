param(
    [string]$TessdataDirectory = "$env:ProgramFiles\Tesseract-OCR\tessdata"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $TessdataDirectory)) {
    throw "Không tìm thấy thư mục tessdata: $TessdataDirectory. Hãy cài Tesseract trước."
}

$models = @{
    "vie.traineddata" = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/vie.traineddata"
    "eng.traineddata" = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/eng.traineddata"
}

foreach ($entry in $models.GetEnumerator()) {
    $destination = Join-Path $TessdataDirectory $entry.Key
    if (Test-Path $destination) {
        Write-Host "$($entry.Key) đã tồn tại, bỏ qua." -ForegroundColor DarkGray
        continue
    }
    Write-Host "Đang tải $($entry.Key)..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $entry.Value -OutFile $destination
}

Write-Host "Đã cài dữ liệu ngôn ngữ vie+eng vào $TessdataDirectory" -ForegroundColor Green
