package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ielammari.bridge.model.Education;

public interface EducationRepository extends JpaRepository<Education, Integer> {

	/**
	 * A path reads most recent first, and a qualification still being read for
	 * (null end year) sits at the top of it. Written out because null ordering
	 * cannot be expressed in a derived method name.
	 */
	@Query("""
			SELECT e FROM Education e
			WHERE e.candidate.id = :candidateId
			ORDER BY e.endYear DESC NULLS FIRST, e.startYear DESC
			""")
	List<Education> findPath(@Param("candidateId") Integer candidateId);

}
