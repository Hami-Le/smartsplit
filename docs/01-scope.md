# Phạm vi đồ án SmartSplit

## Mục tiêu

Xây dựng ứng dụng web Client–Server giúp một nhóm ghi nhận khoản chi, phân bổ nghĩa vụ, tính số dư công nợ và đề xuất các giao dịch thanh toán rút gọn.

## MVP bắt buộc

1. Đăng ký, đăng nhập và JWT.
2. Quản lý nhóm và thành viên.
3. Phân quyền OWNER, ADMIN, MEMBER.
4. CRUD khoản chi.
5. Chia đều, theo phần trăm và theo số tiền.
6. Chọn người tham gia khoản chi.
7. Tính số dư của từng thành viên.
8. Ghi nhận thanh toán công nợ.
9. Đề xuất danh sách chuyển khoản rút gọn.
10. Lịch sử và dashboard cơ bản.

## Nâng cao ưu tiên

1. OCR hóa đơn và bước xác nhận dữ liệu.
2. VietQR theo giao dịch đề xuất.
3. Nhắc nợ qua email.
4. Xuất báo cáo Excel hoặc PDF.

## Ngoài phạm vi MVP

- Chat nhóm thời gian thực.
- Đồng bộ tài khoản ngân hàng.
- Đa tiền tệ có tỷ giá thời gian thực.
- Ứng dụng mobile native.
- AI chatbot tổng quát.

## Actor

- Khách chưa đăng nhập.
- Thành viên.
- Quản trị viên nhóm.
- Chủ nhóm.
- Quản trị viên hệ thống, chỉ bổ sung khi môn học yêu cầu.

## Quy tắc nghiệp vụ trọng yếu

- Tổng `ExpenseShare.share_amount` phải bằng `Expense.total_amount`.
- Tổng `ExpensePayer.paid_amount` phải bằng `Expense.total_amount`.
- Thành viên tham gia khoản chi phải thuộc nhóm tại thời điểm tạo khoản chi.
- Không được xóa thành viên đang còn số dư khác 0; chỉ chuyển sang trạng thái rời nhóm.
- Sửa khoản chi phải cập nhật lại toàn bộ số dư một cách nguyên tử.
- Một giao dịch thanh toán không được vượt quá nghĩa vụ hiện tại nếu chưa có quyền xác nhận đặc biệt.
