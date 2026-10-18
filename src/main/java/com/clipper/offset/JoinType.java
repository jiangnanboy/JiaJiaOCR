package com.clipper.offset;

/**
 * The JoinType enumerator is only needed when offsetting (inflating/shrinking).
 * It isn't needed for polygon clipping.
 * <p>
 * When adding paths to a ClipperOffset object via the AddPaths method, the
 * joinType parameter may be any one of these types - Square, Round or Miter.
 *
 */
public enum JoinType {
	/**
	 * Squaring is applied uniformally at all joins where the internal join angle is
	 * less that 90 degrees. The squared edge will be at exactly the offset distance
	 * from the join vertex.
	 */
	Square,
	/**
	 * Rounding is applied to all joins that have convex external angles, and it
	 * maintains the exact offset distance from the join vertex.
	 */
	Round,
	/**
	 * There's a necessary limit to mitered joins (to avoid narrow angled joins
	 * producing excessively long and narrow spikes). So where mitered joins would
	 * exceed a given maximum miter distance (relative to the offset distance),
	 * these are 'squared' instead.
	 */
	Miter;

}