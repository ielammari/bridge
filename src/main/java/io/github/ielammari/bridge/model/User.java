package io.github.ielammari.bridge.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Root account holding identity and credentials.
 * <p>
 * Subclasses map to their own tables and are distinguished by the role column,
 * so the stored role and the concrete type cannot drift apart.
 */
@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User {

	// SERIAL in the schema, so int4: the identifier is an Integer, not a Long.
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_utilisateur")
	private Integer id;

	@Column(name = "email", nullable = false, unique = true, length = 150)
	private String email;

	@Column(name = "mot_de_passe", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "nom", nullable = false, length = 80)
	private String lastName;

	@Column(name = "prenom", nullable = false, length = 80)
	private String firstName;

	@Column(name = "telephone", length = 20)
	private String phone;

	@Column(name = "date_naissance")
	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "sexe", length = 20)
	private Gender gender;

	@Column(name = "ville", length = 100)
	private String city;

	@Column(name = "pays", length = 100)
	private String country;

	@Column(name = "date_inscription", nullable = false)
	private LocalDate registrationDate;

	protected User() {
	}

	/** For the roles that are provisioned rather than self registered. */
	protected User(String email, String passwordHash, String firstName, String lastName, String phone) {
		this(email, passwordHash, firstName, lastName, phone, null, null, null, null);
	}

	protected User(String email, String passwordHash, String firstName, String lastName, String phone,
			LocalDate birthDate, Gender gender, String city, String country) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.phone = phone;
		this.birthDate = birthDate;
		this.gender = gender;
		this.city = city;
		this.country = country;
		this.registrationDate = LocalDate.now();
	}

	/**
	 * Answered by every subtype, so callers never need to test the concrete
	 * class to learn which role an account holds.
	 */
	public abstract Role getRole();

	public Integer getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public LocalDate getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(LocalDate registrationDate) {
		this.registrationDate = registrationDate;
	}

}
