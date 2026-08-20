package io.github.ielammari.bridge.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

	/** Whether this evaluator already holds that hour. */
	boolean existsByEvaluatorIdAndDateAndTime(Integer evaluatorId, LocalDate date, LocalTime time);

	boolean existsByEvaluatorIdAndDateAndTimeAndIdNot(Integer evaluatorId, LocalDate date, LocalTime time,
			Integer id);

	/** Whether the candidate is already expected somewhere at that hour. */
	boolean existsByApplicationCandidateIdAndDateAndTime(Integer candidateId, LocalDate date, LocalTime time);

	boolean existsByApplicationCandidateIdAndDateAndTimeAndIdNot(Integer candidateId, LocalDate date,
			LocalTime time, Integer id);

	Optional<Appointment> findByApplicationIdAndType(Integer applicationId, AppointmentType type);

	List<Appointment> findByApplicationIdOrderByDateAscTimeAsc(Integer applicationId);

	/** One evaluator's day, with enough of each occupant to name it. */
	@Query("SELECT a FROM Appointment a JOIN FETCH a.application ap "
			+ "JOIN FETCH ap.candidate JOIN FETCH ap.offer "
			+ "WHERE a.date = :date AND a.evaluator.id = :evaluatorId ORDER BY a.time")
	List<Appointment> findByDateAndEvaluatorWithCandidate(LocalDate date, Integer evaluatorId);

	/** The exams this expert holds that are still waiting to be sat. */
	@Query("SELECT a FROM Appointment a JOIN FETCH a.application ap "
			+ "JOIN FETCH ap.candidate JOIN FETCH ap.offer "
			+ "WHERE a.type = io.github.ielammari.bridge.model.AppointmentType.TECHNIQUE "
			+ "AND a.evaluator.id = :expertId "
			+ "AND ap.status = io.github.ielammari.bridge.model.ApplicationStatus.EXAMEN_TECHNIQUE "
			+ "ORDER BY a.date, a.time")
	List<Appointment> findExamsAssignedTo(Integer expertId);

	/** How many interviews each evaluator holds over a range, for the picker. */
	@Query("SELECT a.evaluator.id, COUNT(a) FROM Appointment a "
			+ "WHERE a.date BETWEEN :from AND :to GROUP BY a.evaluator.id")
	List<Object[]> countByEvaluatorBetween(LocalDate from, LocalDate to);

	/** Whether this evaluator holds an interview on that application. */
	boolean existsByApplicationIdAndEvaluatorId(Integer applicationId, Integer evaluatorId);

	/** Whether this evaluator holds an interview with that candidate at all. */
	boolean existsByApplicationCandidateIdAndEvaluatorId(Integer candidateId, Integer evaluatorId);

	/** Whether this evaluator holds an interview on any application to that offer. */
	boolean existsByApplicationOfferIdAndEvaluatorId(Integer offerId, Integer evaluatorId);

	/**
	 * Exams whose hour has gone by without an evaluation being recorded. The
	 * recruiter is prompted to hand them to somebody else.
	 */
	@Query("SELECT a FROM Appointment a JOIN FETCH a.application ap "
			+ "JOIN FETCH ap.candidate JOIN FETCH ap.offer "
			+ "WHERE a.type = :type AND a.status = io.github.ielammari.bridge.model.AppointmentStatus.PLANIFIE "
			+ "AND (a.date < :date OR (a.date = :date AND a.time <= :time)) "
			+ "AND ap.status = io.github.ielammari.bridge.model.ApplicationStatus.EXAMEN_TECHNIQUE")
	List<Appointment> findOverdue(AppointmentType type, LocalDate date, LocalTime time);

}
