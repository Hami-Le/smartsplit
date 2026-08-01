# SmartSplit Starter

SmartSplit là hệ thống quản lý và chia sẻ chi phí nhóm theo mô hình Client–Server. Bộ mã nguồn hiện đã hoàn thành **Iteration 5**: xác thực JWT, nhóm và thành viên, CRUD khoản chi, công nợ, dashboard, báo cáo và OCR hóa đơn có bước xác nhận dữ liệu.

## Công nghệ

- Backend: Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Flyway, Apache POI, OpenPDF, Tesseract OCR local
- Frontend: React 19, TypeScript, Vite 8
- Database: MySQL 8.4
- Đóng gói: Docker Compose, Nginx

## Khởi chạy bằng Docker

```bash
cp .env.example .env
docker compose up --build
```

- Web: http://localhost:8080
- Backend API: http://localhost:8081/api
- Health check: http://localhost:8081/api/health

## Chạy thủ công

### Database

```bash
docker compose up mysql -d
```

### Backend

Yêu cầu Java 21 và Maven 3.9+.

```bash
cd backend
mvn spring-boot:run
```

### Frontend

Yêu cầu Node.js 22.12+ hoặc Node.js 24 LTS.

```bash
cd frontend
npm install
npm run dev
```

## Tài khoản và API thử nghiệm

Đăng ký:

```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Ha Mi","email":"hami@example.com","password":"Password@123"}'
```

Đăng nhập:

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"hami@example.com","password":"Password@123"}'
```

Rút gọn công nợ, thay `TOKEN` bằng access token nhận được sau đăng nhập:

```bash
curl -X POST http://localhost:8081/api/balances/simplify \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"balances":[
    {"memberId":1,"memberName":"A","balance":-100000},
    {"memberId":2,"memberName":"B","balance":-200000},
    {"memberId":3,"memberName":"C","balance":150000},
    {"memberId":4,"memberName":"D","balance":150000}
  ]}'
```

## Quy ước tài chính

- Mọi số tiền được lưu bằng `BIGINT`, đơn vị đồng.
- Không dùng `float` hoặc `double` cho tiền.
- Số dư dương: cần được nhận tiền.
- Số dư âm: cần trả tiền.
- Tổng số dư trong một nhóm phải bằng 0.

## Tài liệu

- [Phạm vi MVP](docs/01-scope.md)
- [Kiến trúc](docs/02-architecture.md)
- [ERD](docs/03-erd.md)
- [API contract](docs/04-api-contract.md)
- [Roadmap](docs/05-roadmap.md)
- [Iteration 3: công nợ và thanh toán](docs/08-iteration-3.md)
- [Iteration 4: dashboard và báo cáo](docs/09-iteration-4.md)
- [Iteration 5: OCR hóa đơn](docs/10-iteration-5.md)
- [Iteration 5B: Tesseract OCR local](docs/11-iteration-5b-tesseract.md)

## Việc tiếp theo

Iteration 6 tập trung VietQR, thông báo/nhắc nợ, audit log, integration test và dữ liệu demo.

## Iteration 1 — Nhóm và thành viên

Iteration 1 đã bổ sung giao diện đăng ký/đăng nhập, quản lý JWT, CRUD nhóm, phân quyền thành viên và lời mời bằng token.

Khi cập nhật từ Iteration 0:

1. Dừng backend.
2. Chép các tệp Iteration 1 vào project.
3. Chạy lại `SmartSplitApplication`; Flyway sẽ tự chạy `V2__group_status.sql`.
4. Khởi động lại frontend bằng `npm run dev`.
5. Mở `http://localhost:5173/register` hoặc đăng nhập bằng tài khoản đã tạo trước đó.

Tài liệu chi tiết: `docs/06-iteration-1.md`.

## Iteration 2 — Khoản chi và chia tiền

Iteration 2 đã bổ sung danh sách khoản chi, tạo/xem/sửa/xóa, nhiều người thanh toán, chọn người tham gia và các kiểu chia `EQUAL`, `PERCENTAGE`, `EXACT`.

Khi cập nhật từ Iteration 1, xem `UPGRADE-ITERATION-2.md`. Tài liệu chi tiết: `docs/07-iteration-2.md`.


## Iteration 3 — Công nợ và thanh toán

Iteration 3 tính số dư theo công thức `đã trả - phải chịu + đã gửi - đã nhận`, tạo phương án chuyển khoản bằng Greedy và cho phép ghi nhận hoặc hủy mềm settlement.

Khi cập nhật từ Iteration 2, xem `UPGRADE-ITERATION-3.md`. Tài liệu chi tiết: `docs/08-iteration-3.md`.


## Iteration 4 — Dashboard và báo cáo

Iteration 4 bổ sung dashboard theo khoảng ngày, thống kê theo tháng/danh mục/thành viên, bộ lọc khoản chi và xuất Excel/PDF.

Khi cập nhật từ Iteration 3, xem `UPGRADE-ITERATION-4.md`. Tài liệu chi tiết: `docs/09-iteration-4.md`.


## Iteration 5 — OCR hóa đơn

Iteration 5/5B bổ sung upload ảnh, Tesseract OCR chạy local, bộ phân tích hóa đơn tiếng Việt, tự điền form, đính kèm ảnh và xử lý JWT hết hạn.

Khi cập nhật từ Iteration 4, xem `UPGRADE-ITERATION-5.md`. Tài liệu chi tiết: `docs/10-iteration-5.md`.
