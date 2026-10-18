package com.clipper.core;

/**
 * All polygon clipping is performed with a Clipper object with the specific
 * boolean operation indicated by the ClipType parameter passed in its Execute
 * method.
 * <p>
 * With regard to open paths (polylines), clipping rules generally match those
 * of closed paths (polygons). However, when there are both polyline and polygon
 * subjects, the following clipping rules apply:
 * <ul>
 * <li>union operations - polylines will be clipped by any overlapping polygons
 * so that only non-overlapped portions will be returned in the solution,
 * together with solution polygons</li>
 * <li>intersection, difference and xor operations - polylines will be clipped
 * by 'clip' polygons, and there will be not interaction between polylines and
 * any subject polygons.</li>
 * </ul>
 *
 * There are four boolean operations:
 * <ul>
 * <li>AND (intersection) - regions covered by both subject and clip
 * polygons</li>
 * <li>OR (union) - regions covered by subject or clip polygons, or both
 * polygons</li>
 * <li>NOT (difference) - regions covered by subject, but not clip polygons</li>
 * <li>XOR (exclusive or) - regions covered by subject or clip polygons, but not
 * both</li>
 * </ul>
 */
public enum ClipType {

	None,
	/** Preserves regions covered by both subject and clip polygons */
	Intersection,
	/** Preserves regions covered by subject or clip polygons, or both polygons */
	Union,
	/** Preserves regions covered by subject, but not clip polygons */
	Difference,
	/** Preserves regions covered by subject or clip polygons, but not both */
	Xor;

}