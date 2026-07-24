package io.github.ielammari.bridge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.EvaluationType;

public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

	boolean existsByApplicationIdAndType(Integer applicationId, EvaluationType type);

}
