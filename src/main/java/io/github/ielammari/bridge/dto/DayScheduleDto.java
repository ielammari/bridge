package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.util.List;

/** The hourly grid for one day, used by HR when booking an interview. */
public record DayScheduleDto(LocalDate date, List<DaySlotDto> slots) {
}
