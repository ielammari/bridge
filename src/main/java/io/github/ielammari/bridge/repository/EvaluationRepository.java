package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.EvaluationType;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

	boolean existsByApplicationIdAndType(Integer applicationId, EvaluationType type);

	/** The evaluations recorded against one application, oldest first. */
	@Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.scores s LEFT JOIN FETCH s.trait "
			+ "JOIN FETCH e.evaluator WHERE e.application.id = :applicationId ORDER BY e.date")
	List<Evaluation> findTrail(Integer applicationId);

	/** The evaluations one evaluator authored, most recent first. */
	@Query("SELECT DISTINCT e FROM Evaluation e LEFT JOIN FETCH e.scores s LEFT JOIN FETCH s.trait "
			+ "JOIN FETCH e.application a JOIN FETCH a.candidate JOIN FETCH a.offer "
			+ "WHERE e.evaluator.id = :evaluatorId ORDER BY e.date DESC")
	List<Evaluation> findAuthoredBy(Integer evaluatorId);

	boolean existsByApplicationIdAndEvaluatorId(Integer applicationId, Integer evaluatorId);

	/**
	 * Whether this evaluator has assessed this candidate at all, on any of their
	 * applications. What lets an expert open the person behind an exam they ran,
	 * and nobody else.
	 */
	@Query("""
			SELECT COUNT(e) > 0 FROM Evaluation e
			WHERE e.evaluator.id = :evaluatorId AND e.application.candidate.id = :candidateId
			""")
	boolean hasAssessed(@Param("evaluatorId") Integer evaluatorId,
			@Param("candidateId") Integer candidateId);

	/** Whether this evaluator has worked on any application to this offer. */
	@Query("""
			SELECT COUNT(e) > 0 FROM Evaluation e
			WHERE e.evaluator.id = :evaluatorId AND e.application.offer.id = :offerId
			""")
	boolean hasAssessedForOffer(@Param("evaluatorId") Integer evaluatorId,
			@Param("offerId") Integer offerId);

}
