package com.smartsplit.report.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.report.dto.DashboardResponse;
import com.smartsplit.report.service.GeneratedReport;
import com.smartsplit.report.service.ReportExportService;
import com.smartsplit.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
public class ReportController {
    private final ReportService reportService;
    private final ReportExportService reportExportService;

    public ReportController(
            ReportService reportService,
            ReportExportService reportExportService
    ) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
    }

    @GetMapping("/groups/{groupId}/dashboard")
    public ApiResponse<DashboardResponse> dashboard(
            @PathVariable Long groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.ok(reportService.getDashboard(groupId, from, to));
    }

    @GetMapping("/groups/{groupId}/reports/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "xlsx") String format,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        GeneratedReport report = reportExportService.export(groupId, from, to, format);
        String fileName = "smartsplit-report-group-" + groupId + "." + report.extension();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentLength(report.content().length)
                .body(report.content());
    }
}
