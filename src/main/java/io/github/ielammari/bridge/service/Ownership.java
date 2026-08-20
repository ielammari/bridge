package io.github.ielammari.bridge.service;

import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.JobOffer;

/**
 * What a recruiter may act on: the offers they published and every application
 * that arrived through them. A refusal is a not found rather than a forbidden,
 * so the id space cannot be walked to learn what other recruiters run.
 */
final class Ownership {

	private Ownership() {
	}

	static JobOffer requireOwnOffer(JobOffer offer, Integer hrId) {
		if (!isPublishedBy(offer, hrId)) {
			throw ApiException.notFound("Cette offre est introuvable.");
		}
		return offer;
	}

	static Application requireOwnApplication(Application application, Integer hrId) {
		if (!isPublishedBy(application.getOffer(), hrId)) {
			throw ApiException.notFound("Cette candidature est introuvable.");
		}
		return application;
	}

	private static boolean isPublishedBy(JobOffer offer, Integer hrId) {
		return offer != null && offer.getPublisher() != null
				&& offer.getPublisher().getId().equals(hrId);
	}

}
