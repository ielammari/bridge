package io.github.ielammari.bridge.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	// Stored as the enum name in the free text diplome column, validated in the
	// service rather than by a database constraint.
	@Enumerated(EnumType.STRING)
	@Column(name = "diplome", length = 80)
	private Degree degree;

	protected Candidate() {
	}

	public Candidate(String email, String passwordHash, String firstName, String lastName, String phone,
			LocalDate birthDate, Gender gender, String city, String country) {
		super(email, passwordHash, firstName, lastName, phone, birthDate, gender, city, country);
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

	public Degree getDegree() {
		return degree;
	}

	public void setDegree(Degree degree) {
		this.degree = degree;
	}

}
