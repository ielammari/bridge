package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Application;
import io.github.ielammari.bridge.model.ApplicationStatus;

public interface ApplicationRepository extends JpaRepository<Application, Integer> {

	boolean existsByCandidateIdAndOfferId(Integer candidateId, Integer offerId);

	@Query("SELECT a FROM Application a JOIN FETCH a.candidate JOIN FETCH a.offer "
			+ "WHERE a.status = :status ORDER BY a.applicationDate")
	List<Application> findByStatusWithCandidateAndOffer(ApplicationStatus status);

	@Query("SELECT a FROM Application a JOIN FETCH a.offer o "
			+ "WHERE a.candidate.id = :candidateId ORDER BY a.applicationDate DESC")
	List<Application> findByCandidate(Integer candidateId);

	@Query("SELECT a FROM Application a JOIN FETCH a.candidate c "
			+ "WHERE a.offer.id = :offerId ORDER BY a.applicationDate DESC")
	List<Application> findByOffer(Integer offerId);

	@Query("SELECT a FROM Application a JOIN FETCH a.offer o JOIN FETCH a.candidate c WHERE a.id = :id")
	Optional<Application> findByIdWithOfferAndCandidate(Integer id);

}
