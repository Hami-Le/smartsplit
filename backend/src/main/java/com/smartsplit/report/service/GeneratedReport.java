package com.smartsplit.report.service;

public record GeneratedReport(
        byte[] content,
        String contentType,
        String extension
) {}
