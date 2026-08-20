package io.github.ielammari.bridge.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One CV a candidate keeps on file. Several may exist at once, and the one
 * chosen at apply time is copied onto the application, so replacing a document
 * later never changes what a recruiter already received.
 */
@Entity
@Table(name = "cv")
public class Cv {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_cv")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidat", nullable = false)
	private Candidate candidate;

	@Column(name = "intitule", nullable = false, length = 120)
	private String label;

	@Column(name = "chemin", nullable = false, length = 255)
	private String path;

	@Column(name = "date_depot", nullable = false)
	private Instant uploadedAt;

	protected Cv() {
	}

	public Cv(Candidate candidate, String label, String path) {
		this.candidate = candidate;
		this.label = label;
		this.path = path;
		this.uploadedAt = Instant.now();
	}

	public Integer getId() {
		return id;
	}

	public Candidate getCandidate() {
		return candidate;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getPath() {
		return path;
	}

	public Instant getUploadedAt() {
		return uploadedAt;
	}

}
