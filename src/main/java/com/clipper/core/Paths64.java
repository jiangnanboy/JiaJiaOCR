package com.clipper.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Paths64 represent one or more Path64 structures. While a single path can
 * represent a simple polygon, multiple paths are usually required to define
 * complex polygons that contain one or more holes.
 */
@SuppressWarnings("serial")
public class Paths64 extends ArrayList<Path64> {

	public Paths64() {
		super();
	}

	public Paths64(int n) {
		super(n);
	}

	public Paths64(List<Path64> paths) {
		super(paths);
	}

	public Paths64(Path64... paths) {
		super(Arrays.asList(paths));
	}

	@Override
	public String toString() {
		String s = "";
		for (Path64 p : this) {
			s = s + p.toString() + "\n";
		}
		return s;
	}

}
