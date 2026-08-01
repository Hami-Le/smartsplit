package com.smartsplit.group.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.group.dto.*;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupInvitationRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Service
public class GroupService {
    private static final int INVITATION_VALID_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupInvitationRepository invitationRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            GroupInvitationRepository invitationRepository,
            CurrentUserService currentUserService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public GroupDetailResponse create(CreateGroupRequest request) {
        User currentUser = currentUserService.getRequiredUser();

        ExpenseGroup group = new ExpenseGroup();
        group.setName(request.name().trim());
        group.setDescription(normalizeNullable(request.description()));
        group.setDefaultCurrency(normalizeCurrency(request.defaultCurrency()));
        group.setCreatedBy(currentUser);
        ExpenseGroup savedGroup = groupRepository.save(group);

        GroupMember owner = new GroupMember();
        owner.setGroup(savedGroup);
        owner.setUser(currentUser);
        owner.setRole(GroupRole.OWNER);
        memberRepository.save(owner);

        return toDetail(savedGroup, owner);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listMine() {
        User currentUser = currentUserService.getRequiredUser();
        return memberRepository
                .findAllByUser_IdAndStatusAndGroup_StatusOrderByJoinedAtDesc(
                        currentUser.getId(),
                        GroupMemberStatus.ACTIVE,
                        GroupStatus.ACTIVE
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getById(Long groupId) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        GroupMember membership = requireActiveMembership(groupId, currentUser.getId());
        return toDetail(group, membership);
    }

    @Transactional
    public GroupDetailResponse update(Long groupId, UpdateGroupRequest request) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        GroupMember membership = requireManager(groupId, currentUser.getId());

        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BusinessException("GROUP_NAME_REQUIRED", "Tên nhóm không được để trống");
            }
            group.setName(name);
        }
        if (request.description() != null) {
            group.setDescription(normalizeNullable(request.description()));
        }
        if (request.defaultCurrency() != null) {
            group.setDefaultCurrency(normalizeCurrency(request.defaultCurrency()));
        }

