package com.clipper.engine;

import com.clipper.core.ClipType;
import com.clipper.core.FillRule;
import com.clipper.core.Path64;
import com.clipper.core.PathType;
import com.clipper.core.Paths64;

/**
 * The Clipper class performs boolean 'clipping'. This class is very similar to
 * ClipperD except that coordinates passed to Clipper64 objects are of type
 * <code>long</code> instead of type <code>double</code>.
 */
public class Clipper64 extends ClipperBase {

	public final void addPath(Path64 path, PathType polytype) {
		addPath(path, polytype, false);
	}

	public final void addPath(Path64 path, PathType polytype, boolean isOpen) {
		super.AddPath(path, polytype, isOpen);
	}

	public final void addPaths(Paths64 paths, PathType polytype) {
		addPaths(paths, polytype, false);
	}

	public final void addPaths(Paths64 paths, PathType polytype, boolean isOpen) {
		super.AddPaths(paths, polytype, isOpen);
	}

	public final void addSubject(Paths64 paths) {
		addPaths(paths, PathType.Subject);
	}

	public final void addOpenSubject(Paths64 paths) {
		addPaths(paths, PathType.Subject, true);
	}

	public final void addClip(Paths64 paths) {
		addPaths(paths, PathType.Clip);
	}

	/**
	 * Once subject and clip paths have been assigned (via
	 * {@link #addSubject(Paths64) addSubject()}, {@link #addOpenSubject(Paths64)
	 * addOpenSubject()} and {@link #addClip(Paths64) addClip()} methods),
	 * <code>Execute()</code> can then perform the specified clipping operation
	 * (intersection, union, difference or XOR).
	 * <p>
	 * The solution parameter can be either a Paths64 or a PolyTree64, though since
	 * the Paths64 structure is simpler and more easily populated (with clipping
	 * about 5% faster), it should generally be preferred.
	 * <p>
	 * While polygons in solutions should never intersect (either with other
	 * polygons or with themselves), they will frequently be nested such that outer
	 * polygons will contain inner 'hole' polygons with in turn may contain outer
	 * polygons (to any level of nesting). And given that PolyTree64 and PolyTreeD
	 * preserve these parent-child relationships, these two PolyTree classes will be
	 * very useful to some users.
	 */
	public final boolean Execute(ClipType clipType, FillRule fillRule, Paths64 solutionClosed, Paths64 solutionOpen) {
		solutionClosed.clear();
		solutionOpen.clear();
		try {
			ExecuteInternal(clipType, fillRule);
			BuildPaths(solutionClosed, solutionOpen);
		} catch (Exception e) {
			succeeded = false;
		}

		ClearSolutionOnly();
		return succeeded;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, Paths64 solutionClosed) {
		return Execute(clipType, fillRule, solutionClosed, new Paths64());
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree, Paths64 openPaths) {
		polytree.Clear();
		openPaths.clear();
		usingPolytree = true;
		try {
			ExecuteInternal(clipType, fillRule);
			BuildTree(polytree, openPaths);
		} catch (Exception e) {
			succeeded = false;
		}

		ClearSolutionOnly();
		return succeeded;
	}

	public final boolean Execute(ClipType clipType, FillRule fillRule, PolyTree64 polytree) {
		return Execute(clipType, fillRule, polytree, new Paths64());
	}

}