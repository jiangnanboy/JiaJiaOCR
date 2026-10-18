package com.clipper.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This structure contains a sequence of Point64 vertices defining a single
 * contour (see also terminology). Paths may be open and represent a series of
 * line segments defined by 2 or more vertices, or they may be closed and
 * represent polygons. Whether or not a path is open depends on its context.
 * Closed paths may be 'outer' contours, or they may be 'hole' contours, and
 * this usually depends on their orientation (whether arranged roughly
 * clockwise, or arranged counter-clockwise).
 */
@SuppressWarnings("serial")
public class Path64 extends ArrayList<Point64> {

	public Path64() {
		super();
	}

	public Path64(int n) {
		super(n);
	}

	public Path64(List<Point64> path) {
		super(path);
	}

	public Path64(Point64... path) {
		super(Arrays.asList(path));
	}

	@Override
	public String toString() {
		String s = "";
		for (Point64 p : this) {
			s = s + p.toString() + " ";
		}
		return s;
	}

}
