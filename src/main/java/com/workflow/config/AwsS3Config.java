package com.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsS3Config {

    @Bean
    public S3Client s3Client(
            org.springframework.core.env.Environment env) {
        return S3Client.builder()
                .region(Region.of(env.getProperty("spring.cloud.aws.region.static")))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        env.getProperty("spring.cloud.aws.credentials.access-key"),
                                        env.getProperty("spring.cloud.aws.credentials.secret-key"))))
                .build();
    }
}
