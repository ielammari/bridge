package io.github.ielammari.bridge.dto;

import java.util.List;

/**
 * Everything the public listing needs: the open positions, and the domains they
 * span, which is what it filters on.
 */
public record PublicMarketDto(
		List<PublicOfferDto> offers,
		List<String> domains) {
}
