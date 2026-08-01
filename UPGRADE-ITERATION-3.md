# Nâng cấp từ Iteration 2 lên Iteration 3

Iteration 3 bổ sung bảng công nợ thật từ dữ liệu khoản chi, thuật toán đề xuất chuyển khoản và lịch sử thanh toán.

## 1. Sao lưu

Dừng Spring Boot và frontend, sau đó sao lưu thư mục project hiện tại.

Không xóa database `smartsplit`.

## 2. Chép patch

Giải nén `smartsplit-iteration3-patch.zip`, chép toàn bộ nội dung bên trong vào thư mục `smartsplit-starter` và chọn ghi đè file trùng tên.

## 3. Chạy backend

Bật MySQL trong XAMPP rồi chạy lại `SmartSplitApplication`.

Flyway tự chạy:

```text
V4__settlement_balance_indexes.sql
```

Trong `flyway_schema_history` phải có version `4` với trạng thái thành công.

## 4. Chạy frontend

Iteration 3 không thêm package npm mới:

```powershell
cd frontend
npm run dev
```

Mở `http://localhost:5173`, vào một nhóm rồi nhấn **Công nợ**.

## 5. Luồng kiểm thử đề xuất

1. Nhóm có Hà, Minh và Lan.
2. Hà trả `600.000 đ`, chia đều cho ba người.
3. Bảng số dư phải là Hà `+400.000 đ`, Minh `-200.000 đ`, Lan `-200.000 đ`.
4. Ghi nhận Minh chuyển Hà `100.000 đ`.
5. Số dư mới phải là Hà `+300.000 đ`, Minh `-100.000 đ`, Lan `-200.000 đ`.
6. Hủy giao dịch vừa tạo; bảng số dư phải quay lại trạng thái trước bước 4.

## 6. API mới

```http
GET    /api/groups/{groupId}/balances
GET    /api/groups/{groupId}/suggested-transfers
GET    /api/groups/{groupId}/settlements
POST   /api/groups/{groupId}/settlements
DELETE /api/settlements/{settlementId}
```

Tiền được gửi dưới dạng số nguyên, đơn vị đồng.
