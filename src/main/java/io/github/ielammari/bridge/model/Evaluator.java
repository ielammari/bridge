package io.github.ielammari.bridge.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Any actor that can evaluate applications and declare availability slots.
 * Shared parent of the HR manager and the technical expert.
 */
@Entity
@Table(name = "evaluateur")
@PrimaryKeyJoinColumn(name = "id_evaluateur")
public abstract class Evaluator extends User {

	protected Evaluator() {
	}

	protected Evaluator(String email, String passwordHash, String firstName, String lastName, String phone) {
		super(email, passwordHash, firstName, lastName, phone);
	}

}
