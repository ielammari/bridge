package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Hiring;

public interface HiringRepository extends JpaRepository<Hiring, Integer> {

	Optional<Hiring> findByApplicationId(Integer applicationId);

	/** The hires register, most recent start date first. */
	@Query("SELECT h FROM Hiring h JOIN FETCH h.application a JOIN FETCH a.candidate JOIN FETCH a.offer "
			+ "ORDER BY h.startDate DESC")
	List<Hiring> findRegister();

}
