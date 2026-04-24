package com.workflow.service.storage;

import java.io.InputStream;

public interface IStorageService {

    void upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType);

    String generatePresignedUrl(String key);

    void delete(String key);

    /**
     * Returns a presigned URL for the given storage key, or {@code null} if the key is null.
     * Shared utility to avoid duplicating null-guard logic across services.
     */
    default String resolveFileUrl(String key) {
        return key == null ? null : generatePresignedUrl(key);
    }
}
