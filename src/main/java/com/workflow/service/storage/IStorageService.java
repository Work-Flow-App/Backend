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
}
