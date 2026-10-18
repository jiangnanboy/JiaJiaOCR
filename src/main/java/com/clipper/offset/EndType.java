package com.clipper.offset;

/**
 * The EndType enumerator is only needed when offsetting (inflating/shrinking).
 * It isn't needed for polygon clipping.
 * <p>
 * EndType has 5 values:
 * <ul>
 * <li><b>Polygon</b>: the path is treated as a polygon
 * <li><b>Join</b>: ends are joined and the path treated as a polyline
 * <li><b>Square</b>: ends extend the offset amount while being squared off
 * <li><b>Round</b>: ends extend the offset amount while being rounded off
 * <li><b>Butt</b>: ends are squared off without any extension
 * </ul>
 * With both EndType.Polygon and EndType.Join, path closure will occur
 * regardless of whether or not the first and last vertices in the path match.
 */
public enum EndType {

	Polygon, Joined, Butt, Square, Round;

}