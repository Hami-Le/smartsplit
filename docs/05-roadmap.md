# Roadmap triển khai

## Iteration 0 — Nền tảng

- [x] Monorepo frontend/backend.
- [x] MySQL và Flyway.
- [x] Đăng ký, đăng nhập JWT cơ bản.
- [x] Thuật toán rút gọn công nợ.
- [x] Unit test thuật toán.
- [x] Docker Compose.

## Iteration 1 — Nhóm và thành viên

- [x] CRUD nhóm.
- [x] Thành viên và vai trò.
- [x] Mời bằng link token.
- [x] Kiểm tra quyền theo nhóm.
- [x] Trang danh sách nhóm.
- [x] Trang chi tiết nhóm.

**Điều kiện hoàn thành:** người ngoài nhóm không thể xem hoặc sửa dữ liệu bằng cách đổi ID.

## Iteration 2 — Khoản chi

- [x] CRUD khoản chi.
- [x] Một hoặc nhiều người trả.
- [x] Chia đều.
- [x] Chia theo phần trăm.
- [x] Chia theo số tiền.
- [x] Xử lý phần dư khi chia.
- [x] Kiểm tra quyền và tổng tiền.
- [ ] Upload ảnh hóa đơn — chuyển sang Iteration 5 cùng OCR.

**Điều kiện hoàn thành:** tổng người trả và tổng phần chia luôn bằng tổng khoản chi.

## Iteration 3 — Công nợ

- [x] Tính balance từ dữ liệu gốc.
- [x] Giao dịch đề xuất.
- [x] Settlement một phần/toàn phần.
- [x] Lịch sử giao dịch và hủy mềm.
- [x] Test bất biến tổng số dư bằng 0.

## Iteration 4 — Dashboard và báo cáo

- [x] Chi tiêu theo tháng.
- [x] Chi tiêu theo danh mục.
- [x] Thành viên trả/chi nhiều nhất.
- [x] Lọc khoản chi theo từ khóa, danh mục và ngày.
- [x] Xuất Excel.
- [x] Xuất PDF.

**Điều kiện hoàn thành:** số liệu dashboard khớp dữ liệu khoản chi, người ngoài nhóm không xem hoặc tải báo cáo.

## Iteration 5 — AI/OCR

- [x] Upload và OCR hóa đơn.
- [x] Trích xuất tổng tiền, ngày, cửa hàng.
- [x] Gợi ý danh mục.
- [x] Màn hình xác nhận.
- [ ] Bộ dữ liệu kiểm thử 30–50 hóa đơn.
- [ ] Báo cáo precision/accuracy và thời gian nhập liệu.

## Iteration 6 — Hoàn thiện

- [ ] VietQR.
- [x] Export báo cáo — hoàn thành ở Iteration 4.
- [x] Sổ chi tiêu cá nhân.
- [x] Thống kê chi cá nhân theo danh mục.
- [x] Ngân sách cá nhân theo tháng.
- [ ] Audit log.
- [ ] Integration test.
- [ ] Docker production.
- [ ] Demo data và kịch bản bảo vệ.
