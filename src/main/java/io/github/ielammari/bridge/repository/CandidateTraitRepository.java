package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.model.CandidateTrait;
import io.github.ielammari.bridge.model.CandidateTraitId;

public interface CandidateTraitRepository extends JpaRepository<CandidateTrait, CandidateTraitId> {

	@Query("SELECT ct FROM CandidateTrait ct JOIN FETCH ct.trait t JOIN FETCH t.category "
			+ "WHERE ct.id.candidateId = :candidateId ORDER BY t.category.id, t.label")
	List<CandidateTrait> findByCandidate(Integer candidateId);

	@Transactional
	void deleteByIdCandidateId(Integer candidateId);

}
