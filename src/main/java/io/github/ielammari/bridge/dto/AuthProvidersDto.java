package io.github.ielammari.bridge.dto;

/** What the auth pages need to offer a sign in method. A null id hides it. */
public record AuthProvidersDto(String googleClientId) {
}
