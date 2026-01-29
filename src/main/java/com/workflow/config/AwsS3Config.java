package com.workflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

        @Bean
        public S3Client s3Client(Environment env) {
                return S3Client.builder()
                                .region(Region.of(env.getProperty("spring.cloud.aws.region.static")))
                                .credentialsProvider(
                                                StaticCredentialsProvider.create(
                                                                AwsBasicCredentials.create(
                                                                                env.getProperty("spring.cloud.aws.credentials.access-key"),
                                                                                env.getProperty("spring.cloud.aws.credentials.secret-key"))))
                                .build();
        }

        @Bean
        public S3Presigner s3Presigner(Environment env) {
                return S3Presigner.builder()
                                .region(Region.of(env.getProperty("spring.cloud.aws.region.static")))
                                .credentialsProvider(
                                                StaticCredentialsProvider.create(
                                                                AwsBasicCredentials.create(
                                                                                env.getProperty("spring.cloud.aws.credentials.access-key"),
                                                                                env.getProperty("spring.cloud.aws.credentials.secret-key"))))
                                .build();
        }
}