        return toDetail(groupRepository.save(group), membership);
    }

    @Transactional
    public void archive(Long groupId) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        GroupMember membership = requireActiveMembership(groupId, currentUser.getId());
        if (membership.getRole() != GroupRole.OWNER) {
            throw forbidden("Chỉ chủ nhóm mới có thể lưu trữ nhóm");
        }
        group.setStatus(GroupStatus.ARCHIVED);
        groupRepository.save(group);
    }

    @Transactional
    public InvitationResponse invite(Long groupId, InviteMemberRequest request) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        requireManager(groupId, currentUser.getId());

        String email = normalizeEmail(request.email());
        if (email.equalsIgnoreCase(currentUser.getEmail())) {
            throw new BusinessException(
                    "CANNOT_INVITE_SELF",
                    "Bạn đã là thành viên của nhóm",
                    HttpStatus.CONFLICT
            );
        }

        boolean activeMemberExists = memberRepository
                .findAllByGroup_IdAndStatusOrderByJoinedAtAsc(groupId, GroupMemberStatus.ACTIVE)
                .stream()
                .anyMatch(member -> member.getUser().getEmail().equalsIgnoreCase(email));
        if (activeMemberExists) {
            throw new BusinessException(
                    "MEMBER_ALREADY_EXISTS",
                    "Email này đã là thành viên của nhóm",
                    HttpStatus.CONFLICT
            );
        }

        invitationRepository
                .findFirstByGroup_IdAndEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        groupId,
                        email,
                        InvitationStatus.PENDING
                )
                .ifPresent(existingInvitation -> {
                    if (existingInvitation.getExpiresAt().isAfter(LocalDateTime.now())) {
                        throw new BusinessException(
                                "INVITATION_ALREADY_PENDING",
                                "Email này đã có lời mời đang chờ xử lý",
                                HttpStatus.CONFLICT
                        );
                    }
                    existingInvitation.setStatus(InvitationStatus.EXPIRED);
                    invitationRepository.save(existingInvitation);
                });

        String rawToken = generateToken();
        GroupInvitation invitation = new GroupInvitation();
        invitation.setGroup(group);
        invitation.setEmail(email);
        invitation.setTokenHash(hashToken(rawToken));
        invitation.setInvitedBy(currentUser);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(INVITATION_VALID_DAYS));
        GroupInvitation saved = invitationRepository.save(invitation);

        return new InvitationResponse(
                saved.getId(),
                group.getId(),
                group.getName(),
                saved.getEmail(),
                saved.getStatus().name(),
                saved.getExpiresAt(),
                rawToken,
                "/invitations/" + rawToken
        );
    }

    @Transactional
    public AcceptInvitationResponse acceptInvitation(String token) {
        User currentUser = currentUserService.getRequiredUser();
        GroupInvitation invitation = invitationRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new BusinessException(
                        "INVITATION_NOT_FOUND",
                        "Lời mời không tồn tại hoặc không hợp lệ",
                        HttpStatus.NOT_FOUND
                ));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException(
                    "INVITATION_NOT_PENDING",
                    "Lời mời này đã được xử lý",
                    HttpStatus.CONFLICT
            );
        }
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(
                    "INVITATION_EXPIRED",
                    "Lời mời đã hết hạn",
                    HttpStatus.GONE
            );
        }
        if (!invitation.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw forbidden("Lời mời này được gửi cho một tài khoản email khác");
        }
        if (invitation.getGroup().getStatus() != GroupStatus.ACTIVE) {
            throw new BusinessException(
                    "GROUP_ARCHIVED",
                    "Nhóm đã được lưu trữ",
                    HttpStatus.GONE
            );
        }

        GroupMember membership = memberRepository
                .findByGroup_IdAndUser_Id(invitation.getGroup().getId(), currentUser.getId())
                .orElseGet(GroupMember::new);
        membership.setGroup(invitation.getGroup());
        membership.setUser(currentUser);
        membership.setRole(GroupRole.MEMBER);
        membership.setStatus(GroupMemberStatus.ACTIVE);
        membership.setJoinedAt(LocalDateTime.now());
        membership.setLeftAt(null);
        memberRepository.save(membership);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        return new AcceptInvitationResponse(
                invitation.getGroup().getId(),
                invitation.getGroup().getName(),
                GroupRole.MEMBER.name()
        );
    }

    @Transactional
    public GroupMemberResponse updateMemberRole(
            Long groupId,
            Long memberUserId,
            UpdateMemberRoleRequest request
    ) {
        User currentUser = currentUserService.getRequiredUser();
        getActiveGroup(groupId);
        GroupMember actor = requireActiveMembership(groupId, currentUser.getId());
        if (actor.getRole() != GroupRole.OWNER) {
            throw forbidden("Chỉ chủ nhóm mới có thể thay đổi vai trò thành viên");
        }
        if (request.role() == GroupRole.OWNER) {
            throw new BusinessException(
                    "OWNER_TRANSFER_NOT_SUPPORTED",
                    "Chưa hỗ trợ chuyển quyền chủ nhóm trong Iteration 1"
            );
        }

        GroupMember target = requireActiveMembership(groupId, memberUserId);
        if (target.getRole() == GroupRole.OWNER) {
            throw new BusinessException(
                    "CANNOT_CHANGE_OWNER_ROLE",
                    "Không thể thay đổi vai trò của chủ nhóm",
                    HttpStatus.CONFLICT
            );
        }
        target.setRole(request.role());
        return toMember(memberRepository.save(target));
    }

    @Transactional
    public void removeMember(Long groupId, Long memberUserId) {
        User currentUser = currentUserService.getRequiredUser();
        getActiveGroup(groupId);
        GroupMember actor = requireManager(groupId, currentUser.getId());
        GroupMember target = requireActiveMembership(groupId, memberUserId);

        if (target.getRole() == GroupRole.OWNER) {
            throw new BusinessException(
                    "CANNOT_REMOVE_OWNER",
                    "Không thể xóa chủ nhóm",
                    HttpStatus.CONFLICT
            );
        }
        if (target.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(
                    "CANNOT_REMOVE_SELF",
                    "Bạn không thể tự xóa mình bằng thao tác này",
                    HttpStatus.CONFLICT
            );
        }
        if (actor.getRole() == GroupRole.ADMIN && target.getRole() == GroupRole.ADMIN) {
            throw forbidden("Quản trị viên không thể xóa quản trị viên khác");
        }

        target.setStatus(GroupMemberStatus.REMOVED);
        target.setLeftAt(LocalDateTime.now());
        memberRepository.save(target);
    }

    private ExpenseGroup getActiveGroup(Long groupId) {
        return groupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "GROUP_NOT_FOUND",
                        "Không tìm thấy nhóm",
                        HttpStatus.NOT_FOUND
                ));
    }

    private GroupMember requireActiveMembership(Long groupId, Long userId) {
        return memberRepository.findByGroup_IdAndUser_IdAndStatus(
                        groupId,
                        userId,
                        GroupMemberStatus.ACTIVE
                )
                .orElseThrow(() -> forbidden("Bạn không có quyền truy cập nhóm này"));
    }

    private GroupMember requireManager(Long groupId, Long userId) {
        GroupMember membership = requireActiveMembership(groupId, userId);
        if (membership.getRole() != GroupRole.OWNER && membership.getRole() != GroupRole.ADMIN) {
            throw forbidden("Bạn không có quyền quản lý nhóm này");
        }
        return membership;
    }

    private GroupSummaryResponse toSummary(GroupMember membership) {
        ExpenseGroup group = membership.getGroup();
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getDefaultCurrency(),
                membership.getRole().name(),
                memberRepository.countByGroup_IdAndStatus(group.getId(), GroupMemberStatus.ACTIVE),
                group.getCreatedAt()
        );
    }

    private GroupDetailResponse toDetail(ExpenseGroup group, GroupMember currentMembership) {
        List<GroupMemberResponse> members = memberRepository
                .findAllByGroup_IdAndStatusOrderByJoinedAtAsc(group.getId(), GroupMemberStatus.ACTIVE)
                .stream()
                .map(this::toMember)
                .toList();
        return new GroupDetailResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getDefaultCurrency(),
                currentMembership.getRole().name(),
                group.getCreatedBy().getId(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                members
        );
    }

    private GroupMemberResponse toMember(GroupMember member) {
        User user = member.getUser();
        return new GroupMemberResponse(
                member.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                member.getRole().name(),
                member.getStatus().name(),
                member.getJoinedAt()
        );
    }

    private String normalizeCurrency(String currency) {
        return currency == null || currency.isBlank()
                ? "VND"
                : currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng", exception);
        }
    }

    private BusinessException forbidden(String message) {
        return new BusinessException("FORBIDDEN", message, HttpStatus.FORBIDDEN);
    }
}
