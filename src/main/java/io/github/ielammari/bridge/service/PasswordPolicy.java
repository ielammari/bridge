package io.github.ielammari.bridge.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import io.github.ielammari.bridge.exception.ApiException;

/**
 * The rules a chosen password must meet. The signup form shows the same list as
 * a checklist; this is where they are enforced.
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 12;

	private static final Pattern LOWERCASE = Pattern.compile("\\p{Ll}");
	private static final Pattern UPPERCASE = Pattern.compile("\\p{Lu}");
	private static final Pattern DIGIT = Pattern.compile("\\p{Nd}");
	private static final Pattern SPECIAL = Pattern.compile("[^\\p{L}\\p{Nd}]");
	private static final Pattern TRIPLE = Pattern.compile("(.)\\1\\1");
	private static final Pattern DIACRITIC = Pattern.compile("\\p{M}");

	/** Personal fragments shorter than this match too much to be meaningful. */
	private static final int MIN_FRAGMENT = 4;

	private PasswordPolicy() {
	}

	/** @throws ApiException naming every rule the password breaks, not just the first. */
	public static void check(String password, String email, String firstName, String lastName) {
		List<String> unmet = new ArrayList<>();

		if (password == null || password.length() < MIN_LENGTH) {
			unmet.add(MIN_LENGTH + " caractères au minimum");
		}
		if (password == null) {
			throw ApiException.badRequest("WEAK_PASSWORD", message(unmet));
		}

		if (!LOWERCASE.matcher(password).find()) {
			unmet.add("une lettre minuscule");
		}
		if (!UPPERCASE.matcher(password).find()) {
			unmet.add("une lettre majuscule");
		}
		if (!DIGIT.matcher(password).find()) {
			unmet.add("un chiffre");
		}
		if (!SPECIAL.matcher(password).find()) {
			unmet.add("un caractère spécial");
		}
		if (TRIPLE.matcher(password).find()) {
			unmet.add("jamais trois fois le même caractère de suite");
		}
		if (containsPersonalFragment(password, email, firstName, lastName)) {
			unmet.add("ne reprend ni votre nom ni votre adresse email");
		}

		if (!unmet.isEmpty()) {
			throw ApiException.badRequest("WEAK_PASSWORD", message(unmet));
		}
	}

	private static boolean containsPersonalFragment(String password, String email, String firstName, String lastName) {
		String folded = fold(password);
		return fragments(email, firstName, lastName).stream().anyMatch(folded::contains);
	}

	private static List<String> fragments(String email, String firstName, String lastName) {
		String localPart = email == null ? "" : email.split("@")[0];
		return java.util.stream.Stream.of(firstName, lastName, localPart)
				.filter(part -> part != null)
				.map(part -> fold(part.trim()))
				.filter(part -> part.length() >= MIN_FRAGMENT)
				.toList();
	}

	private static String fold(String value) {
		return DIACRITIC.matcher(Normalizer.normalize(value, Normalizer.Form.NFD))
				.replaceAll("")
				.toLowerCase();
	}

	private static String message(List<String> unmet) {
		return "Ce mot de passe ne remplit pas les conditions suivantes : " + String.join(", ", unmet) + ".";
	}

}
