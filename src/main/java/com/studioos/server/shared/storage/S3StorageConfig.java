package com.studioos.server.shared.storage;

import java.net.URI;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(name = "storage.s3.enabled", havingValue = "true")
public class S3StorageConfig {

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${storage.s3.access-key-id:}") String accessKeyId,
            @Value("${storage.s3.secret-access-key:}") String secretAccessKey) {

        if (accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }

        return DefaultCredentialsProvider.create();
    }

    @Bean
    public Region awsRegion(@Value("${storage.s3.region}") String region) {
        return Region.of(region);
    }

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            AwsCredentialsProvider awsCredentialsProvider,
            Region awsRegion,
            @Value("${storage.s3.endpoint-url:}") String endpointUrl,
            @Value("${storage.s3.path-style-access:false}") boolean pathStyleAccess) {

        var builder = S3Client.builder()
                .region(awsRegion)
                .credentialsProvider(awsCredentialsProvider);

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        if (pathStyleAccess) {
            builder.forcePathStyle(true);
        }

        return builder.build();
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            AwsCredentialsProvider awsCredentialsProvider,
            Region awsRegion,
            @Value("${storage.s3.endpoint-url:}") String endpointUrl,
            @Value("${storage.s3.path-style-access:false}") boolean pathStyleAccess) {

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(awsRegion)
                .credentialsProvider(awsCredentialsProvider);

        if (endpointUrl != null && !endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }

        if (pathStyleAccess) {
            builder.serviceConfiguration(
                    software.amazon.awssdk.services.s3.S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }

        return builder.build();
    }
}
