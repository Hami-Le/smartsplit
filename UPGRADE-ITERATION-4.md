# Nâng cấp từ Iteration 3 lên Iteration 4

Iteration 4 bổ sung dashboard thống kê, bộ lọc khoản chi và xuất báo cáo Excel/PDF.

## 1. Sao lưu và dừng ứng dụng

- Dừng `SmartSplitApplication` trong IntelliJ.
- Nhấn `Ctrl + C` tại terminal đang chạy frontend.
- Sao lưu thư mục `smartsplit-starter` hiện tại.
- Không xóa database `smartsplit`.

## 2. Chép patch

Giải nén `smartsplit-iteration4-patch.zip`, chép toàn bộ nội dung bên trong vào thư mục project và chọn ghi đè các file trùng tên.

Bản patch có kèm lại migration V4 đã sửa để tránh lỗi tạo index trùng của bản Iteration 3 đầu tiên.

## 3. Tải dependency Maven mới

Iteration 4 thêm Apache POI để tạo Excel và OpenPDF để tạo PDF.

Trong IntelliJ:

1. Mở cửa sổ **Maven** ở cạnh phải.
2. Nhấn **Reload All Maven Projects**.
3. Chờ trạng thái tải dependency hoàn tất.

Hoặc chạy trong thư mục `backend`:

```powershell
mvn clean test
```

## 4. Chạy backend

Bật MySQL trong XAMPP rồi chạy lại `SmartSplitApplication`.

Flyway sẽ tự chạy:

```text
V5__report_indexes.sql
```

Trong `flyway_schema_history`, version `5` phải có `success = 1`.

Không cần chạy câu SQL sửa thủ công nếu version 4 hiện đã thành công.

## 5. Chạy frontend

Iteration 4 không thêm package npm mới:

```powershell
cd frontend
npm run dev
```

Mở `http://localhost:5173`, vào một nhóm rồi nhấn **Dashboard**.

## 6. Luồng kiểm thử

1. Chọn khoảng ngày có dữ liệu và nhấn **Áp dụng**.
2. Kiểm tra tổng chi, trung bình, khoản lớn nhất và công nợ.
3. Kiểm tra biểu đồ theo tháng, danh mục và bảng thành viên.
4. Nhấn **Xuất Excel**; file phải có 4 sheet: Tổng quan, Khoản chi, Công nợ, Thanh toán.
5. Nhấn **Xuất PDF**; nội dung tiếng Việt phải hiển thị đúng.
6. Quay về chi tiết nhóm, lọc khoản chi theo từ khóa, danh mục và ngày.

## 7. API mới

```http
GET /api/groups/{groupId}/dashboard?from=2026-01-01&to=2026-07-31
GET /api/groups/{groupId}/reports/export?format=xlsx&from=2026-01-01&to=2026-07-31
GET /api/groups/{groupId}/reports/export?format=pdf&from=2026-01-01&to=2026-07-31
```

Khoảng báo cáo tối đa là 36 tháng. Khi không gửi ngày, dashboard mặc định hiển thị 6 tháng gần nhất.
