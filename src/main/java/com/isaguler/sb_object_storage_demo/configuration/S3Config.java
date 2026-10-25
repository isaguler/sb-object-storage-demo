package com.isaguler.sb_object_storage_demo.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
class S3Config {

	// SeaweedFS needs path-style URLs (http://host:8333/bucket/key), not virtual-hosted ones (http://bucket.host:8333/key)

	@Bean
	S3Client s3Client(StorageProperties props) {
		return S3Client.builder()
				.endpointOverride(props.endpoint())
				.region(Region.of(props.region()))
				.credentialsProvider(credentials(props))
				.forcePathStyle(true)
				.build();
	}

	@Bean
	S3Presigner s3Presigner(StorageProperties props) {
		return S3Presigner.builder()
				.endpointOverride(props.publicEndpoint())
				.region(Region.of(props.region()))
				.credentialsProvider(credentials(props))
				.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
				.build();
	}

	private static StaticCredentialsProvider credentials(StorageProperties props) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey(), props.secretKey()));
	}

}
