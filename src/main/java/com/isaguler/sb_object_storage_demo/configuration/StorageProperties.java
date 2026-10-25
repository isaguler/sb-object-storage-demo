package com.isaguler.sb_object_storage_demo.configuration;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint       address the application uses to reach the S3 API
 * @param publicEndpoint address put into presigned URLs; must be reachable by the client. Defaults to {@code endpoint}.
 */
@ConfigurationProperties("storage")
public record StorageProperties(
		URI endpoint,
		URI publicEndpoint,
		String region,
		String accessKey,
		String secretKey,
		String bucket,
		Duration presignExpiry) {

	public StorageProperties {
		if (publicEndpoint == null) {
			publicEndpoint = endpoint;
		}
	}

}
