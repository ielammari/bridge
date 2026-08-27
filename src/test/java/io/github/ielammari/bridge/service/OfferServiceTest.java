package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import io.github.ielammari.bridge.dto.OfferDto;
import io.github.ielammari.bridge.dto.OfferRequest;
import io.github.ielammari.bridge.dto.OfferRequest.RequirementSelection;
import io.github.ielammari.bridge.exception.ApiException;
import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Degree;
import io.github.ielammari.bridge.model.OfferStatus;
import io.github.ielammari.bridge.repository.TraitRepository;
import io.github.ielammari.bridge.repository.UserRepository;

@SpringBootTest
@Transactional
class OfferServiceTest {

	@Autowired private OfferService offerService;
	@Autowired private TraitRepository traits;
	@Autowired private UserRepository users;

	private Integer hrId() {
		return users.findByEmailIgnoreCase("rh@bridge.local").orElseThrow().getId();
	}

	private Integer aTraitId() {
		return traits.findAll().get(0).getId();
	}

	private OfferRequest offer(boolean publishNow, List<RequirementSelection> reqs, BigDecimal min, BigDecimal max) {
		return new OfferRequest("Titre", null, "Description", Degree.BAC_3, ContractType.PERMANENT,
				"Lyon", null, min, max, false, reqs, publishNow);
	}

	@Test
	void createAsDraftKeepsTheOfferUnpublished() {
		OfferDto dto = offerService.create(hrId(),
				offer(false, List.of(new RequirementSelection(aTraitId(), true)), null, null));

		assertThat(dto.status()).isEqualTo(OfferStatus.BROUILLON);
	}

	@Test
	void createWithPublishNowPublishesImmediately() {
		OfferDto dto = offerService.create(hrId(),
				offer(true, List.of(new RequirementSelection(aTraitId(), true)), null, null));

		assertThat(dto.status()).isEqualTo(OfferStatus.PUBLIEE);
	}

	@Test
	void anOfferWithoutAMandatoryTraitIsRejected() {
		assertThatThrownBy(() -> offerService.create(hrId(),
				offer(false, List.of(new RequirementSelection(aTraitId(), false)), null, null)))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "NO_MANDATORY_TRAIT");
	}

	@Test
	void anInvertedSalaryRangeIsRejected() {
		assertThatThrownBy(() -> offerService.create(hrId(),
				offer(false, List.of(new RequirementSelection(aTraitId(), true)),
						new BigDecimal("60000"), new BigDecimal("40000"))))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "SALARY_RANGE_INVALID");
	}

	@Test
	void publishThenCloseMovesTheStatusAcrossTheLifecycle() {
		Integer id = offerService.create(hrId(),
				offer(false, List.of(new RequirementSelection(aTraitId(), true)), null, null)).id();

		assertThat(offerService.publish(hrId(), id).status()).isEqualTo(OfferStatus.PUBLIEE);
		assertThat(offerService.close(hrId(), id).status()).isEqualTo(OfferStatus.CLOTUREE);
	}

	@Test
	void aClosedOfferCannotBePublishedAgain() {
		Integer id = offerService.create(hrId(),
				offer(true, List.of(new RequirementSelection(aTraitId(), true)), null, null)).id();
		offerService.close(hrId(), id);

		assertThatThrownBy(() -> offerService.publish(hrId(), id))
				.isInstanceOf(ApiException.class)
				.hasFieldOrPropertyWithValue("code", "OFFER_CLOSED");
	}

	@Test
	void updateReplacesTheRequirementSet() {
		List<Integer> ids = traits.findAll().stream().limit(3).map(t -> t.getId()).toList();
		Integer id = offerService.create(hrId(), offer(false,
				List.of(new RequirementSelection(ids.get(0), true), new RequirementSelection(ids.get(1), false)),
				null, null)).id();

		OfferDto updated = offerService.update(hrId(), id, offer(false,
				List.of(new RequirementSelection(ids.get(2), true)), null, null));

		assertThat(updated.requirements()).singleElement()
				.satisfies(r -> assertThat(r.traitId()).isEqualTo(ids.get(2)));
	}

}
