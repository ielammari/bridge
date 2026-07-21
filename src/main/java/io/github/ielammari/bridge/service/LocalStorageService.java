package io.github.ielammari.bridge.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.github.ielammari.bridge.exception.ApiException;

/**
 * Stores CV files on local disk under a configured root, one directory per
 * candidate. Only the relative path is meant to be persisted; the bytes stay on
 * disk.
 */
@Service
public class LocalStorageService implements StorageService {

	private static final String PDF_CONTENT_TYPE = "application/pdf";

	private final Path root;
	private final long maxBytes;

	public LocalStorageService(
			@Value("${bridge.storage.cv-dir}") String cvDir,
			@Value("${bridge.storage.max-file-size-bytes}") long maxBytes) {
		this.root = Path.of(cvDir).toAbsolutePath().normalize();
		this.maxBytes = maxBytes;
	}

	@Override
	public String storeCv(Integer candidateId, MultipartFile file) {
		validate(file);

		String relativePath = candidateId + "/" + UUID.randomUUID() + ".pdf";
		Path target = resolve(relativePath);

		try {
			Files.createDirectories(target.getParent());
			try (InputStream in = file.getInputStream()) {
				Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw ApiException.internal("CV_STORAGE_FAILED",
					"Le CV n'a pas pu être enregistré. Veuillez réessayer.");
		}

		return relativePath;
	}

	@Override
	public Resource loadCv(String relativePath) {
		Path target = resolve(relativePath);
		if (!Files.isReadable(target)) {
			throw ApiException.notFound("Ce CV est introuvable.");
		}
		try {
			return new UrlResource(target.toUri());
		} catch (IOException e) {
			throw ApiException.notFound("Ce CV est introuvable.");
		}
	}

	@Override
	public void deleteCv(String relativePath) {
		if (relativePath == null || relativePath.isBlank()) {
			return;
		}
		try {
			Files.deleteIfExists(resolve(relativePath));
		} catch (IOException e) {
			// A CV that cannot be deleted must not block the profile update that
			// is replacing it; the stale file is harmless.
		}
	}

	private void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw ApiException.badRequest("CV_MISSING", "Aucun fichier n'a été fourni.");
		}
		if (file.getSize() > maxBytes) {
			throw ApiException.badRequest("CV_TOO_LARGE",
					"Le CV dépasse la taille maximale de 5 Mo.");
		}
		if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
			throw ApiException.badRequest("CV_NOT_PDF", "Le CV doit être un fichier PDF.");
		}
	}

	/** Resolves a relative path under the root and refuses any escape from it. */
	private Path resolve(String relativePath) {
		Path resolved = root.resolve(relativePath).normalize();
		if (!resolved.startsWith(root)) {
			throw ApiException.badRequest("CV_INVALID_PATH", "Chemin de fichier invalide.");
		}
		return resolved;
	}

}
