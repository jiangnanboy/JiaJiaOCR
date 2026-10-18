package com.clipper.core;

/**
 * Complex polygons are defined by one or more closed paths that set both outer
 * and inner polygon boundaries. But only portions of these paths (or
 * 'contours') may be setting polygon boundaries, so crossing a path may or may
 * not mean entering or exiting a 'filled' polygon region. For this reason
 * complex polygons require filling rules that define which polygon sub-regions
 * will be considered inside a given polygon, and which sub-regions will not.
 * <p>
 * The Clipper Library supports 4 filling rules: Even-Odd, Non-Zero, Positive
 * and Negative. These rules are base on the winding numbers (see below) of each
 * polygon sub-region, which in turn are based on the orientation of each path.
 * Orientation is determined by the order in which vertices are declared during
 * path construction, and whether these vertices progress roughly clockwise or
 * counter-clockwise.
 * <p>
 * By far the most widely used filling rules for polygons are EvenOdd and
 * NonZero, sometimes called Alternate and Winding respectively.
 * <p>
 * https://en.wikipedia.org/wiki/Nonzero-rule
 */
public enum FillRule {

	/** Only odd numbered sub-regions are filled */
	EvenOdd,
	/** Only non-zero sub-regions are filled */
	NonZero,
	/** Only sub-regions with winding counts > 0 are filled */
	Positive,
	/** Only sub-regions with winding counts < 0 are filled */
	Negative;

}