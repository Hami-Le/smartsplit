# ERD

```mermaid
erDiagram
  USERS ||--o{ GROUPS : creates
  USERS ||--o{ GROUP_MEMBERS : joins
  GROUPS ||--o{ GROUP_MEMBERS : contains
  GROUPS ||--o{ GROUP_INVITATIONS : has
  GROUPS ||--o{ EXPENSES : contains
  USERS ||--o{ EXPENSES : creates
  EXPENSES ||--o{ EXPENSE_PAYERS : paid_by
  USERS ||--o{ EXPENSE_PAYERS : pays
  EXPENSES ||--o{ EXPENSE_SHARES : allocated_to
  USERS ||--o{ EXPENSE_SHARES : owes
  GROUPS ||--o{ SETTLEMENTS : records
  USERS ||--o{ SETTLEMENTS : payer
  USERS ||--o{ SETTLEMENTS : receiver
  EXPENSES ||--o{ ATTACHMENTS : has
  USERS ||--o{ NOTIFICATIONS : receives
  CATEGORIES ||--o{ EXPENSES : classifies

  USERS {
    bigint id PK
    varchar full_name
    varchar email UK
    varchar password_hash
    varchar avatar_url
    varchar phone
    varchar provider
    varchar role
    varchar status
    datetime created_at
    datetime updated_at
  }

  GROUPS {
    bigint id PK
    varchar name
    varchar description
    varchar default_currency
    bigint created_by FK
    datetime created_at
    datetime updated_at
  }

  GROUP_MEMBERS {
    bigint id PK
    bigint group_id FK
    bigint user_id FK
    varchar role
    varchar status
    datetime joined_at
  }

  EXPENSES {
    bigint id PK
    bigint group_id FK
    bigint category_id FK
    varchar title
    bigint total_amount
    date expense_date
    bigint created_by FK
    varchar status
    bigint version
  }

  EXPENSE_PAYERS {
    bigint id PK
    bigint expense_id FK
    bigint user_id FK
    bigint paid_amount
  }

  EXPENSE_SHARES {
    bigint id PK
    bigint expense_id FK
    bigint user_id FK
    bigint share_amount
    decimal share_percentage
    varchar split_type
  }

  SETTLEMENTS {
    bigint id PK
    bigint group_id FK
    bigint payer_id FK
    bigint receiver_id FK
    bigint amount
    varchar status
    datetime settled_at
  }
```


## ReceiptScans — Iteration 5

```text
id
group_id
uploaded_by
category_id
original_name
storage_path
content_type
file_size
provider
status
raw_text
merchant
total_amount
expense_date
confidence
message
created_at
expires_at
attached_at
```

`ReceiptScans` lưu phiên nhận dạng trước khi khoản chi được tạo. Sau khi xác nhận, ảnh được liên kết với khoản chi qua bảng `Attachments`.
