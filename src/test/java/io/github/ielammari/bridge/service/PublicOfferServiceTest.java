package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.dto.PublicOfferDetailDto;
import io.github.ielammari.bridge.dto.PublicOfferDto;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.Trait;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

/** What a reader with no account is shown of the offers. */
@SpringBootTest
@Transactional
class PublicOfferServiceTest {

	@Autowired private OfferService offerService;
	@Autowired private PublicOfferService publicOffers;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	/** Two traits whose categories differ, the earlier ordered one first. */
	private List<Trait> twoCategories() {
		List<Trait> all = traits.findAllWithCategory();
		Trait first = all.get(0);
		Trait other = all.stream()
				.filter(trait -> !trait.getCategory().getId().equals(first.getCategory().getId()))
				.max(Comparator.comparing(trait -> trait.getCategory().getDisplayOrder()))
				.orElseThrow();
		return List.of(first, other);
	}

	private OfferDto offer(String title, boolean published, List<RequirementSelection> requirements) {
		return offerService.create(hrId(), new OfferRequest(title, "Atelier Iris", "Description longue",
				Degree.BAC_3, ContractType.PERMANENT, "Lyon", null, null, null,
				true, requirements, published));
	}

	private OfferDto published(String title) {
		return offer(title, true, List.of(new RequirementSelection(twoCategories().get(0).getId(), true)));
	}

	private PublicOfferDto find(Integer id) {
		return publicOffers.market().offers().stream()
				.filter(entry -> entry.id().equals(id))
				.findFirst()
				.orElse(null);
	}

	@Test
	void anOpenPositionIsListedWithWhatIdentifiesIt() {
		OfferDto created = published("Ingenieure plateforme");

		PublicOfferDto listed = find(created.id());

		assertThat(listed).isNotNull();
		assertThat(listed.title()).isEqualTo("Ingenieure plateforme");
		assertThat(listed.company()).isEqualTo("Atelier Iris");
		assertThat(listed.location()).isEqualTo("Lyon");
		assertThat(listed.contractType()).isEqualTo(ContractType.PERMANENT);
	}

	@Test
	void aDraftIsNeitherListedNorReadable() {
		OfferDto draft = offer("Brouillon", false,
				List.of(new RequirementSelection(twoCategories().get(0).getId(), true)));

		assertThat(find(draft.id())).isNull();
		assertThatThrownBy(() -> publicOffers.detail(draft.id()))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("introuvable");
	}

	@Test
	void aClosedOfferIsNeitherListedNorReadable() {
		OfferDto open = published("Poste pourvu");
		offerService.close(hrId(), open.id());

		assertThat(find(open.id())).isNull();
		assertThatThrownBy(() -> publicOffers.detail(open.id()))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void anUnknownOfferIsNotFound() {
		assertThatThrownBy(() -> publicOffers.detail(-1))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("introuvable");
	}

	/** The filter's options are every domain the open positions span, once each. */
	@Test
	void theMarketNamesTheDomainsItsOffersSpan() {
		List<Trait> pair = twoCategories();
		offer("Deux domaines", true, List.of(
				new RequirementSelection(pair.get(0).getId(), true),
				new RequirementSelection(pair.get(1).getId(), false)));

		List<String> domains = publicOffers.market().domains();

		assertThat(domains).contains(pair.get(0).getCategory().getLabel(),
				pair.get(1).getCategory().getLabel());
		assertThat(domains).doesNotHaveDuplicates();
		assertThat(domains.indexOf(pair.get(0).getCategory().getLabel()))
				.isLessThan(domains.indexOf(pair.get(1).getCategory().getLabel()));
	}

	/** The categories the offer looks for, deduplicated and in the picker order. */
	@Test
	void theDomainsAreTheCategoriesTheOfferLooksFor() {
		List<Trait> pair = twoCategories();
		List<Trait> sameCategory = traits.findAllWithCategory().stream()
				.filter(trait -> trait.getCategory().getId().equals(pair.get(0).getCategory().getId()))
				.limit(2)
				.toList();

		OfferDto created = offer("Deux domaines", true, List.of(
				new RequirementSelection(pair.get(1).getId(), false),
				new RequirementSelection(sameCategory.get(0).getId(), true),
				new RequirementSelection(sameCategory.get(1).getId(), true)));

		assertThat(find(created.id()).domains()).containsExactly(
				pair.get(0).getCategory().getLabel(),
				pair.get(1).getCategory().getLabel());
	}

	/** The page carries what decides an application, and nothing behind it. */
	@Test
	void theDetailCarriesTheDescriptionTheLevelAndTheTraits() {
		List<Trait> pair = twoCategories();
		OfferDto created = offer("Poste detaille", true, List.of(
				new RequirementSelection(pair.get(0).getId(), true),
				new RequirementSelection(pair.get(1).getId(), false)));

		PublicOfferDetailDto detail = publicOffers.detail(created.id());

		assertThat(detail.description()).isEqualTo("Description longue");
		assertThat(detail.requiredDegree()).isEqualTo(Degree.BAC_3);
		assertThat(detail.requirements()).hasSize(2);
		assertThat(detail.requirements().get(0).mandatory()).isTrue();
		assertThat(detail.requirements().get(1).mandatory()).isFalse();
		assertThat(detail.offer().title()).isEqualTo("Poste detaille");
	}

}
