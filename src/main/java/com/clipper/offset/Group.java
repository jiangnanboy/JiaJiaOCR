package com.clipper.offset;

import com.clipper.core.Path64;
import com.clipper.core.Paths64;

class Group {

	Paths64 inPaths;
	Path64 outPath;
	Paths64 outPaths;
	JoinType joinType;
	EndType endType;
	boolean pathsReversed;

	Group(Paths64 paths, JoinType joinType) {
		this(paths, joinType, EndType.Polygon);
	}

	Group(Paths64 paths, JoinType joinType, EndType endType) {
		inPaths = new Paths64(paths);
		this.joinType = joinType;
		this.endType = endType;
		outPath = new Path64();
		outPaths = new Paths64();
		pathsReversed = false;
	}
}