package io.github.ielammari.bridge.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A candidate's application to an offer. Never deleted: rejection and discard
 * are status changes, so the full history stays visible on the tracking page.
 */
@Entity
@Table(name = "candidature")
public class Application {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_candidature")
	private Integer id;

	@Column(name = "date_candidature", nullable = false)
	private Instant applicationDate;

	// A snapshot of the CV path at apply time, kept independent of later profile
	// changes. Mandatory on an application.
	@Column(name = "cv_joint", nullable = false, length = 255)
	private String attachedCv;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 30)
	private ApplicationStatus status;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidat", nullable = false)
	private Candidate candidate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_offre", nullable = false)
	private JobOffer offer;

	protected Application() {
	}

	public Application(Candidate candidate, JobOffer offer, String attachedCv) {
		this.candidate = candidate;
		this.offer = offer;
		this.attachedCv = attachedCv;
		this.status = ApplicationStatus.NOUVELLE;
		this.applicationDate = Instant.now();
	}

	public Integer getId() {
		return id;
	}

	public Instant getApplicationDate() {
		return applicationDate;
	}

	public String getAttachedCv() {
		return attachedCv;
	}

	public ApplicationStatus getStatus() {
		return status;
	}

	public void setStatus(ApplicationStatus status) {
		this.status = status;
	}

	public Candidate getCandidate() {
		return candidate;
	}

	public JobOffer getOffer() {
		return offer;
	}

}
