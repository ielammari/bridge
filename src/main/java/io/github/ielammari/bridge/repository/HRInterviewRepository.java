package io.github.ielammari.bridge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.HRInterview;

public interface HRInterviewRepository extends JpaRepository<HRInterview, Integer> {

	Optional<HRInterview> findByApplicationId(Integer applicationId);
}
