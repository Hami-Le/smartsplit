package com.smartsplit.settlement.entity;

import com.smartsplit.group.entity.ExpenseGroup;
import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
public class Settlement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private User payer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status = SettlementStatus.CONFIRMED;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (settledAt == null && status == SettlementStatus.CONFIRMED) {
            settledAt = createdAt;
        }
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public void setGroup(ExpenseGroup group) { this.group = group; }
    public User getPayer() { return payer; }
    public void setPayer(User payer) { this.payer = payer; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public SettlementStatus getStatus() { return status; }
    public void setStatus(SettlementStatus status) { this.status = status; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
