package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Runs the technical evaluation and scores the examined traits. */
@Entity
@Table(name = "expert_technique")
@DiscriminatorValue("EXPERT")
@PrimaryKeyJoinColumn(name = "id_expert")
public class TechnicalExpert extends Evaluator {

	@Column(name = "specialite", length = 80)
	private String specialty;

	protected TechnicalExpert() {
	}

	public TechnicalExpert(String email, String passwordHash, String firstName, String lastName, String phone) {
		super(email, passwordHash, firstName, lastName, phone);
	}

	@Override
	public Role getRole() {
		return Role.EXPERT;
	}

	public String getSpecialty() {
		return specialty;
	}

	public void setSpecialty(String specialty) {
		this.specialty = specialty;
	}

}
