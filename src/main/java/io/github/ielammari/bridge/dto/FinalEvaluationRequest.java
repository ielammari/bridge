package io.github.ielammari.bridge.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.ielammari.bridge.model.ContractType;
import io.github.ielammari.bridge.model.Decision;
import io.github.ielammari.bridge.model.RemoteMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * The final HR decision. The interview data is recorded whatever the outcome;
 * the hiring terms are used only on acceptance and are validated then by the
 * service, since bean validation cannot depend on the decision.
 */
public record FinalEvaluationRequest(

		@NotNull(message = "La décision est obligatoire.")
		Decision decision,

		@Size(max = 4000, message = "Le commentaire est trop long.")
		String comment,

		@Valid
		@NotNull(message = "Les données d'entretien sont obligatoires.")
		InterviewData interview,

		@Valid
		HiringTerms hiring) {

	/** Kept even when the application is refused. */
	public record InterviewData(
			@PositiveOrZero(message = "Le salaire attendu ne peut pas être négatif.")
			BigDecimal expectedSalary,
			LocalDate availabilityDate,
			ContractType envisagedContract,
			@Size(max = 30) String noticePeriod,
			@Size(max = 50) String scheduleFlexibility,
			RemoteMode remoteExpectation,
			@Size(max = 4000) String cultureFit) {
	}

	/** Present only on acceptance. */
	public record HiringTerms(
			@PositiveOrZero(message = "Le salaire négocié ne peut pas être négatif.")
			BigDecimal negotiatedSalary,
			LocalDate startDate,
			ContractType finalContract,
			@Size(max = 30) String trialPeriod,
			Boolean executiveStatus,
			@Size(max = 4000) String benefits) {
	}
}
