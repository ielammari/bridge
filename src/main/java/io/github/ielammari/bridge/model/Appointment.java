package io.github.ielammari.bridge.model;

import java.time.LocalDate;
import java.time.LocalTime;

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
 * A scheduled interview (technical exam or HR interview). HR sets its date,
 * time and evaluator directly: the expert they picked for a technical exam, and
 * themselves for the HR interview. The calendar belongs to the evaluator, so
 * two of them may hold an interview at the same hour and neither may hold two.
 */
@Entity
@Table(name = "rendez_vous")
public class Appointment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_rendez_vous")
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private AppointmentType type;

	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false, length = 20)
	private AppointmentStatus status;

	@Column(name = "date_rendez_vous", nullable = false)
	private LocalDate date;

	@Column(name = "heure_rendez_vous", nullable = false)
	private LocalTime time;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidature", nullable = false)
	private Application application;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_evaluateur", nullable = false)
	private Evaluator evaluator;

	protected Appointment() {
	}

	public Appointment(Application application, AppointmentType type, LocalDate date, LocalTime time,
			Evaluator evaluator) {
		this.application = application;
		this.type = type;
		this.date = date;
		this.time = time;
		this.evaluator = evaluator;
		this.status = AppointmentStatus.PLANIFIE;
	}

	public Integer getId() {
		return id;
	}

	public AppointmentType getType() {
		return type;
	}

	public AppointmentStatus getStatus() {
		return status;
	}

	public void setStatus(AppointmentStatus status) {
		this.status = status;
	}

	public LocalDate getDate() {
		return date;
	}

	public LocalTime getTime() {
		return time;
	}

	public void reschedule(LocalDate date, LocalTime time) {
		this.date = date;
		this.time = time;
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	/** Hands the interview to another evaluator, who then holds that hour. */
	public void assignTo(Evaluator evaluator) {
		this.evaluator = evaluator;
	}

	public Application getApplication() {
		return application;
	}

}
