package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.MatchDto;
import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferMatchDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.RegisterRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest;
import io.github.ielammari.bridge.dto.UpdateProfileRequest.TraitSelection;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/**
 * Exercises the matching gate through the real feed: build an offer and a
 * candidate profile, then assert whether the offer surfaces.
 */
@SpringBootTest
@Transactional
class MatchingServiceTest {

	@Autowired private AuthService authService;
	@Autowired private ProfileService profileService;
	@Autowired private OfferService offerService;
	@Autowired private MatchingService matchingService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer newCandidate(String email) {
		return authService.register(new RegisterRequest(email, "Motdepasse1!x", "Match", "Test", null, LocalDate.of(1995, 5, 20), null, null, null)).user().id();
	}

	private List<Trait> someTraits(int n) {
		return traits.findAll().subList(0, n);
	}

	private void giveProfile(Integer candidateId, Degree degree, List<Trait> held) {
		profileService.update(candidateId, new UpdateProfileRequest(degree, null,
				held.stream().map(t -> new TraitSelection(t.getId(), null)).toList()));
	}

	private List<Integer> ids(List<OfferMatchDto> entries) {
		return entries.stream().map(entry -> entry.offer().id()).toList();
	}

	private MatchDto matchFor(Integer candidateId, Integer offerId) {
		return matchingService.feed(candidateId, false).stream()
				.filter(entry -> entry.offer().id().equals(offerId))
				.findFirst().orElseThrow().match();
	}

	private Integer publishOffer(Degree required, List<RequirementSelection> reqs) {
		OfferDto dto = offerService.create(hrId(), new OfferRequest(
				"Ingénieur", null, "Description", required, ContractType.PERMANENT,
				"Paris", null, null, null, false, reqs, true));
		return dto.id();
	}

	@Test
	void offerSurfacesWhenAllRequiredTraitsAndDegreeAreMet() {
		List<Trait> t = someTraits(2);
		Integer candidate = newCandidate("m1@example.fr");
		giveProfile(candidate, Degree.BAC_5, t);
		publishOffer(Degree.BAC_3, List.of(
				new RequirementSelection(t.get(0).getId(), true),
				new RequirementSelection(t.get(1).getId(), true)));

		assertThat(matchingService.feed(candidate, true)).hasSize(1);
	}

	@Test
	void offerIsHiddenWhenARequiredTraitIsMissing() {
		List<Trait> t = someTraits(3);
		Integer candidate = newCandidate("m2@example.fr");
		giveProfile(candidate, Degree.BAC_5, List.of(t.get(0))); // holds only one
		publishOffer(Degree.BAC, List.of(
				new RequirementSelection(t.get(0).getId(), true),
				new RequirementSelection(t.get(1).getId(), true)));

		assertThat(matchingService.feed(candidate, true)).isEmpty();
	}

	@Test
	void aMissingPlusTraitDoesNotHideTheOffer() {
		List<Trait> t = someTraits(2);
		Integer candidate = newCandidate("m3@example.fr");
		giveProfile(candidate, Degree.BAC_3, List.of(t.get(0))); // holds the required one only
		publishOffer(Degree.BAC, List.of(
				new RequirementSelection(t.get(0).getId(), true),
				new RequirementSelection(t.get(1).getId(), false))); // t1 is a plus

		assertThat(matchingService.feed(candidate, true)).hasSize(1);
	}

	@Test
	void offerIsHiddenWhenTheDegreeIsTooLow() {
		List<Trait> t = someTraits(1);
		Integer candidate = newCandidate("m4@example.fr");
		giveProfile(candidate, Degree.BAC, t);
		publishOffer(Degree.BAC_5, List.of(new RequirementSelection(t.get(0).getId(), true)));

		assertThat(matchingService.feed(candidate, true)).isEmpty();
	}

	@Test
	void aCandidateWithoutADegreeSeesNothing() {
		List<Trait> t = someTraits(1);
		Integer candidate = newCandidate("m5@example.fr");
		giveProfile(candidate, null, t);
		publishOffer(Degree.BAC, List.of(new RequirementSelection(t.get(0).getId(), true)));

		assertThat(matchingService.feed(candidate, true)).isEmpty();
	}

	@Test
	void theWholeMarketIsListedWithWhatTheCandidateLacks() {
		List<Trait> t = someTraits(2);
		Integer candidate = newCandidate("m7@example.fr");
		giveProfile(candidate, Degree.BAC_5, List.of(t.get(0)));
		Integer offerId = publishOffer(Degree.BAC, List.of(
				new RequirementSelection(t.get(0).getId(), true),
				new RequirementSelection(t.get(1).getId(), true)));

		assertThat(ids(matchingService.feed(candidate, true))).doesNotContain(offerId);

		MatchDto match = matchFor(candidate, offerId);
		assertThat(match.compatible()).isFalse();
		assertThat(match.degreeMet()).isTrue();
		assertThat(match.missingTraits()).containsExactly(t.get(1).getLabel());
	}

	@Test
	void aDegreeShortOfTheOfferIsReportedOnItsOwn() {
		List<Trait> t = someTraits(1);
		Integer candidate = newCandidate("m8@example.fr");
		giveProfile(candidate, Degree.BAC, t);
		Integer offerId = publishOffer(Degree.BAC_5, List.of(new RequirementSelection(t.get(0).getId(), true)));

		MatchDto match = matchFor(candidate, offerId);
		assertThat(match.compatible()).isFalse();
		assertThat(match.degreeMet()).isFalse();
		assertThat(match.missingTraits()).isEmpty();
	}

	@Test
	void unpublishedOffersAreOutsideEveryScope() {
		List<Trait> t = someTraits(1);
		Integer candidate = newCandidate("m6@example.fr");
		giveProfile(candidate, Degree.BAC_5, t);

		// A draft (publishNow = false).
		Integer draft = offerService.create(hrId(), new OfferRequest("Brouillon", null, "d", Degree.BAC,
				ContractType.PERMANENT, null, null, null, null,
				false, List.of(new RequirementSelection(t.get(0).getId(), true)), false)).id();
		// A published then closed offer.
		Integer closed = publishOffer(Degree.BAC, List.of(new RequirementSelection(t.get(0).getId(), true)));
		offerService.close(hrId(), closed);

		assertThat(ids(matchingService.feed(candidate, true))).doesNotContain(draft, closed);
		assertThat(ids(matchingService.feed(candidate, false))).doesNotContain(draft, closed);
	}

}
