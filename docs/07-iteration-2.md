# Iteration 2 — Khoản chi và chia tiền

## Chức năng đã triển khai

- Danh sách khoản chi trong trang chi tiết nhóm.
- Tìm kiếm theo tên/ghi chú và lọc theo danh mục trên giao diện.
- Tạo, xem, sửa và xóa mềm khoản chi.
- Một hoặc nhiều thành viên cùng thanh toán.
- Chỉ chọn những thành viên thực sự tham gia.
- Chia đều, chia theo phần trăm và chia theo số tiền cụ thể.
- Phân bổ phần dư theo thứ tự người tham gia, bảo đảm không thất thoát 1 đồng.
- Kiểm tra quyền theo nhóm và quyền sửa/xóa.
- Unit test cho chia đều và các trường hợp tổng không hợp lệ.

## Bất biến nghiệp vụ

Với mỗi khoản chi hợp lệ:

```text
Tổng số tiền những người đã trả = Tổng khoản chi
Tổng phần tiền các thành viên phải chịu = Tổng khoản chi
```

Tiền được lưu bằng `BIGINT`, đơn vị đồng. Backend không dùng `float` hoặc `double` cho tiền.

## Cách chia đều

Ví dụ 100.000 đ chia cho 3 người:

```text
Người 1: 33.334 đ
Người 2: 33.333 đ
Người 3: 33.333 đ
Tổng:    100.000 đ
```

Phần dư được cộng từng đồng cho các thành viên đầu tiên theo thứ tự gửi lên API.

## Cách chia theo phần trăm

- Mỗi tỷ lệ phải lớn hơn 0.
- Tổng tỷ lệ phải chính xác bằng 100%.
- Hỗ trợ tối đa 4 chữ số thập phân.
- Thành viên cuối cùng nhận phần chênh lệch làm tròn để tổng tiền luôn khớp.

## Cách chia theo số tiền

Mỗi thành viên nhập phần tiền cụ thể. Tổng các phần phải bằng tổng khoản chi.

## API

```text
GET    /api/categories
POST   /api/groups/{groupId}/expenses
GET    /api/groups/{groupId}/expenses
GET    /api/expenses/{expenseId}
PUT    /api/expenses/{expenseId}
DELETE /api/expenses/{expenseId}
```

Danh sách hỗ trợ các query parameter tùy chọn: `from`, `to`, `categoryId`, `search`.

## Chưa thuộc Iteration 2

- Upload ảnh và OCR hóa đơn.
- Tính số dư công nợ của nhóm.
- Ghi nhận thanh toán.
- QR chuyển khoản.

Các phần trên được thực hiện ở Iteration 3 và Iteration 5.
