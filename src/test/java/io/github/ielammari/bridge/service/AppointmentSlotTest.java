package io.github.ielammari.bridge.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

/** The past slot rule, checked without depending on the hour the suite runs at. */
class AppointmentSlotTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 8, 2);
	private static final LocalTime NOW = LocalTime.of(14, 30);

	private boolean isPast(LocalDate date, LocalTime time) {
		return AppointmentService.isPast(date, time, TODAY, NOW);
	}

	@Test
	void yesterdayIsPastAtEveryHour() {
		assertThat(isPast(TODAY.minusDays(1), LocalTime.of(16, 0))).isTrue();
	}

	@Test
	void tomorrowIsNeverPastEvenAtTheFirstHour() {
		assertThat(isPast(TODAY.plusDays(1), LocalTime.of(9, 0))).isFalse();
	}

	@Test
	void anEarlierHourTodayIsPast() {
		assertThat(isPast(TODAY, LocalTime.of(9, 0))).isTrue();
	}

	@Test
	void aLaterHourTodayIsStillAvailable() {
		assertThat(isPast(TODAY, LocalTime.of(15, 0))).isFalse();
	}

	/** The current hour has started, so it is no longer bookable. */
	@Test
	void theCurrentMomentIsPast() {
		assertThat(isPast(TODAY, NOW)).isTrue();
	}

}
