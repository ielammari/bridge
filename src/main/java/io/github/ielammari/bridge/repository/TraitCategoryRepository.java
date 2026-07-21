package io.github.ielammari.bridge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.TraitCategory;

public interface TraitCategoryRepository extends JpaRepository<TraitCategory, Integer> {

	List<TraitCategory> findAllByOrderByIdAsc();

}
