package com.smartsplit.expense.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.dto.*;
import com.smartsplit.expense.entity.*;
import com.smartsplit.expense.repository.CategoryRepository;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExpenseService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final CurrentUserService currentUserService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            CategoryRepository categoryRepository,
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            CurrentUserService currentUserService
    ) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ExpenseResponse create(Long groupId, UpsertExpenseRequest request) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        requireActiveMembership(groupId, currentUser.getId());
        Map<Long, User> activeUsers = activeUsers(groupId);

        Expense expense = new Expense();
        expense.setGroup(group);
        expense.setCreatedBy(currentUser);
        applyRequest(expense, request, activeUsers);

        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(
            Long groupId,
            LocalDate from,
            LocalDate to,
            Long categoryId,
            String search
    ) {
        User currentUser = currentUserService.getRequiredUser();
        getActiveGroup(groupId);
        requireActiveMembership(groupId, currentUser.getId());
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(
                    "INVALID_DATE_RANGE",
                    "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc"
            );
        }

        String normalizedSearch = search == null ? null : search.trim().toLowerCase(Locale.ROOT);
        return expenseRepository
                .findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(
                        groupId,
                        ExpenseStatus.ACTIVE
                )
                .stream()
                .filter(expense -> from == null || !expense.getExpenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.getExpenseDate().isAfter(to))
                .filter(expense -> categoryId == null || (
                        expense.getCategory() != null
                                && expense.getCategory().getId().equals(categoryId)
                ))
                .filter(expense -> normalizedSearch == null || normalizedSearch.isBlank()
                        || expense.getTitle().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || (expense.getDescription() != null
                        && expense.getDescription().toLowerCase(Locale.ROOT).contains(normalizedSearch)))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(Long expenseId) {
        User currentUser = currentUserService.getRequiredUser();
        Expense expense = getActiveExpense(expenseId);
        requireActiveMembership(expense.getGroup().getId(), currentUser.getId());
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse update(Long expenseId, UpsertExpenseRequest request) {
        User currentUser = currentUserService.getRequiredUser();
        Expense expense = getActiveExpense(expenseId);
        GroupMember membership = requireActiveMembership(
                expense.getGroup().getId(),
                currentUser.getId()
        );
        requireEditor(expense, currentUser, membership);
        Map<Long, User> activeUsers = activeUsers(expense.getGroup().getId());

        // Xóa các dòng cũ trước khi thêm lại để tránh đụng unique(expense_id, user_id).
        expense.replacePayers(List.of());
        expense.replaceShares(List.of());
        expenseRepository.saveAndFlush(expense);

        applyRequest(expense, request, activeUsers);
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void delete(Long expenseId) {
        User currentUser = currentUserService.getRequiredUser();
        Expense expense = getActiveExpense(expenseId);
        GroupMember membership = requireActiveMembership(
                expense.getGroup().getId(),
                currentUser.getId()
        );
        requireEditor(expense, currentUser, membership);
        expense.setStatus(ExpenseStatus.DELETED);
        expenseRepository.save(expense);
    }

    private void applyRequest(
            Expense expense,
            UpsertExpenseRequest request,
            Map<Long, User> activeUsers
    ) {
        expense.setTitle(request.title().trim());
        expense.setDescription(normalizeNullable(request.description()));
        expense.setTotalAmount(request.totalAmount());
        expense.setExpenseDate(request.expenseDate());
        expense.setCategory(resolveCategory(request.categoryId()));
        expense.replacePayers(buildPayers(request, activeUsers));
        expense.replaceShares(buildShares(request, activeUsers));
    }

    private List<ExpensePayer> buildPayers(
            UpsertExpenseRequest request,
            Map<Long, User> activeUsers
    ) {
        requireUniqueUsers(
                request.payers().stream().map(ExpensePayerInput::userId).toList(),
                "Một thành viên không thể xuất hiện nhiều lần trong danh sách người trả"
        );

        long totalPaid = 0L;
        List<ExpensePayer> payers = new ArrayList<>();
        for (ExpensePayerInput input : request.payers()) {
            User user = requireActiveUser(input.userId(), activeUsers);
            totalPaid = safeAdd(totalPaid, input.amount());

            ExpensePayer payer = new ExpensePayer();
            payer.setUser(user);
            payer.setPaidAmount(input.amount());
            payers.add(payer);
        }
        if (totalPaid != request.totalAmount()) {
            throw new BusinessException(
                    "PAYER_TOTAL_MISMATCH",
                    "Tổng tiền những người đã trả phải bằng tổng khoản chi"
            );
        }
        return payers;
    }

    private List<ExpenseShare> buildShares(
            UpsertExpenseRequest request,
            Map<Long, User> activeUsers
    ) {
        ExpenseSplitInput split = request.split();
        requireUniqueUsers(
                split.participants().stream().map(ExpenseSplitParticipantInput::userId).toList(),
                "Một thành viên không thể xuất hiện nhiều lần trong danh sách chia tiền"
        );
        split.participants().forEach(input -> requireActiveUser(input.userId(), activeUsers));

        return switch (split.type()) {
            case EQUAL -> buildEqualShares(request.totalAmount(), split.participants(), activeUsers);
            case EXACT -> buildExactShares(request.totalAmount(), split.participants(), activeUsers);
            case PERCENTAGE -> buildPercentageShares(
                    request.totalAmount(),
                    split.participants(),
                    activeUsers
            );
        };
    }

    private List<ExpenseShare> buildEqualShares(
            long totalAmount,
            List<ExpenseSplitParticipantInput> participants,
            Map<Long, User> activeUsers
    ) {
        int count = participants.size();
        long baseAmount = totalAmount / count;
        long remainder = totalAmount % count;
        List<ExpenseShare> shares = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            long amount = baseAmount + (index < remainder ? 1L : 0L);
            shares.add(newShare(
                    activeUsers.get(participants.get(index).userId()),
                    amount,
                    null,
                    SplitType.EQUAL
            ));
        }
        return shares;
    }

    private List<ExpenseShare> buildExactShares(
            long totalAmount,
            List<ExpenseSplitParticipantInput> participants,
            Map<Long, User> activeUsers
    ) {
        long totalShares = 0L;
        List<ExpenseShare> shares = new ArrayList<>();
        for (ExpenseSplitParticipantInput input : participants) {
            if (input.amount() == null || input.amount() <= 0) {
                throw new BusinessException(
                        "INVALID_EXACT_SHARE",
                        "Số tiền của mỗi người phải lớn hơn 0"
                );
            }
            totalShares = safeAdd(totalShares, input.amount());
            shares.add(newShare(
                    activeUsers.get(input.userId()),
                    input.amount(),
                    null,
                    SplitType.EXACT
            ));
        }
        if (totalShares != totalAmount) {
            throw new BusinessException(
                    "SHARE_TOTAL_MISMATCH",
                    "Tổng phần tiền phải chịu phải bằng tổng khoản chi"
            );
        }
        return shares;
    }

    private List<ExpenseShare> buildPercentageShares(
            long totalAmount,
            List<ExpenseSplitParticipantInput> participants,
            Map<Long, User> activeUsers
    ) {
        List<BigDecimal> normalizedPercentages = new ArrayList<>();
        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (ExpenseSplitParticipantInput input : participants) {
            BigDecimal percentage = normalizePercentage(input.percentage());
            normalizedPercentages.add(percentage);
            totalPercentage = totalPercentage.add(percentage);
        }
        if (totalPercentage.compareTo(ONE_HUNDRED.setScale(4)) != 0) {
            throw new BusinessException(
                    "PERCENTAGE_TOTAL_MISMATCH",
                    "Tổng phần trăm phải bằng 100%"
            );
        }

        List<ExpenseShare> shares = new ArrayList<>();
        long assigned = 0L;
        for (int index = 0; index < participants.size(); index++) {
            ExpenseSplitParticipantInput input = participants.get(index);
            BigDecimal percentage = normalizedPercentages.get(index);
            long amount;
            if (index == participants.size() - 1) {
                amount = totalAmount - assigned;
            } else {
                amount = BigDecimal.valueOf(totalAmount)
                        .multiply(percentage)
                        .divide(ONE_HUNDRED, 0, RoundingMode.DOWN)
                        .longValueExact();
                assigned = safeAdd(assigned, amount);
            }
            shares.add(newShare(
                    activeUsers.get(input.userId()),
                    amount,
                    percentage,
                    SplitType.PERCENTAGE
            ));
        }
        return shares;
    }


    private BigDecimal normalizePercentage(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(
                    "INVALID_PERCENTAGE",
                    "Phần trăm của mỗi người phải lớn hơn 0"
            );
        }
        try {
            return value.setScale(4, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new BusinessException(
                    "INVALID_PERCENTAGE_SCALE",
                    "Phần trăm chỉ hỗ trợ tối đa 4 chữ số thập phân"
            );
        }
    }

    private ExpenseShare newShare(
            User user,
            long amount,
            BigDecimal percentage,
            SplitType splitType
    ) {
        ExpenseShare share = new ExpenseShare();
        share.setUser(user);
        share.setShareAmount(amount);
        share.setSharePercentage(percentage);
        share.setSplitType(splitType);
        return share;
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Expense getActiveExpense(Long expenseId) {
        return expenseRepository.findByIdAndStatus(expenseId, ExpenseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "EXPENSE_NOT_FOUND",
                        "Không tìm thấy khoản chi",
                        HttpStatus.NOT_FOUND
                ));
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
                .orElseThrow(() -> new BusinessException(
                        "FORBIDDEN",
                        "Bạn không có quyền truy cập nhóm này",
                        HttpStatus.FORBIDDEN
                ));
    }

    private Map<Long, User> activeUsers(Long groupId) {
        return memberRepository
                .findAllByGroup_IdAndStatusOrderByJoinedAtAsc(
                        groupId,
                        GroupMemberStatus.ACTIVE
                )
                .stream()
                .map(GroupMember::getUser)
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private User requireActiveUser(Long userId, Map<Long, User> activeUsers) {
        User user = activeUsers.get(userId);
        if (user == null) {
            throw new BusinessException(
                    "INVALID_GROUP_MEMBER",
                    "Người trả và người tham gia phải là thành viên đang hoạt động của nhóm"
            );
        }
        return user;
    }

    private void requireEditor(Expense expense, User currentUser, GroupMember membership) {
        boolean creator = expense.getCreatedBy().getId().equals(currentUser.getId());
        boolean manager = membership.getRole() == GroupRole.OWNER
                || membership.getRole() == GroupRole.ADMIN;
        if (!creator && !manager) {
            throw new BusinessException(
                    "FORBIDDEN",
                    "Chỉ người tạo khoản chi hoặc người quản lý nhóm mới có thể sửa/xóa",
                    HttpStatus.FORBIDDEN
            );
        }
    }

    private void requireUniqueUsers(List<Long> userIds, String message) {
        if (new HashSet<>(userIds).size() != userIds.size()) {
            throw new BusinessException("DUPLICATE_MEMBER", message);
        }
    }

    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_TOO_LARGE", "Số tiền vượt quá giới hạn hỗ trợ");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ExpenseResponse toResponse(Expense expense) {
        CategoryResponse category = expense.getCategory() == null
                ? null
                : new CategoryResponse(
                        expense.getCategory().getId(),
                        expense.getCategory().getName(),
                        expense.getCategory().getIcon()
                );
        SplitType splitType = expense.getShares().isEmpty()
                ? null
                : expense.getShares().getFirst().getSplitType();

        List<ExpensePersonAmountResponse> payers = expense.getPayers().stream()
                .sorted(Comparator.comparing(payer -> payer.getUser().getFullName()))
                .map(payer -> personAmount(payer.getUser(), payer.getPaidAmount(), null))
                .toList();
        List<ExpensePersonAmountResponse> shares = expense.getShares().stream()
                .sorted(Comparator.comparing(share -> share.getUser().getFullName()))
                .map(share -> personAmount(
                        share.getUser(),
                        share.getShareAmount(),
                        share.getSharePercentage()
                ))
                .toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getGroup().getId(),
                expense.getGroup().getName(),
                expense.getTitle(),
                expense.getDescription(),
                expense.getTotalAmount(),
                expense.getExpenseDate(),
                category,
                expense.getCreatedBy().getId(),
                expense.getCreatedBy().getFullName(),
                expense.getStatus(),
                splitType,
                expense.getVersion(),
                expense.getCreatedAt(),
                expense.getUpdatedAt(),
                payers,
                shares
        );
    }

    private ExpensePersonAmountResponse personAmount(
            User user,
            Long amount,
            BigDecimal percentage
    ) {
        return new ExpensePersonAmountResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatarUrl(),
                amount,
                percentage
        );
    }
}
