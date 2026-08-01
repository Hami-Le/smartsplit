package com.smartsplit.ocr.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.ocr.dto.ParseReceiptTextRequest;
import com.smartsplit.ocr.dto.ReceiptAttachmentResponse;
import com.smartsplit.ocr.dto.ReceiptScanResponse;
import com.smartsplit.ocr.service.ReceiptOcrService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class ReceiptOcrController {
    private final ReceiptOcrService receiptOcrService;

    public ReceiptOcrController(ReceiptOcrService receiptOcrService) {
        this.receiptOcrService = receiptOcrService;
    }

    @PostMapping(
            value = "/groups/{groupId}/receipt-scans",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<ReceiptScanResponse> scan(
            @PathVariable Long groupId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(receiptOcrService.scanImage(groupId, file));
    }

    @PostMapping("/groups/{groupId}/receipt-scans/parse-text")
    public ApiResponse<ReceiptScanResponse> parseText(
            @PathVariable Long groupId,
            @Valid @RequestBody ParseReceiptTextRequest request
    ) {
        return ApiResponse.ok(receiptOcrService.parseText(groupId, request.rawText()));
    }

    @GetMapping("/expenses/{expenseId}/attachments")
    public ApiResponse<List<ReceiptAttachmentResponse>> attachments(@PathVariable Long expenseId) {
        return ApiResponse.ok(receiptOcrService.listAttachments(expenseId));
    }

    @PostMapping("/expenses/{expenseId}/receipt-scans/{scanId}/attach")
    public ApiResponse<ReceiptScanResponse> attach(
            @PathVariable Long expenseId,
            @PathVariable Long scanId
    ) {
        return ApiResponse.ok(receiptOcrService.attachToExpense(scanId, expenseId));
    }

    @GetMapping("/receipt-scans/{scanId}/file")
    public ResponseEntity<?> file(@PathVariable Long scanId) {
        ReceiptOcrService.ReceiptFile file = receiptOcrService.getFile(scanId);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.resource());
    }
}
