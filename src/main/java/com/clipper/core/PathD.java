package com.clipper.core;

import java.util.ArrayList;
import java.util.List;

/**
 * This structure contains a sequence of PointD vertices defining a single
 * contour (see also terminology). Paths may be open and represent a series of
 * line segments defined by 2 or more vertices, or they may be closed and
 * represent polygons. Whether or not a path is open depends on its context.
 * Closed paths may be 'outer' contours, or they may be 'hole' contours, and
 * this usually depends on their orientation (whether arranged roughly
 * clockwise, or arranged counter-clockwise).
 */
@SuppressWarnings("serial")
public class PathD extends ArrayList<PointD> {

	public PathD() {
		super();
	}

	public PathD(int n) {
		super(n);
	}

	public PathD(List<PointD> path) {
		super(path);
	}

	@Override
	public String toString() {
		String s = "";
		for (PointD p : this) {
			s = s + p.toString() + " ";
		}
		return s;
	}

}
