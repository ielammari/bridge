package io.github.ielammari.bridge.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DegreeTest {

	@Test
	void higherDegreeSatisfiesLowerRequirement() {
		assertThat(Degree.BAC_5.satisfies(Degree.BAC_3)).isTrue();
	}

	@Test
	void equalDegreeSatisfiesRequirement() {
		assertThat(Degree.BAC_3.satisfies(Degree.BAC_3)).isTrue();
	}

	@Test
	void lowerDegreeDoesNotSatisfyHigherRequirement() {
		assertThat(Degree.BAC.satisfies(Degree.DOCTORAT)).isFalse();
	}

	@Test
	void noRequirementIsAlwaysSatisfied() {
		assertThat(Degree.BAC.satisfies(null)).isTrue();
	}

	@Test
	void theLadderIsOrderedFromBacToDoctorat() {
		assertThat(Degree.BAC.rank())
				.isLessThan(Degree.BAC_2.rank());
		assertThat(Degree.BAC_2.rank())
				.isLessThan(Degree.BAC_3.rank());
		assertThat(Degree.BAC_3.rank())
				.isLessThan(Degree.BAC_5.rank());
		assertThat(Degree.BAC_5.rank())
				.isLessThan(Degree.DOCTORAT.rank());
	}

}
