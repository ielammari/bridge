package io.github.ielammari.bridge.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The interviews a calendar covers, with the number of bookable hours a day
 * holds, so a client can say how much of one is spent.
 */
public record CalendarDto(LocalDate from, LocalDate to, int capacity, List<CalendarEntryDto> entries) {
}
