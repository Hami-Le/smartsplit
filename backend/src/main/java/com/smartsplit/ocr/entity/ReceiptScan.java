package com.smartsplit.ocr.entity;

import com.smartsplit.expense.entity.Category;
import com.smartsplit.group.entity.ExpenseGroup;
import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipt_scans")
public class ReceiptScan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "storage_path", length = 700)
    private String storagePath;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(nullable = false, length = 40)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceiptScanStatus status;

    @Lob
    @Column(name = "raw_text", columnDefinition = "LONGTEXT")
    private String rawText;

    @Column(length = 180)
    private String merchant;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "expense_date")
    private LocalDate expenseDate;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attached_at")
    private LocalDateTime attachedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (expiresAt == null) expiresAt = now.plusDays(7);
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public void setGroup(ExpenseGroup group) { this.group = group; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public ReceiptScanStatus getStatus() { return status; }
    public void setStatus(ReceiptScanStatus status) { this.status = status; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getMerchant() { return merchant; }
    public void setMerchant(String merchant) { this.merchant = merchant; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getAttachedAt() { return attachedAt; }
    public void markAttached() {
        status = ReceiptScanStatus.ATTACHED;
        attachedAt = LocalDateTime.now();
    }
}
