package com.smartsplit.group.entity;

import com.smartsplit.user.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "group_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_members_group_user",
                columnNames = {"group_id", "user_id"}
        )
)
public class GroupMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GroupMemberStatus status = GroupMemberStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public ExpenseGroup getGroup() { return group; }
    public void setGroup(ExpenseGroup group) { this.group = group; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public GroupRole getRole() { return role; }
    public void setRole(GroupRole role) { this.role = role; }
    public GroupMemberStatus getStatus() { return status; }
    public void setStatus(GroupMemberStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
}
