package com.isaguler.sb_object_storage_demo.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

@Component
public class ExcelGenerator {

	public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	public byte[] generate() {
		var out = new ByteArrayOutputStream();
		try (var workbook = new Workbook(out, "sb-object-storage-demo", "1.0")) {
			Worksheet sheet = workbook.newWorksheet("Sheet 1");
			sheet.value(0, 0, "test");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}

}
