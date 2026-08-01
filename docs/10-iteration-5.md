# Iteration 5 — Receipt OCR và tự động nhập liệu

## Mục tiêu

Iteration 5 đưa yếu tố AI/OCR vào luồng nghiệp vụ chính thay vì chỉ gọi API để tạo lời khuyên chung. Người dùng có thể tải ảnh hóa đơn, nhận dạng văn bản, trích xuất trường dữ liệu và xác nhận trước khi tạo khoản chi.

## Luồng xử lý

```text
Ảnh hóa đơn
   ↓
Kiểm tra loại và kích thước tệp
   ↓
Lưu file cục bộ
   ↓
Tesseract 5 local (`vie+eng`, LSTM)
   ↓
ReceiptTextParser
   ├── cửa hàng
   ├── tổng tiền
   ├── ngày
   └── danh mục gợi ý
   ↓
Người dùng kiểm tra và áp dụng
   ↓
Tạo khoản chi
   ↓
Đính kèm ảnh và kết quả OCR
```

Khi Tesseract chưa được cài hoặc chưa có dữ liệu ngôn ngữ, hệ thống không bị lỗi khởi động. Người dùng vẫn có thể dán văn bản OCR để kiểm thử riêng bộ phân tích.

## Kiểm soát tệp

- Định dạng: JPG, PNG, WebP.
- Kích thước tối đa mặc định: 5 MB.
- Tên tệp lưu trên server được thay bằng UUID.
- Không tin cậy đường dẫn/tên tệp từ client.
- Chỉ thành viên nhóm mới quét hoặc đọc ảnh.
- Chỉ người tạo khoản chi, OWNER hoặc ADMIN được đính kèm ảnh.

## Trích xuất dữ liệu

### Tổng tiền

Parser ưu tiên số tiền trên các dòng chứa từ khóa như:

```text
tổng
tổng cộng
thanh toán
total
grand total
amount due
```

Nếu không có dòng tổng, parser dùng số tiền hợp lý lớn nhất làm phương án dự phòng. Kết quả luôn cần người dùng xác nhận.

### Ngày

Hỗ trợ các dạng:

```text
dd/MM/yyyy
dd-MM-yyyy
yyyy-MM-dd
```

### Danh mục

Phân loại dựa trên từ khóa nghiệp vụ hiện có:

- Ăn uống.
- Di chuyển.
- Khách sạn.
- Mua sắm.
- Giải trí.
- Khác.

Đây là baseline có thể dùng để so sánh với một mô hình phân loại học máy ở giai đoạn thực nghiệm.

## Dữ liệu

Bảng `receipt_scans` lưu:

- Nhóm và người upload.
- Tệp nguồn.
- Provider OCR.
- Trạng thái.
- Văn bản nhận dạng.
- Merchant, total, date, category.
- Confidence.
- Thời điểm hết hạn và đính kèm.

Khi người dùng lưu khoản chi, tệp được ghi vào `attachments` và liên kết với `expenses`.

## Trạng thái

```text
COMPLETED       OCR/parser hoàn tất
MANUAL_REQUIRED OCR chưa cấu hình, cần nhập thủ công
FAILED          provider OCR báo lỗi
ATTACHED        ảnh đã gắn vào khoản chi
```

## API

```http
POST /api/groups/{groupId}/receipt-scans
Content-Type: multipart/form-data

POST /api/groups/{groupId}/receipt-scans/parse-text
Content-Type: application/json

GET /api/receipt-scans/{scanId}/file

POST /api/expenses/{expenseId}/receipt-scans/{scanId}/attach

GET /api/expenses/{expenseId}/attachments
```

## Đánh giá cho báo cáo

Nên thu thập 30–50 ảnh hóa đơn và lập bảng gồm:

```text
receipt_id
image_quality
merchant_expected
merchant_actual
total_expected
total_actual
date_expected
date_actual
category_expected
category_actual
processing_time_ms
manual_entry_time_ms
```

Chỉ số đề xuất:

- Accuracy nhận dạng tổng tiền.
- Accuracy nhận dạng ngày.
- Accuracy merchant.
- Accuracy phân loại danh mục.
- Thời gian OCR so với nhập tay.
- Tỷ lệ người dùng phải sửa từng trường.

## Sửa phiên hết hạn

Iteration 5 chuẩn hóa phản hồi bảo mật:

- HTTP 401 + `AUTHENTICATION_REQUIRED`: token thiếu, hết hạn hoặc không hợp lệ.
- HTTP 403 + `ACCESS_DENIED`: đã đăng nhập nhưng không đủ quyền.

Frontend chỉ tự đăng xuất với HTTP 401, tránh đăng xuất nhầm khi người dùng bị từ chối quyền trong một nhóm.
