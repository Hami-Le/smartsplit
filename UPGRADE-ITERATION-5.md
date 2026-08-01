# Nâng cấp SmartSplit lên Iteration 5

Iteration 5 bổ sung upload ảnh hóa đơn, OCR bằng Tesseract local, bộ phân tích hóa đơn tiếng Việt, gợi ý danh mục, tự điền form và tự động đăng xuất khi JWT hết hạn.

## 1. Dừng và sao lưu

1. Dừng `SmartSplitApplication`.
2. Dừng frontend bằng `Ctrl + C`.
3. Sao lưu thư mục project hiện tại.
4. Giải nén patch Iteration 5 và chép đè vào project.
5. Không xóa database `smartsplit`.

## 2. Reload Maven

Iteration 5 thêm thư viện `google-cloud-vision`, vì vậy trong IntelliJ chọn:

```text
Maven → Reload All Maven Projects
```

Đợi các import `com.google.cloud.vision.v1.*` hết màu đỏ.

## 3. Chạy migration V6

Bật MySQL trong XAMPP và chạy lại backend. Flyway sẽ chạy:

```text
V6__receipt_ocr.sql
```

Trong `flyway_schema_history` cần có:

```text
version = 6
success = 1
```

Database sẽ có thêm bảng `receipt_scans`.

## 4. Chạy khi chưa cài Tesseract

Nếu chưa cài Tesseract, đặt:

```yaml
OCR_ENABLED=false
```

Ứng dụng vẫn chạy bình thường. Trong form **Thêm khoản chi**:

1. Mở phần `Ảnh khó đọc? Dán văn bản để kiểm thử bộ phân tích`.
2. Dán:

```text
HIGHLANDS COFFEE
23/07/2026
Coffee 89.000
Cake 45.000
VAT 13.400
TOTAL 147.400 VND
```

3. Nhấn **Phân tích văn bản**.
4. Kết quả dự kiến:
   - Cửa hàng: `HIGHLANDS COFFEE`.
   - Tổng tiền: `147.400 đ`.
   - Ngày: `2026-07-23`.
   - Danh mục: `Ăn uống`.
5. Nhấn **Áp dụng vào biểu mẫu**.

## 5. Bật OCR ảnh thật bằng Tesseract local

Làm theo tài liệu [UPGRADE-ITERATION-5B-TESSERACT.md](UPGRADE-ITERATION-5B-TESSERACT.md): cài Tesseract 5, dữ liệu `vie+eng`, sau đó đặt `OCR_ENABLED=true`. Không cần Google Cloud, API key hoặc billing.

## 6. Kiểm tra upload và OCR

1. Vào nhóm → **Thêm khoản chi**.
2. Chọn ảnh JPG, PNG hoặc WebP, tối đa 5 MB.
3. Nhấn **Quét hóa đơn**.
4. Kiểm tra cửa hàng, tổng tiền, ngày và danh mục.
5. Nhấn **Áp dụng vào biểu mẫu**.
6. Chọn người trả và người tham gia.
7. Lưu khoản chi.
8. Trang chi tiết phải có mục **Ảnh hóa đơn** và nút **Mở ảnh**.

Ảnh được lưu trong thư mục cấu hình bởi:

```text
OCR_STORAGE_DIRECTORY=./uploads/receipts
```

## 7. Kiểm tra phiên JWT hết hạn

Khi backend trả HTTP 401:

- Frontend xóa phiên cũ.
- Chuyển về `/login`.
- Hiện thông báo yêu cầu đăng nhập lại.

HTTP 403 do thiếu quyền nhóm không làm người dùng bị đăng xuất.

## 8. API mới

```http
POST /api/groups/{groupId}/receipt-scans
POST /api/groups/{groupId}/receipt-scans/parse-text
GET  /api/receipt-scans/{scanId}/file
POST /api/expenses/{expenseId}/receipt-scans/{scanId}/attach
GET  /api/expenses/{expenseId}/attachments
```
