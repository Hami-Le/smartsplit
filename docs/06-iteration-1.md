# Iteration 1 — Nhóm và thành viên

## Chức năng đã hoàn thành

- Giao diện đăng ký và đăng nhập.
- Lưu JWT và thông tin người dùng trong `localStorage` để phục vụ giai đoạn phát triển.
- Bảo vệ các trang `/groups`, `/groups/new`, `/groups/{id}`.
- Tạo, xem, sửa và lưu trữ nhóm.
- Tự động thêm người tạo nhóm với vai trò `OWNER`.
- Danh sách thành viên và các vai trò `OWNER`, `ADMIN`, `MEMBER`.
- Chủ nhóm có thể thay đổi vai trò `ADMIN`/`MEMBER`.
- Chủ nhóm hoặc quản trị viên có thể xóa thành viên theo giới hạn phân quyền.
- Tạo link mời có token ngẫu nhiên; database chỉ lưu SHA-256 của token.
- Link mời hết hạn sau 7 ngày và chỉ tài khoản có đúng email mới chấp nhận được.
- Kiểm tra quyền truy cập theo membership; người ngoài nhóm nhận HTTP 403 khi đổi ID.
- Unit test cho việc tự gán `OWNER` và chặn người ngoài nhóm.

## Migration mới

`V2__group_status.sql` thêm trạng thái cho nhóm:

```text
ACTIVE
ARCHIVED
```

Sau khi backend khởi động lại, Flyway tự chạy migration V2. Không chạy file SQL bằng tay.

## Luồng demo

1. Đăng ký tài khoản A.
2. Tạo nhóm “Du lịch Đà Lạt”.
3. Kiểm tra A có vai trò `OWNER`.
4. Tạo link mời cho email của tài khoản B.
5. Mở trình duyệt ẩn danh, đăng ký/đăng nhập đúng email B.
6. Mở link và chấp nhận lời mời.
7. B xuất hiện trong danh sách thành viên với vai trò `MEMBER`.
8. A đổi B thành `ADMIN` rồi đổi lại `MEMBER`.
9. Đăng nhập tài khoản không thuộc nhóm và gọi `/api/groups/{id}` để kiểm tra HTTP 403.

## Cách chạy với XAMPP

1. Start MySQL trong XAMPP.
2. Run `SmartSplitApplication` trong IntelliJ.
3. Tại `frontend`, chạy `npm run dev`.
4. Mở `http://localhost:5173`.

Cấu hình mặc định trong `application.yml` sử dụng:

```text
Database: smartsplit
Username: root
Password: để trống
Port: 3306
```

Docker vẫn hoạt động vì `docker-compose.yml` truyền các biến môi trường riêng vào backend.
