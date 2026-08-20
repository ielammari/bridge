package io.github.ielammari.bridge.model;

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
 * One qualification on a candidate's academic path. Descriptive, not a gate: an
 * offer's requirement is checked against the single scalar level
 * ({@link Candidate#getDegree()}), which an ordered comparison needs.
 */
@Entity
@Table(name = "formation")
public class Education {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_formation")
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidat", nullable = false)
	private Candidate candidate;

	@Column(name = "intitule", nullable = false, length = 150)
	private String title;

	@Column(name = "etablissement", nullable = false, length = 150)
	private String institution;

	@Column(name = "domaine", length = 150)
	private String fieldOfStudy;

	@Column(name = "annee_debut", nullable = false)
	private Short startYear;

	// Null while the qualification is still being read for.
	@Column(name = "annee_fin")
	private Short endYear;

	protected Education() {
	}

	public Education(Candidate candidate, String title, String institution, String fieldOfStudy,
			Short startYear, Short endYear) {
		this.candidate = candidate;
		this.title = title;
		this.institution = institution;
		this.fieldOfStudy = fieldOfStudy;
		this.startYear = startYear;
		this.endYear = endYear;
	}

	public Integer getId() {
		return id;
	}

	public Candidate getCandidate() {
		return candidate;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getInstitution() {
		return institution;
	}

	public void setInstitution(String institution) {
		this.institution = institution;
	}

	public String getFieldOfStudy() {
		return fieldOfStudy;
	}

	public void setFieldOfStudy(String fieldOfStudy) {
		this.fieldOfStudy = fieldOfStudy;
	}

	public Short getStartYear() {
		return startYear;
	}

	public void setStartYear(Short startYear) {
		this.startYear = startYear;
	}

	public Short getEndYear() {
		return endYear;
	}

	public void setEndYear(Short endYear) {
		this.endYear = endYear;
	}

	/** Whether the qualification is still being read for. */
	public boolean isOngoing() {
		return endYear == null;
	}

}
