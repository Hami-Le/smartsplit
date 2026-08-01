# Nâng cấp Iteration 5B — Tesseract OCR local

Bản vá này thay Google Cloud Vision bằng Tesseract 5 chạy trực tiếp trên máy. Không cần billing, API key, `gcloud` hoặc Internet khi quét hóa đơn.

## 1. Thành phần thay đổi

- Xóa dependency `google-cloud-vision` khỏi Maven.
- Thêm `TesseractOcrClient` gọi `tesseract.exe` bằng `ProcessBuilder`.
- Tiền xử lý ảnh: phóng ảnh nhỏ, chuyển xám và chuẩn hóa tương phản.
- Dùng đồng thời dữ liệu ngôn ngữ `vie+eng`.
- Giới hạn thời gian OCR để tiến trình không treo backend.
- Giữ nguyên bảng `receipt_scans`, API và giao diện hiện tại; không cần migration database mới.
- Khi Tesseract chưa cài, ảnh vẫn được lưu và hệ thống chuyển sang trạng thái `MANUAL_REQUIRED`.

## 2. Cài Tesseract trên Windows

1. Cài Tesseract 5 bản 64-bit.
2. Trong trình cài đặt, chọn thêm ngôn ngữ Vietnamese và English nếu có.
3. Đường dẫn mặc định nên là:

```text
C:\Program Files\Tesseract-OCR\tesseract.exe
C:\Program Files\Tesseract-OCR\tessdata\vie.traineddata
C:\Program Files\Tesseract-OCR\tessdata\eng.traineddata
```

4. Đóng và mở lại IntelliJ.
5. Kiểm tra:

```powershell
tesseract --version
tesseract --list-langs
```

Danh sách phải có `vie` và `eng`.

Nếu thiếu ngôn ngữ, mở PowerShell bằng quyền Administrator tại thư mục project rồi chạy:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-vietnamese-tessdata.ps1
```

Sau đó kiểm tra toàn bộ cấu hình:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-tesseract.ps1
```

## 3. Cấu hình IntelliJ

Vào `Run → Edit Configurations → SmartSplitApplication → Environment variables`.

Xóa các biến Google cũ nếu có:

```text
GOOGLE_APPLICATION_CREDENTIALS
```

Đặt:

```text
OCR_ENABLED=true
OCR_PROVIDER=tesseract
TESSERACT_EXECUTABLE=C:/Program Files/Tesseract-OCR/tesseract.exe
TESSERACT_DATA_PATH=C:/Program Files/Tesseract-OCR/tessdata
TESSERACT_LANGUAGE=vie+eng
TESSERACT_PSM=6
TESSERACT_PREPROCESS=true
```

Hai biến đường dẫn trên là tùy chọn. Nếu `tesseract` đã có trong PATH, có thể chỉ cần `OCR_ENABLED=true`; ứng dụng cũng tự dò đường dẫn mặc định trong `Program Files`.

## 4. Chép patch và chạy

1. Dừng backend và frontend.
2. Chép nội dung patch vào thư mục project, chọn Replace.
3. Trong IntelliJ chọn `Maven → Reload All Maven Projects` để loại dependency Google cũ.
4. Chọn `Build → Rebuild Project` hoặc xóa `backend/target`.
5. Chạy lại backend và frontend.

Không xóa database. Bản này không có Flyway migration mới.

## 5. Kiểm thử

```text
Nhóm → Thêm khoản chi → Chọn ảnh → Quét hóa đơn
```

Kết quả thành công phải có:

```text
Nguồn: TESSERACT LOCAL
Trạng thái: OCR hoàn tất
```

Dùng ảnh chụp thẳng, đủ sáng, chữ lớn và không cắt mất dòng `TỔNG CỘNG` hoặc `TOTAL`.

Có thể kiểm tra Tesseract độc lập trước khi chạy SmartSplit:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-tesseract.ps1 -ImagePath "D:\hoa-don.jpg"
```

## 6. Cấu hình nâng cao

```text
TESSERACT_PSM=6                  # một khối văn bản thống nhất
TESSERACT_TIMEOUT_SECONDS=40     # thời gian tối đa
TESSERACT_PREPROCESS=true        # phóng ảnh + xám + tăng tương phản
TESSERACT_TARGET_WIDTH=1800      # chiều rộng ảnh xử lý
```

Nếu hóa đơn có bố cục rời rạc, thử `TESSERACT_PSM=3` rồi restart backend.

## 7. Lỗi thường gặp

### `tesseract is not recognized`

Đặt đầy đủ `TESSERACT_EXECUTABLE` hoặc thêm `C:\Program Files\Tesseract-OCR` vào PATH.

### `Error opening data file ... vie.traineddata`

Cài `vie.traineddata` và đặt đúng `TESSERACT_DATA_PATH`.

### OCR không tìm thấy chữ

Chụp lại ảnh thẳng, tăng ánh sáng, cắt bớt nền hoặc thử PSM 3.

### Backend vẫn nhắc Google Vision

Xóa `backend/target`, Reload Maven và Rebuild Project để loại class cũ.
