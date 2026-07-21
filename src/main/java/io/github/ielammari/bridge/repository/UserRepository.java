package io.github.ielammari.bridge.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ielammari.bridge.model.User;

public interface UserRepository extends JpaRepository<User, Integer> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

}
