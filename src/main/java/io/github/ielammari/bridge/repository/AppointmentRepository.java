package io.github.ielammari.bridge.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.AppointmentType;

public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {

	boolean existsByDateAndTime(LocalDate date, LocalTime time);

	boolean existsByDateAndTimeAndIdNot(LocalDate date, LocalTime time, Integer id);

	Optional<Appointment> findByApplicationIdAndType(Integer applicationId, AppointmentType type);

	List<Appointment> findByApplicationIdOrderByDateAscTimeAsc(Integer applicationId);

	@Query("SELECT a FROM Appointment a JOIN FETCH a.application ap JOIN FETCH ap.candidate "
			+ "WHERE a.date = :date ORDER BY a.time")
	List<Appointment> findByDateWithCandidate(LocalDate date);

}
