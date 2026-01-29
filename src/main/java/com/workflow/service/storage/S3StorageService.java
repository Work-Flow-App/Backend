package com.workflow.service.storage;

import lombok.RequiredArgsConstructor;

import java.io.InputStream;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService implements IStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public void upload(String key, InputStream inputStream, long contentLength, String contentType) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request,
                RequestBody.fromInputStream(inputStream, contentLength));
    }

    @Override
    public String generatePresignedUrl(String key) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(p -> p
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest));

        return presignedRequest.url().toString();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(b -> b.bucket(bucket).key(key));
    }
}
