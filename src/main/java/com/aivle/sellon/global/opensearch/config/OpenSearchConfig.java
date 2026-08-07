package com.aivle.sellon.global.opensearch.config;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

@Configuration
public class OpenSearchConfig {

    private static final String SIGNING_SERVICE_NAME = "es";

    @Value("${cloud.aws.opensearch.host}")
    private String host;

    @Value("${cloud.aws.opensearch.region}")
    private String region;

    @Value("${cloud.aws.opensearch.access-key}")
    private String accessKey;

    @Value("${cloud.aws.opensearch.secret-key}")
    private String secretKey;

    @Bean(destroyMethod = "close")
    public SdkHttpClient openSearchHttpClient() {
        return ApacheHttpClient.builder().build();
    }

    @Bean
    public OpenSearchClient openSearchClient(SdkHttpClient openSearchHttpClient) {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );

        OpenSearchTransport transport = new AwsSdk2Transport(
                openSearchHttpClient,
                host,
                SIGNING_SERVICE_NAME,
                Region.of(region),
                AwsSdk2TransportOptions.builder()
                        .setCredentials(credentialsProvider)
                        .build()
        );

        return new OpenSearchClient(transport);
    }
}
