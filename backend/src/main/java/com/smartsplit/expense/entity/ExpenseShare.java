package com.smartsplit.expense.entity;

import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "expense_shares",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_shares_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
public class ExpenseShare {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "share_amount", nullable = false)
    private Long shareAmount;

    @Column(name = "share_percentage", precision = 7, scale = 4)
    private BigDecimal sharePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "split_type", nullable = false, length = 20)
    private SplitType splitType;

    public Long getId() { return id; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getShareAmount() { return shareAmount; }
    public void setShareAmount(Long shareAmount) { this.shareAmount = shareAmount; }
    public BigDecimal getSharePercentage() { return sharePercentage; }
    public void setSharePercentage(BigDecimal sharePercentage) { this.sharePercentage = sharePercentage; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
}
