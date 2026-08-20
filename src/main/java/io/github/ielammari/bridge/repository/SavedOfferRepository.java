package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.SavedOffer;

public interface SavedOfferRepository extends JpaRepository<SavedOffer, SavedOffer.Key> {

	List<SavedOffer> findByCandidateIdOrderBySavedAtDesc(Integer candidateId);

	boolean existsByCandidateIdAndOfferId(Integer candidateId, Integer offerId);

	void deleteByCandidateIdAndOfferId(Integer candidateId, Integer offerId);

	long countByCandidateId(Integer candidateId);

}
