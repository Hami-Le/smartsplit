package com.smartsplit.expense.entity;

import com.smartsplit.group.entity.ExpenseGroup;
import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpensePayer> payers = new ArrayList<>();

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExpenseShare> shares = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void replacePayers(List<ExpensePayer> replacements) {
        payers.clear();
        replacements.forEach(payer -> {
            payer.setExpense(this);
            payers.add(payer);
        });
    }

    public void replaceShares(List<ExpenseShare> replacements) {
        shares.clear();
        replacements.forEach(share -> {
            share.setExpense(this);
            shares.add(share);
        });
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public void setGroup(ExpenseGroup group) { this.group = group; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public ExpenseStatus getStatus() { return status; }
    public void setStatus(ExpenseStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ExpensePayer> getPayers() { return payers; }
    public List<ExpenseShare> getShares() { return shares; }
}
