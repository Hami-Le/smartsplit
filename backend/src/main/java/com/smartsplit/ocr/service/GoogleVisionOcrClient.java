package com.smartsplit.ocr.service;

/**
 * Google Vision was removed from the local-development edition because it
 * requires Cloud Billing. The active provider is {@link TesseractOcrClient}.
 * This empty compatibility marker prevents stale source trees from retaining
 * imports from the former Google client library after applying a patch.
 */
@Deprecated(forRemoval = true)
public final class GoogleVisionOcrClient {
    private GoogleVisionOcrClient() {
    }
}
