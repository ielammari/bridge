package io.github.ielammari.bridge.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Configures offers, screens applications, runs the final interview. */
@Entity
@Table(name = "responsable_rh")
@DiscriminatorValue("RH")
@PrimaryKeyJoinColumn(name = "id_rh")
public class HRManager extends Evaluator {

	@Column(name = "departement", length = 80)
	private String department;

	protected HRManager() {
	}

	public HRManager(String email, String passwordHash, String firstName, String lastName, String phone) {
		super(email, passwordHash, firstName, lastName, phone);
	}

	@Override
	public Role getRole() {
		return Role.RH;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

}
