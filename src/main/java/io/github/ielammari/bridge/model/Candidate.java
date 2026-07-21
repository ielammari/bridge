package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Job seeker. Holds the CV path and the scalar degree used by the matching gate. */
@Entity
@Table(name = "candidat")
@DiscriminatorValue("CANDIDAT")
@PrimaryKeyJoinColumn(name = "id_candidat")
public class Candidate extends User {

	@Column(name = "chemin_cv", length = 255)
	private String cvPath;

	@Column(name = "niveau_experience", length = 40)
	private String experienceLevel;

	@Column(name = "diplome", length = 80)
	private String degree;

	protected Candidate() {
	}

	public Candidate(String email, String passwordHash, String firstName, String lastName, String phone) {
		super(email, passwordHash, firstName, lastName, phone);
	}

	@Override
	public Role getRole() {
		return Role.CANDIDAT;
	}

	public String getCvPath() {
		return cvPath;
	}

	public void setCvPath(String cvPath) {
		this.cvPath = cvPath;
	}

	public String getExperienceLevel() {
		return experienceLevel;
	}

	public void setExperienceLevel(String experienceLevel) {
		this.experienceLevel = experienceLevel;
	}

	public String getDegree() {
		return degree;
	}

	public void setDegree(String degree) {
		this.degree = degree;
	}

}
