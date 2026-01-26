package com.workflow.service.storage;

import java.io.InputStream;

public interface IStorageService {

    String upload(
            String key,
            InputStream inputStream,
            long contentLength,
            String contentType);

    void delete(String key);
}
