# Cập nhật từ Iteration 0 lên Iteration 1

## Cách an toàn nhất

1. Dừng Spring Boot và Vite.
2. Sao lưu thư mục project hiện tại.
3. Giải nén bản Iteration 1 và chép đè vào thư mục `smartsplit-starter`.
4. Không xóa database `smartsplit`.
5. Run lại `SmartSplitApplication`.
6. Kiểm tra log có migration `V2__group_status.sql` thành công.
7. Chạy frontend:

```powershell
cd frontend
npm run dev
```

Không cần chạy lại `npm install` vì Iteration 1 không thêm thư viện frontend mới.

## Kiểm tra nhanh

- Mở `http://localhost:5173/login` và đăng nhập bằng tài khoản đã tạo.
- Vào “Nhóm của tôi” → “Tạo nhóm”.
- Kiểm tra phpMyAdmin:
  - `expense_groups` có bản ghi mới và `status = ACTIVE`.
  - `group_members` có người tạo với `role = OWNER`.

## Trường hợp backend không khởi động

Mở bảng `flyway_schema_history`. Phiên bản `2` phải có `success = 1`.

Nếu XAMPP dùng cổng khác 3306, sửa `spring.datasource.url` trong `application.yml`.
