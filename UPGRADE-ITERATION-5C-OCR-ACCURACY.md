# Iteration 5C — Cải thiện độ chính xác OCR hóa đơn

Bản vá này sửa lỗi bộ phân tích số tiền có thể ghép hai số độc lập, ví dụ `8.100 118.390`, thành `8.100.118.390`. Đồng thời bổ sung nhận dạng tốt hơn cho ảnh chụp biên lai ngân hàng và ví điện tử.

## Thay đổi chính

- Không còn cho phép regex tiền ghép tùy ý qua dấu cách.
- Chấm điểm ứng viên số tiền theo nhãn `Tổng tiền`, `Số tiền giao dịch`, `Amount`, `Thành tiền` và các dòng kế tiếp.
- Bỏ qua số tài khoản, mã giao dịch, mã tham chiếu, số dư, mã hóa đơn và chuỗi định danh dài.
- Nhận tên từ nhãn `Cửa hàng`, `Merchant`, `Người nhận`, `Đối tác`, `Nhà cung cấp` trước khi dùng dòng đầu làm phương án dự phòng.
- Hạn chế nhận nhầm tên nền tảng/POS như MoMo, Timo, DanTriSoft, KiotViet, Sapo thành cửa hàng.
- Tesseract chạy nhiều Page Segmentation Mode: cấu hình hiện tại, PSM 11 cho ảnh chụp ứng dụng/ngân hàng và PSM 4 cho hóa đơn dạng cột. Hệ thống chọn kết quả văn bản có chất lượng cao hơn.
- Độ tin cậy không còn tự động lên 95% chỉ vì tìm đủ bốn trường.
- Không cho áp dụng vào biểu mẫu khi chưa nhận được tổng tiền.
- Hiển thị cảnh báo khi kết quả dưới 75%.

## Cập nhật

1. Dừng backend và frontend.
2. Sao lưu project.
3. Giải nén patch và chép đè vào thư mục `smartsplit-starter`.
4. Trong IntelliJ chọn `Build → Rebuild Project`.
5. Không xóa database; không có migration Flyway mới.
6. Chạy lại backend và frontend.

## Biến môi trường

Giữ các biến hiện tại và thêm:

```text
TESSERACT_MULTI_PASS=true
```

Nếu IntelliJ dùng một ô duy nhất:

```text
OCR_ENABLED=true;OCR_PROVIDER=tesseract;TESSERACT_EXECUTABLE=C:/Program Files/Tesseract-OCR/tesseract.exe;TESSERACT_DATA_PATH=C:/Program Files/Tesseract-OCR/tessdata;TESSERACT_LANGUAGE=vie+eng;TESSERACT_PSM=6;TESSERACT_MULTI_PASS=true;TESSERACT_PREPROCESS=true
```

Multi-pass xử lý chậm hơn một lần quét đơn, nhưng phù hợp cho đồ án và tăng khả năng đọc ảnh chụp từ nhiều ứng dụng.

## Kiểm thử đề xuất

### Hóa đơn bán hàng

```text
TỔNG TIỀN
8.100 118.390
```

Hệ thống phải chọn `118.390 đ`, không được ghép thành `8.100.118.390 đ`.

### Timo/ngân hàng

```text
Số tiền giao dịch
-150.000 VND
Người nhận
NGUYEN VAN AN
Mã giao dịch 202607291234567890
```

Kết quả phải là `150.000 đ`, tên `NGUYEN VAN AN`; không được chọn mã giao dịch.

## Giới hạn còn lại

Tesseract nhận dạng ký tự, còn việc hiểu nghiệp vụ hóa đơn dựa trên luật và từ khóa. Không thể bảo đảm đúng 100% với mọi mẫu ngân hàng, ảnh mờ hoặc giao diện mới. Người dùng luôn phải xác nhận trước khi lưu. Để tinh chỉnh theo dữ liệu thật, nên chuẩn bị bộ 20–50 ảnh đã che số tài khoản và đo tỷ lệ đúng cho tên, ngày và số tiền.
