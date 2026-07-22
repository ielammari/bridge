package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.JobOffer;
import io.github.ielammari.bridge.model.OfferStatus;

public interface JobOfferRepository extends JpaRepository<JobOffer, Integer> {

	@Query("SELECT DISTINCT o FROM JobOffer o "
			+ "LEFT JOIN FETCH o.requirements r LEFT JOIN FETCH r.trait t LEFT JOIN FETCH t.category "
			+ "ORDER BY o.publicationDate DESC, o.id DESC")
	List<JobOffer> findAllWithRequirements();

	@Query("SELECT DISTINCT o FROM JobOffer o "
			+ "LEFT JOIN FETCH o.requirements r LEFT JOIN FETCH r.trait t LEFT JOIN FETCH t.category "
			+ "WHERE o.status = :status ORDER BY o.publicationDate DESC, o.id DESC")
	List<JobOffer> findByStatusWithRequirements(OfferStatus status);

	@Query("SELECT o FROM JobOffer o "
			+ "LEFT JOIN FETCH o.requirements r LEFT JOIN FETCH r.trait t LEFT JOIN FETCH t.category "
			+ "WHERE o.id = :id")
	Optional<JobOffer> findByIdWithRequirements(Integer id);

}
