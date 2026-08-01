package com.smartsplit.expense.entity;

import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 80)
    private String icon;

    @Column(name = "is_system", nullable = false)
    private boolean system = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getIcon() { return icon; }
    public boolean isSystem() { return system; }
    public User getCreatedBy() { return createdBy; }
}
