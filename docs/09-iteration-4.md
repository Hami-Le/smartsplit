# Iteration 4 — Dashboard, tìm kiếm và xuất báo cáo

## Mục tiêu

Tổng hợp dữ liệu chi tiêu đã có thành thông tin dễ theo dõi và có thể xuất ra tài liệu phục vụ báo cáo đồ án.

## Dashboard

Trang `/groups/{groupId}/dashboard` hiển thị:

- Tổng chi trong khoảng thời gian.
- Số khoản chi và giá trị trung bình.
- Khoản chi lớn nhất.
- Tổng settlement đã xác nhận trong kỳ.
- Công nợ hiện tại của cả nhóm.
- Xu hướng chi tiêu theo tháng.
- Cơ cấu chi tiêu theo danh mục.
- Số tiền từng thành viên đã trả và phải chịu.
- Sáu khoản chi gần nhất.

Khoảng mặc định là 6 tháng gần nhất và tối đa 36 tháng.

## Công thức thống kê

```text
totalExpense = tổng expenses ACTIVE trong khoảng ngày
averageExpense = totalExpense / expenseCount
outstandingAmount = tổng các balance dương hiện tại
totalSettled = tổng settlements CONFIRMED trong khoảng ngày
categoryPercentage = categoryAmount / totalExpense × 100
memberSharePercentage = memberShare / totalExpense × 100
```

`outstandingAmount` là trạng thái công nợ hiện tại của nhóm, còn các chỉ số chi tiêu và settlement được lọc theo khoảng ngày.

## Bộ lọc khoản chi

Trang chi tiết nhóm hỗ trợ kết hợp:

- Từ khóa trong tiêu đề hoặc ghi chú.
- Danh mục.
- Ngày bắt đầu.
- Ngày kết thúc.

Bộ lọc được xử lý ở backend, vì vậy có thể mở rộng sang phân trang và truy vấn database trực tiếp ở giai đoạn tối ưu.

## Xuất Excel

File `.xlsx` gồm:

1. **Tổng quan**: chỉ số chính và thống kê danh mục.
2. **Khoản chi**: ngày, danh mục, số tiền, người trả và người tham gia.
3. **Công nợ**: đã trả, phải chịu, đã gửi, đã nhận và số dư.
4. **Thanh toán**: lịch sử settlement trong kỳ.

Tiền được ghi dưới dạng số để người dùng tiếp tục tính toán trong Excel.

## Xuất PDF

PDF ở khổ A4 ngang, gồm:

- Tiêu đề nhóm và khoảng báo cáo.
- Các chỉ số chính.
- Bảng danh mục.
- Bảng thành viên.
- Danh sách khoản chi.

Backend ưu tiên các font Unicode có sẵn trên Windows/Linux để hiển thị tiếng Việt. Trên Windows, hệ thống thử Arial rồi Tahoma.

## Phân quyền

- Chỉ thành viên đang hoạt động trong nhóm mới xem dashboard hoặc tải báo cáo.
- Người ngoài nhóm nhận HTTP `403`.
- Báo cáo chỉ lấy dữ liệu của đúng `groupId` đã được kiểm tra quyền.

## Migration V5

Migration thêm index:

```sql
CREATE INDEX idx_expenses_group_status_category_date
    ON expenses(group_id, status, category_id, expense_date);
```

Index phục vụ các truy vấn theo nhóm, trạng thái, danh mục và thời gian.

## Kiểm thử quan trọng

- Khoảng ngày không hợp lệ phải bị từ chối.
- Khoảng trên 36 tháng phải bị từ chối.
- Nhóm không có dữ liệu vẫn trả dashboard với các giá trị bằng 0.
- Tổng tiền theo danh mục bằng tổng chi.
- Tổng phần phải chịu theo thành viên bằng tổng chi.
- Người ngoài nhóm không tải được báo cáo.
- File Excel và PDF tải về không rỗng và mở được.
