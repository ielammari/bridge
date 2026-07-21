package io.github.ielammari.bridge.model;

/**
 * Degree ladder for the scalar matching check. Declaration order is the ranking:
 * a candidate satisfies an offer when the candidate rank is at least the offer
 * rank. Persisted as the enum name in the free text diplome column.
 */
public enum Degree {
	BAC,
	BAC_2,
	BAC_3,
	BAC_5,
	DOCTORAT;

	/** Higher means a more advanced degree. */
	public int rank() {
		return ordinal();
	}

	public boolean satisfies(Degree required) {
		return required == null || this.rank() >= required.rank();
	}
}
