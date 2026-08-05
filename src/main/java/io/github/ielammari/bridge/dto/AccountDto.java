package io.github.ielammari.bridge.dto;

import java.time.LocalDate;

import io.github.ielammari.bridge.model.Gender;
import io.github.ielammari.bridge.model.Role;

/** The account as its owner configures it. */
public record AccountDto(
		Integer id,
		String email,
		String firstName,
		String lastName,
		String phone,
		LocalDate birthDate,
		Gender gender,
		String city,
		String country,
		Role role,
		LocalDate registrationDate) {
}
