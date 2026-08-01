package com.smartsplit.ocr.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.entity.Expense;
import com.smartsplit.expense.entity.ExpenseStatus;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.ocr.dto.ReceiptAttachmentResponse;
import com.smartsplit.ocr.dto.ReceiptScanResponse;
import com.smartsplit.ocr.entity.ReceiptScan;
import com.smartsplit.ocr.entity.ReceiptScanStatus;
import com.smartsplit.ocr.repository.ReceiptScanRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReceiptOcrService {
    private final ReceiptScanRepository scanRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final CurrentUserService currentUserService;
    private final LocalReceiptStorage storage;
    private final ReceiptOcrClient ocrClient;
    private final ReceiptTextParser textParser;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ReceiptOcrService(
            ReceiptScanRepository scanRepository,
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            ExpenseRepository expenseRepository,
            CurrentUserService currentUserService,
            LocalReceiptStorage storage,
            ReceiptOcrClient ocrClient,
            ReceiptTextParser textParser,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper
    ) {
        this.scanRepository = scanRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.currentUserService = currentUserService;
        this.storage = storage;
        this.ocrClient = ocrClient;
        this.textParser = textParser;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReceiptScanResponse scanImage(Long groupId, MultipartFile file) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = requireActiveGroup(groupId);
        requireActiveMembership(groupId, currentUser.getId());
        StoredReceiptFile stored = storage.store(file);

        ReceiptScan scan = new ReceiptScan();
        scan.setGroup(group);
        scan.setUploadedBy(currentUser);
        scan.setOriginalName(stored.originalName());
        scan.setStoragePath(stored.path().toString());
        scan.setContentType(stored.contentType());
        scan.setFileSize(stored.size());

        if (!ocrClient.isConfigured()) {
            scan.setProvider("TESSERACT_LOCAL");
            scan.setStatus(ReceiptScanStatus.MANUAL_REQUIRED);
            scan.setMessage(
                    "Ảnh đã được lưu nhưng Tesseract local chưa sẵn sàng. "
                            + ocrClient.configurationMessage()
                            + " Bạn vẫn có thể nhập thủ công hoặc dán văn bản để kiểm thử bộ phân tích."
            );
            return toResponse(scanRepository.save(scan));
        }

        try {
            OcrTextResult result = ocrClient.extractText(stored.path());
            scan.setProvider(result.provider());
            scan.setRawText(result.text());
            ParsedReceipt parsed = textParser.parse(result.text());
            applyParsed(scan, parsed);
            scan.setStatus(ReceiptScanStatus.COMPLETED);
            scan.setMessage(resultMessage(parsed, "OCR hoàn tất"));
        } catch (BusinessException exception) {
            scan.setProvider("TESSERACT_LOCAL");
            scan.setStatus(ReceiptScanStatus.FAILED);
            scan.setMessage(exception.getMessage());
        }
        return toResponse(scanRepository.save(scan));
    }

    @Transactional
    public ReceiptScanResponse parseText(Long groupId, String rawText) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = requireActiveGroup(groupId);
        requireActiveMembership(groupId, currentUser.getId());

        ReceiptScan scan = new ReceiptScan();
        scan.setGroup(group);
        scan.setUploadedBy(currentUser);
        scan.setProvider("MANUAL_TEXT");
        scan.setRawText(rawText.trim());
        ParsedReceipt parsed = textParser.parse(rawText);
        applyParsed(scan, parsed);
        scan.setStatus(ReceiptScanStatus.COMPLETED);
        scan.setMessage(resultMessage(parsed, "Đã phân tích văn bản"));
        return toResponse(scanRepository.save(scan));
    }

    @Transactional(readOnly = true)
    public ReceiptFile getFile(Long scanId) {
        User currentUser = currentUserService.getRequiredUser();
        ReceiptScan scan = requireScan(scanId);
        requireActiveMembership(scan.getGroup().getId(), currentUser.getId());
        Resource resource = storage.loadAsResource(scan.getStoragePath());
        return new ReceiptFile(
                resource,
                scan.getContentType() == null ? "application/octet-stream" : scan.getContentType(),
                scan.getOriginalName() == null ? "receipt" : scan.getOriginalName()
        );
    }

    @Transactional(readOnly = true)
    public List<ReceiptAttachmentResponse> listAttachments(Long expenseId) {
        User currentUser = currentUserService.getRequiredUser();
        Expense expense = expenseRepository.findByIdAndStatus(expenseId, ExpenseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "EXPENSE_NOT_FOUND",
                        "Không tìm thấy khoản chi",
                        HttpStatus.NOT_FOUND
                ));
        requireActiveMembership(expense.getGroup().getId(), currentUser.getId());
        return jdbcTemplate.query(
                "SELECT id, file_url, file_type, ocr_status, created_at "
                        + "FROM attachments WHERE expense_id = ? ORDER BY created_at DESC",
                (resultSet, rowNum) -> new ReceiptAttachmentResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("file_url"),
                        resultSet.getString("file_type"),
                        resultSet.getString("ocr_status"),
                        resultSet.getTimestamp("created_at").toLocalDateTime()
                ),
                expenseId
        );
    }

    @Transactional
    public ReceiptScanResponse attachToExpense(Long scanId, Long expenseId) {
        User currentUser = currentUserService.getRequiredUser();
        ReceiptScan scan = requireScan(scanId);
        Expense expense = expenseRepository.findByIdAndStatus(expenseId, ExpenseStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "EXPENSE_NOT_FOUND",
                        "Không tìm thấy khoản chi",
                        HttpStatus.NOT_FOUND
                ));

        if (!expense.getGroup().getId().equals(scan.getGroup().getId())) {
            throw new BusinessException(
                    "RECEIPT_GROUP_MISMATCH",
                    "Hóa đơn và khoản chi không thuộc cùng một nhóm"
            );
        }
        GroupMember membership = requireActiveMembership(scan.getGroup().getId(), currentUser.getId());
        boolean canEdit = expense.getCreatedBy().getId().equals(currentUser.getId())
                || membership.getRole() == GroupRole.OWNER
                || membership.getRole() == GroupRole.ADMIN;
        if (!canEdit) {
            throw new BusinessException(
                    "EXPENSE_EDIT_FORBIDDEN",
                    "Bạn không có quyền đính kèm hóa đơn vào khoản chi này",
                    HttpStatus.FORBIDDEN
            );
        }
        if (!scan.getUploadedBy().getId().equals(currentUser.getId())
                && membership.getRole() == GroupRole.MEMBER) {
            throw new BusinessException(
                    "RECEIPT_ATTACH_FORBIDDEN",
                    "Bạn không có quyền sử dụng phiên OCR này",
                    HttpStatus.FORBIDDEN
            );
        }
        if (scan.getStoragePath() == null || scan.getStoragePath().isBlank()) {
            return toResponse(scan);
        }
        if (scan.getStatus() == ReceiptScanStatus.ATTACHED) {
            return toResponse(scan);
        }

        String resultJson = serializeResult(scan);
        jdbcTemplate.update(
                "INSERT INTO attachments "
                        + "(expense_id, file_url, file_type, ocr_status, ocr_result, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                expenseId,
                "/api/receipt-scans/" + scan.getId() + "/file",
                scan.getContentType(),
                scan.getStatus().name(),
                resultJson,
                LocalDateTime.now()
        );
        scan.markAttached();
        scan.setMessage("Ảnh hóa đơn đã được đính kèm vào khoản chi.");
        return toResponse(scanRepository.save(scan));
    }


    private String resultMessage(ParsedReceipt parsed, String prefix) {
        if (parsed.totalAmount() == null) {
            return prefix + ". Chưa xác định chắc chắn tổng tiền; hãy nhập thủ công và xem văn bản OCR.";
        }
        if (parsed.confidence() == null || parsed.confidence().compareTo(new BigDecimal("0.7500")) < 0) {
            return prefix + ". Kết quả có độ tin cậy thấp; cần kiểm tra kỹ tên cửa hàng và số tiền trước khi áp dụng.";
        }
        return prefix + ". Hãy kiểm tra lại dữ liệu trước khi lưu khoản chi.";
    }

    private void applyParsed(ReceiptScan scan, ParsedReceipt parsed) {
        scan.setMerchant(parsed.merchant());
        scan.setTotalAmount(parsed.totalAmount());
        scan.setExpenseDate(parsed.expenseDate());
        scan.setCategory(parsed.category());
        scan.setConfidence(parsed.confidence());
    }

    private String serializeResult(ReceiptScan scan) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanId", scan.getId());
        result.put("provider", scan.getProvider());
        result.put("merchant", scan.getMerchant());
        result.put("totalAmount", scan.getTotalAmount());
        result.put("expenseDate", scan.getExpenseDate());
        result.put("categoryId", scan.getCategory() == null ? null : scan.getCategory().getId());
        result.put("categoryName", scan.getCategory() == null ? null : scan.getCategory().getName());
        result.put("confidence", scan.getConfidence());
        result.put("rawText", scan.getRawText());
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(
                    "OCR_RESULT_SERIALIZATION_FAILED",
                    "Không thể lưu kết quả OCR",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private ExpenseGroup requireActiveGroup(Long groupId) {
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
                        "GROUP_ACCESS_DENIED",
                        "Bạn không phải thành viên đang hoạt động của nhóm",
                        HttpStatus.FORBIDDEN
                ));
    }

    private ReceiptScan requireScan(Long scanId) {
        ReceiptScan scan = scanRepository.findById(scanId)
                .orElseThrow(() -> new BusinessException(
                        "RECEIPT_SCAN_NOT_FOUND",
                        "Không tìm thấy phiên OCR",
                        HttpStatus.NOT_FOUND
                ));
        if (scan.getExpiresAt().isBefore(LocalDateTime.now()) && scan.getStatus() != ReceiptScanStatus.ATTACHED) {
            throw new BusinessException(
                    "RECEIPT_SCAN_EXPIRED",
                    "Phiên OCR đã hết hạn",
                    HttpStatus.GONE
            );
        }
        return scan;
    }

    private ReceiptScanResponse toResponse(ReceiptScan scan) {
        BigDecimal confidence = scan.getConfidence();
        return new ReceiptScanResponse(
                scan.getId(),
                scan.getGroup().getId(),
                scan.getStatus().name(),
                scan.getProvider(),
                scan.getOriginalName(),
                scan.getStoragePath() != null && !scan.getStoragePath().isBlank(),
                scan.getMerchant(),
                scan.getTotalAmount(),
                scan.getExpenseDate(),
                scan.getCategory() == null ? null : scan.getCategory().getId(),
                scan.getCategory() == null ? null : scan.getCategory().getName(),
                confidence == null ? null : confidence.setScale(4, RoundingMode.HALF_UP).doubleValue(),
                scan.getRawText(),
                scan.getMessage(),
                scan.getCreatedAt()
        );
    }

    public record ReceiptFile(Resource resource, String contentType, String fileName) {
    }
}
