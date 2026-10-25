package com.isaguler.sb_object_storage_demo.service;

import com.isaguler.sb_object_storage_demo.configuration.StorageProperties;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class ObjectStorageService {

	private final S3Client s3Client;

	private final S3Presigner s3Presigner;

	private final StorageProperties props;

	public ObjectStorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties props) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.props = props;
	}

	public void upload(String key, byte[] content, String contentType) {
		String fileName = key.substring(key.lastIndexOf('/') + 1);
		s3Client.putObject(request -> request
				.bucket(props.bucket())
				.key(key)
				.contentType(contentType)
				.contentDisposition(ContentDisposition.attachment().filename(fileName).build().toString()),
				RequestBody.fromBytes(content));
	}

	public PresignedGetObjectRequest presignedGetUrl(String key) {
		return s3Presigner.presignGetObject(request -> request
				.signatureDuration(props.presignExpiry())
				.getObjectRequest(get -> get.bucket(props.bucket()).key(key)));
	}

}
