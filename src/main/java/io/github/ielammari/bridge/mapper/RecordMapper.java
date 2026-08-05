package io.github.ielammari.bridge.mapper;

import java.util.List;

import io.github.ielammari.bridge.dto.AppointmentDto;
import io.github.ielammari.bridge.dto.EvaluationDto;
import io.github.ielammari.bridge.dto.HiringDto;
import io.github.ielammari.bridge.dto.HRInterviewDto;
import io.github.ielammari.bridge.dto.TraitScoreDto;
import io.github.ielammari.bridge.model.Appointment;
import io.github.ielammari.bridge.model.Evaluation;
import io.github.ielammari.bridge.model.Hiring;
import io.github.ielammari.bridge.model.HRInterview;
import io.github.ielammari.bridge.model.TraitScore;

/** Turns what the funnel recorded into the shapes the history views read. */
public final class RecordMapper {

	private RecordMapper() {
	}

	public static EvaluationDto toDto(Evaluation evaluation) {
		return new EvaluationDto(
				evaluation.getId(),
				evaluation.getType(),
				evaluation.getDecision(),
				evaluation.getComment(),
				evaluation.getDate(),
				evaluation.getEvaluator().getFirstName() + " " + evaluation.getEvaluator().getLastName(),
				evaluation.getScores().stream().map(RecordMapper::toDto).toList());
	}

	public static TraitScoreDto toDto(TraitScore score) {
		return new TraitScoreDto(score.getTrait().getId(), score.getTrait().getLabel(), score.getNote());
	}

	public static AppointmentDto toDto(Appointment appointment) {
		return new AppointmentDto(
				appointment.getId(),
				appointment.getType(),
				appointment.getStatus(),
				appointment.getDate(),
				appointment.getTime());
	}

	public static HRInterviewDto toDto(HRInterview interview) {
		return new HRInterviewDto(
				interview.getExpectedSalary(),
				interview.getAvailabilityDate(),
				interview.getEnvisagedContract(),
				interview.getNoticePeriod(),
				interview.getScheduleFlexibility(),
				interview.getRemoteExpectation(),
				interview.getCultureFit());
	}

	public static HiringDto toDto(Hiring hiring) {
		return new HiringDto(
				hiring.getId(),
				hiring.getNegotiatedSalary(),
				hiring.getStartDate(),
				hiring.getFinalContract(),
				hiring.getTrialPeriod(),
				hiring.getExecutiveStatus(),
				hiring.getBenefits());
	}

	public static List<AppointmentDto> toDtos(List<Appointment> appointments) {
		return appointments.stream().map(RecordMapper::toDto).toList();
	}

}
