package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import io.github.ielammari.bridge.exception.ApiException;

class LocalStorageServiceTest {

	private static final long MAX = 5 * 1024 * 1024;

	private StorageService storageOn(Path dir) {
		return new LocalStorageService(dir.toString(), MAX);
	}

	@Test
	void storesAPdfAndReturnsACandidateScopedPath(@TempDir Path dir) {
		StorageService storage = storageOn(dir);
		MockMultipartFile pdf = new MockMultipartFile("file", "cv.pdf", "application/pdf", "%PDF-1.4".getBytes());

		String path = storage.storeCv(42, pdf);

		assertThat(path).startsWith("42/").endsWith(".pdf");
		assertThat(Files.exists(dir.resolve(path))).isTrue();
	}

	@Test
	void rejectsANonPdfFile(@TempDir Path dir) {
		StorageService storage = storageOn(dir);
		MockMultipartFile notPdf = new MockMultipartFile("file", "cv.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document", "xxxx".getBytes());

		assertThatThrownBy(() -> storage.storeCv(1, notPdf))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "CV_NOT_PDF");
	}

	@Test
	void rejectsAFileOverTheCap(@TempDir Path dir) {
		StorageService storage = storageOn(dir);
		byte[] tooBig = new byte[(int) (MAX + 1)];
		MockMultipartFile big = new MockMultipartFile("file", "cv.pdf", "application/pdf", tooBig);

		assertThatThrownBy(() -> storage.storeCv(1, big))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "CV_TOO_LARGE");
	}

	@Test
	void rejectsAnEmptyUpload(@TempDir Path dir) {
		StorageService storage = storageOn(dir);
		MockMultipartFile empty = new MockMultipartFile("file", "cv.pdf", "application/pdf", new byte[0]);

		assertThatThrownBy(() -> storage.storeCv(1, empty))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "CV_MISSING");
	}

	@Test
	void loadingAMissingCvReportsNotFound(@TempDir Path dir) {
		StorageService storage = storageOn(dir);

		assertThatThrownBy(() -> storage.loadCv("1/does-not-exist.pdf"))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "RESOURCE_NOT_FOUND");
	}

}
