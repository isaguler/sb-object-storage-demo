package com.isaguler.sb_object_storage_demo.controller;

import java.time.Instant;
import java.util.UUID;

import com.isaguler.sb_object_storage_demo.service.ExcelGenerator;
import com.isaguler.sb_object_storage_demo.service.ObjectStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

	private final ExcelGenerator excelGenerator;

	private final ObjectStorageService storageService;

	public FileController(ExcelGenerator excelGenerator, ObjectStorageService storageService) {
		this.excelGenerator = excelGenerator;
		this.storageService = storageService;
	}

	@PostMapping("/excel")
	@ResponseStatus(HttpStatus.CREATED)
	public UploadResponse createExcel() {
		String key = "reports/" + UUID.randomUUID() + ".xlsx";
		storageService.upload(key, excelGenerator.generate(), ExcelGenerator.CONTENT_TYPE);
		return new UploadResponse(key);
	}

	@GetMapping("/presigned-url")
	public PresignedUrlResponse presignedUrl(@RequestParam String key) {
		var presigned = storageService.presignedGetUrl(key);
		return new PresignedUrlResponse(presigned.url().toString(), presigned.expiration());
	}

	public record UploadResponse(String key) {
	}

	public record PresignedUrlResponse(String url, Instant expiresAt) {
	}

}
