# Nâng cấp SmartSplit lên Iteration 2

Iteration 2 bổ sung quản lý khoản chi và ba cách chia tiền: chia đều, theo phần trăm và theo số tiền.

## Trước khi cập nhật

1. Dừng `SmartSplitApplication`.
2. Dừng frontend bằng `Ctrl + C`.
3. Sao lưu thư mục project hiện tại.
4. Không xóa database `smartsplit`.

## Cập nhật bằng patch

Giải nén `smartsplit-iteration2-patch.zip`, chép toàn bộ nội dung bên trong vào thư mục `smartsplit-starter` và chọn ghi đè tệp cũ.

## Khởi động lại

1. Bật MySQL trong XAMPP.
2. Run `SmartSplitApplication` trong IntelliJ.
3. Flyway sẽ chạy `V3__expense_indexes.sql`.
4. Trong Terminal:

```powershell
cd frontend
npm run dev
```

Iteration 2 không thêm package npm mới, nên không cần chạy lại `npm install` nếu Iteration 1 đang hoạt động.

## Luồng kiểm thử

1. Mở một nhóm có ít nhất 2 thành viên.
2. Nhấn **Thêm khoản chi**.
3. Tạo khoản 100.000 đ chia đều cho 3 người; kết quả phải là 33.334 đ, 33.333 đ và 33.333 đ.
4. Tạo khoản chia theo phần trăm; tổng tỷ lệ phải đúng 100%.
5. Tạo khoản chia theo số tiền; tổng phần chia phải đúng tổng khoản chi.
6. Mở chi tiết, sửa rồi xóa khoản chi.
7. Kiểm tra các bảng `expenses`, `expense_payers`, `expense_shares` trong phpMyAdmin.

## Quy tắc quyền

- Mọi thành viên đang hoạt động có thể xem và tạo khoản chi.
- Người tạo khoản chi, `OWNER` hoặc `ADMIN` có thể sửa/xóa.
- Người ngoài nhóm không thể truy cập khoản chi bằng cách đổi ID.
