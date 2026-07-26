package io.github.ielammari.bridge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.Hiring;

public interface HiringRepository extends JpaRepository<Hiring, Integer> {

	Optional<Hiring> findByApplicationId(Integer applicationId);

}
