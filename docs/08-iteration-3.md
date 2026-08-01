# Iteration 3 — Công nợ và thanh toán

## Mục tiêu

Tính số dư từ dữ liệu gốc thay vì lưu trực tiếp quan hệ “A nợ B”.

Với từng thành viên:

```text
balance = paidAmount - shareAmount + sentAmount - receivedAmount
```

- `balance > 0`: thành viên cần nhận tiền.
- `balance < 0`: thành viên cần trả tiền.
- Tổng balance của nhóm luôn phải bằng `0`.

## Nguồn dữ liệu

- `expense_payers`: số tiền từng người đã ứng.
- `expense_shares`: phần chi phí từng người phải chịu.
- `settlements` có trạng thái `CONFIRMED`: tiền đã chuyển giữa thành viên.
- Khoản chi `DELETED` và settlement `CANCELLED` không tham gia phép tính.

## Thuật toán đề xuất

1. Tách thành viên thành danh sách người nợ và người cần nhận.
2. Dùng priority queue lấy số tiền lớn nhất của mỗi phía.
3. Tạo giao dịch bằng giá trị nhỏ hơn giữa hai phía.
4. Cập nhật phần còn lại và lặp đến khi cân bằng.

Độ phức tạp triển khai: `O(n log n + k log n)`, với `k` là số giao dịch được tạo.

Thuật toán tạo phương án gọn và hiệu quả, nhưng báo cáo không nên khẳng định Greedy luôn đạt số giao dịch tối thiểu tuyệt đối cho mọi bộ dữ liệu.

## Quy tắc settlement

- Người trả và người nhận phải thuộc nhóm.
- Thành viên thường chỉ được ghi nhận giao dịch liên quan đến mình.
- OWNER/ADMIN có thể ghi nhận thay thành viên khác.
- Người trả phải đang có số dư âm.
- Người nhận phải đang có số dư dương.
- Số tiền không được vượt quá phần có thể bù trừ giữa hai người.
- Hủy settlement là hủy mềm; bản ghi vẫn được giữ để đối chiếu.

## Giao diện

Trang `/groups/{groupId}/balances` có:

- Tổng khoản chi, tổng đã thanh toán và số tiền còn phải chuyển.
- Bảng số dư chi tiết của từng thành viên.
- Danh sách chuyển khoản đề xuất.
- Form ghi nhận thanh toán một phần hoặc toàn phần.
- Lịch sử settlement và chức năng hủy giao dịch do mình tạo.

## Kiểm thử bất biến

- Tổng số dư của nhóm bằng `0`.
- Settlement xác nhận làm giảm đồng thời nợ của payer và khoản cần nhận của receiver.
- Settlement bị hủy không ảnh hưởng số dư.
- Không thể thanh toán vượt quá công nợ hiện tại.
- Người ngoài nhóm không thể xem bảng công nợ.
