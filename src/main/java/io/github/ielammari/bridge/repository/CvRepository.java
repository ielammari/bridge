package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.Cv;

public interface CvRepository extends JpaRepository<Cv, Integer> {

	List<Cv> findByCandidateIdOrderByUploadedAtDesc(Integer candidateId);

	long countByCandidateId(Integer candidateId);

}
