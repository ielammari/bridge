package io.github.ielammari.bridge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.TechnicalExpert;
import io.github.ielammari.bridge.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	// No appointment is tied to a specific expert, so an exam notification goes
	// to every technical expert.
	@Query("SELECT e FROM TechnicalExpert e")
	List<TechnicalExpert> findAllExperts();

}
