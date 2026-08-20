package io.github.ielammari.bridge.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores and retrieves candidate CV files. An interface because the local disk
 * implementation can be swapped for object storage without touching callers.
 */
public interface StorageService {

	/** Stores the file and returns the relative path to record in the database. */
	String storeCv(Integer candidateId, MultipartFile file);

	/** Loads a previously stored file by its relative path. */
	Resource loadCv(String relativePath);

	/** Removes a stored file, ignoring a path that no longer exists. */
	void deleteCv(String relativePath);

}
