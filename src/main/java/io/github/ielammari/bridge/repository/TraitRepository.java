package io.github.ielammari.bridge.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Trait;

public interface TraitRepository extends JpaRepository<Trait, Integer> {

	// Fetches the category with each trait, so grouping does not fire a query
	// per row. The category order is the one chosen for the pickers.
	@Query("SELECT t FROM Trait t JOIN FETCH t.category c ORDER BY c.displayOrder, c.id, t.label")
	List<Trait> findAllWithCategory();

	long countByIdIn(Collection<Integer> ids);

}
