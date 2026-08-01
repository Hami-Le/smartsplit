# Iteration 5B — OCR hóa đơn bằng Tesseract local

## Mục tiêu

Loại bỏ phụ thuộc Cloud Billing khỏi luồng OCR. Tesseract chạy như một tiến trình hệ thống trên cùng máy với Spring Boot và trả văn bản UTF-8 cho bộ phân tích hóa đơn hiện có.

## Kiến trúc

```text
Ảnh hóa đơn
  → LocalReceiptStorage
  → ReceiptImagePreprocessor
  → TesseractOcrClient
  → văn bản vie+eng
  → ReceiptTextParser
  → cửa hàng / tổng tiền / ngày / danh mục
  → người dùng xác nhận
```

## Quyết định thiết kế

- Gọi executable bằng `ProcessBuilder`, không liên kết thư viện native vào JVM.
- Không cần thêm Maven dependency OCR.
- Dùng `--oem 1` cho LSTM và mặc định `--psm 6` cho một khối văn bản.
- Dùng `vie+eng` vì hóa đơn Việt Nam thường chứa cả tiếng Việt, tên sản phẩm và ký hiệu tiếng Anh.
- Kết quả OCR chỉ là gợi ý; người dùng phải xác nhận trước khi tạo khoản chi.
- Ảnh gốc được lưu để đính kèm; ảnh tiền xử lý chỉ là tệp tạm và được xóa sau OCR.

## Tính an toàn

- Không truyền lệnh qua shell; mỗi tham số được đưa riêng vào `ProcessBuilder`.
- Giới hạn loại file, kích thước file và timeout tiến trình.
- Đường dẫn ảnh lấy từ vùng lưu trữ nội bộ, không nhận trực tiếp từ request.
- Log provider được rút gọn trước khi trả về frontend.

## Đánh giá thực nghiệm đề xuất

Chuẩn bị 30–50 hóa đơn với các điều kiện ánh sáng khác nhau và đo:

- Tỷ lệ nhận đúng tổng tiền.
- Tỷ lệ nhận đúng ngày.
- Tỷ lệ nhận đúng tên cửa hàng.
- Thời gian OCR trung bình.
- Thời gian nhập liệu thủ công so với OCR.

Nên tách kết quả theo ảnh rõ, ảnh nghiêng, ảnh thiếu sáng và hóa đơn bị nhàu để báo cáo có giá trị thực nghiệm.
