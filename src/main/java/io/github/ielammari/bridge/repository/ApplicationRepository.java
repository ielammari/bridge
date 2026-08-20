package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

	boolean existsByCandidateIdAndOfferId(Integer candidateId, Integer offerId);

	/**
	 * Whether the candidate already holds an application to this offer that is
	 * still running. A refused one does not count: it is closed, and applying
	 * again is a new attempt rather than a duplicate.
	 */
	boolean existsByCandidateIdAndOfferIdAndStatusNot(Integer candidateId, Integer offerId,
			ApplicationStatus status);

	@Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.offer "
			+ "WHERE a.status = :status ORDER BY a.applicationDate")
	List<Application> findByStatusWithCandidateAndOffer(ApplicationStatus status);

	@Query("SELECT a FROM Application a JOIN FETCH a.offer o "
			+ "WHERE a.candidate.id = :candidateId ORDER BY a.applicationDate DESC")
	List<Application> findByCandidate(Integer candidateId);

	/**
	 * The candidate's applications this evaluator worked on themselves. An exam
	 * entitles them to that application and to nothing else the candidate has
	 * running elsewhere.
	 */
	@Query("SELECT a FROM Application a JOIN FETCH a.offer o "
			+ "WHERE a.candidate.id = :candidateId "
			+ "AND (EXISTS (SELECT 1 FROM Evaluation e WHERE e.application = a AND e.evaluator.id = :evaluatorId)"
			+ "OR EXISTS (SELECT 1 FROM Appointment r WHERE r.application = a AND r.evaluator.id = :evaluatorId))"
			+ "ORDER BY a.applicationDate DESC")
	List<Application> findByCandidateAssessedBy(Integer candidateId, Integer evaluatorId);

	@Query("SELECT a FROM Application a JOIN FETCH a.candidate c "
			+ "WHERE a.offer.id = :offerId ORDER BY a.applicationDate DESC")
	List<Application> findByOffer(Integer offerId);

	/** Every application nobody can move any further, across all offers. */
	@Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.offer "
			+ "WHERE a.status IN :statuses ORDER BY a.applicationDate DESC")
	List<Application> findByStatusIn(List<ApplicationStatus> statuses);

	/** The same, kept to the offers one recruiter published. */
	@Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.offer o "
			+ "WHERE a.status IN :statuses AND o.publisher.id = :publisherId "
			+ "ORDER BY a.applicationDate DESC")
	List<Application> findByStatusInForPublisher(List<ApplicationStatus> statuses, Integer publisherId);

	/** The candidate's applications to one recruiter's own offers. */
	@Query("SELECT a FROM Application a JOIN FETCH a.offer o "
			+ "WHERE a.candidate.id = :candidateId AND o.publisher.id = :publisherId "
			+ "ORDER BY a.applicationDate DESC")
	List<Application> findByCandidateForPublisher(Integer candidateId, Integer publisherId);

	/** Whether this candidate has ever applied to an offer this recruiter published. */
	@Query("""
			SELECT COUNT(a) > 0 FROM Application a
			WHERE a.candidate.id = :candidateId AND a.offer.publisher.id = :publisherId
			""")
	boolean existsByCandidateAndPublisher(Integer candidateId, Integer publisherId);

	@Query("SELECT a FROM Application a JOIN FETCH a.offer o JOIN FETCH a.candidate c WHERE a.id = :id")
	Optional<Application> findByIdWithOfferAndCandidate(Integer id);

}
