# API contract ban đầu

Base URL: `/api`

## Response thành công

```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-07-28T22:00:00+07:00"
}
```

## Response lỗi

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "email": "Email không đúng định dạng"
  },
  "timestamp": "2026-07-28T22:00:00+07:00"
}
```

## Authentication

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/auth/register` | Đăng ký |
| POST | `/auth/login` | Đăng nhập |
| POST | `/auth/refresh` | Đổi access token |
| POST | `/auth/logout` | Thu hồi refresh token |

## Groups

| Method | Endpoint | Quyền |
|---|---|---|
| POST | `/groups` | Authenticated |
| GET | `/groups` | Thành viên |
| GET | `/groups/{groupId}` | Thành viên nhóm |
| PATCH | `/groups/{groupId}` | OWNER/ADMIN |
| DELETE | `/groups/{groupId}` | OWNER; lưu trữ nhóm |
| POST | `/groups/{groupId}/invitations` | OWNER/ADMIN |
| POST | `/invitations/{token}/accept` | Người được mời |
| PATCH | `/groups/{groupId}/members/{userId}/role` | OWNER |
| DELETE | `/groups/{groupId}/members/{userId}` | OWNER/ADMIN |

## Expenses

| Method | Endpoint | Quyền |
|---|---|---|
| GET | `/categories` | Người đã đăng nhập |
| POST | `/groups/{groupId}/expenses` | Thành viên |
| GET | `/groups/{groupId}/expenses` | Thành viên |
| GET | `/expenses/{expenseId}` | Thành viên nhóm |
| PUT | `/expenses/{expenseId}` | Người tạo hoặc OWNER/ADMIN |
| DELETE | `/expenses/{expenseId}` | Người tạo hoặc OWNER/ADMIN |

## Balance và settlement

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/groups/{groupId}/balances` | Số dư ròng |
| GET | `/groups/{groupId}/suggested-transfers` | Giao dịch đề xuất |
| GET | `/groups/{groupId}/settlements` | Lịch sử thanh toán |
| POST | `/groups/{groupId}/settlements` | Ghi nhận và xác nhận thanh toán |
| DELETE | `/settlements/{id}` | Hủy mềm giao dịch |
| POST | `/balances/simplify` | API thử thuật toán Iteration 0 |

## Sổ chi tiêu cá nhân

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/personal-finance?month=yyyy-MM` | Tổng chi, ngân sách, thống kê danh mục và giao dịch trong tháng |
| POST | `/personal-finance/expenses` | Ghi khoản chi cá nhân |
| PUT | `/personal-finance/expenses/{expenseId}` | Sửa khoản chi thuộc tài khoản hiện tại |
| DELETE | `/personal-finance/expenses/{expenseId}` | Xóa khoản chi thuộc tài khoản hiện tại |
| PUT | `/personal-finance/budgets/{yyyy-MM}` | Tạo hoặc cập nhật ngân sách tháng |
| DELETE | `/personal-finance/budgets/{yyyy-MM}` | Xóa ngân sách tháng |

Client không gửi `userId`; backend luôn lấy chủ dữ liệu từ JWT.

```json
{
  "title": "Ăn trưa",
  "amount": 50000,
  "expenseDate": "2026-08-02",
  "categoryId": 1,
  "note": "Cơm văn phòng"
}
```


## Dashboard và báo cáo

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/groups/{groupId}/dashboard` | Tổng hợp dashboard theo khoảng ngày |
| GET | `/groups/{groupId}/reports/export?format=xlsx` | Tải báo cáo Excel |
| GET | `/groups/{groupId}/reports/export?format=pdf` | Tải báo cáo PDF |

Các endpoint nhận tùy chọn `from` và `to` theo định dạng `yyyy-MM-dd`. Khoảng tối đa là 36 tháng.

Ví dụ:

```http
GET /api/groups/10/dashboard?from=2026-01-01&to=2026-07-31
GET /api/groups/10/reports/export?format=xlsx&from=2026-01-01&to=2026-07-31
```

## Request khoản chi mẫu

```json
{
  "title": "Quán BBQ",
  "description": "Ăn tối",
  "totalAmount": 2300000,
  "expenseDate": "2026-07-23",
  "categoryId": 1,
  "payers": [
    {"userId": 10, "amount": 2300000}
  ],
  "split": {
    "type": "EXACT",
    "participants": [
      {"userId": 10, "amount": 800000},
      {"userId": 11, "amount": 700000},
      {"userId": 12, "amount": 800000}
    ]
  }
}
```

`split.type` nhận `EQUAL`, `PERCENTAGE` hoặc `EXACT`. Với `PERCENTAGE`, mỗi participant gửi trường `percentage`; với `EQUAL` chỉ cần `userId`.


## Request settlement mẫu

```json
{
  "payerId": 11,
  "receiverId": 10,
  "amount": 150000,
  "note": "Đã chuyển khoản ngân hàng",
  "settledAt": "2026-07-29T09:30:00"
}
```


## Receipt OCR — Iteration 5

```http
POST /api/groups/{groupId}/receipt-scans
POST /api/groups/{groupId}/receipt-scans/parse-text
GET  /api/receipt-scans/{scanId}/file
POST /api/expenses/{expenseId}/receipt-scans/{scanId}/attach
GET  /api/expenses/{expenseId}/attachments
```

Ảnh chỉ nhận JPG/PNG/WebP, tối đa 5 MB. Kết quả OCR là gợi ý và phải được người dùng xác nhận trước khi tạo khoản chi.
