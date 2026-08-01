package com.smartsplit.expense.entity;

import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(
        name = "expense_payers",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_expense_payers_expense_user",
                columnNames = {"expense_id", "user_id"}
        )
)
public class ExpensePayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount;

    public Long getId() { return id; }
    public Expense getExpense() { return expense; }
    public void setExpense(Expense expense) { this.expense = expense; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Long paidAmount) { this.paidAmount = paidAmount; }
}
