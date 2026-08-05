package io.github.ielammari.bridge.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * An evaluation of an application: the HR preselection, the technical exam, or
 * the final HR interview. Only technical evaluations carry per trait scores.
 * At most one evaluation of each type per application.
 */
@Entity
@Table(name = "evaluation")
public class Evaluation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_evaluation")
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", nullable = false, length = 20)
	private EvaluationType type;

	@Column(name = "commentaire", columnDefinition = "text")
	private String comment;

	@Enumerated(EnumType.STRING)
	@Column(name = "decision", nullable = false, length = 20)
	private Decision decision;

	@Column(name = "date_evaluation", nullable = false)
	private Instant date;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_candidature", nullable = false)
	private Application application;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_evaluateur", nullable = false)
	private Evaluator evaluator;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_rendez_vous", unique = true)
	private Appointment appointment;

	@OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<TraitScore> scores = new ArrayList<>();

	protected Evaluation() {
	}

	public Evaluation(EvaluationType type, Decision decision, String comment,
			Application application, Evaluator evaluator) {
		this.type = type;
		this.decision = decision;
		this.comment = comment;
		this.application = application;
		this.evaluator = evaluator;
		this.date = Instant.now();
	}

	public void addScore(Trait trait, short note) {
		scores.add(new TraitScore(this, trait, note));
	}

	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
	}

	public Integer getId() {
		return id;
	}

	public EvaluationType getType() {
		return type;
	}

	public String getComment() {
		return comment;
	}

	public Decision getDecision() {
		return decision;
	}

	public Instant getDate() {
		return date;
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public Application getApplication() {
		return application;
	}

	public List<TraitScore> getScores() {
		return scores;
	}

}
