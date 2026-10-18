package com.clipper.engine;

import com.clipper.Clipper;
import com.clipper.core.ClipType;
import com.clipper.core.FillRule;
import com.clipper.core.Path64;
import com.clipper.core.PathD;
import com.clipper.core.PathType;
import com.clipper.core.Paths64;
import com.clipper.core.PathsD;

/**
 * The ClipperD class performs boolean 'clipping'. This class is very similar to
 * Clipper64 except that coordinates passed to ClipperD objects are of type
 * <code>double</code> instead of type <code>long</code>.
 */
public class ClipperD extends ClipperBase {

	private double scale;
	private double invScale;

	public ClipperD() {
		this(2);
	}

	/**
	 * @param roundingDecimalPrecision default = 2
	 */
	public ClipperD(int roundingDecimalPrecision) {
		if (roundingDecimalPrecision < -8 || roundingDecimalPrecision > 8) {
			throw new IllegalArgumentException("Error - RoundingDecimalPrecision exceeds the allowed range.");
		}
		scale = Math.pow(10, roundingDecimalPrecision);
		invScale = 1 / scale;
	}

	public void AddPath(PathD path, PathType polytype) {
		AddPath(path, polytype, false);
	}

	public void AddPath(PathD path, PathType polytype, boolean isOpen) {
		super.AddPath(Clipper.ScalePath64(path, scale), polytype, isOpen);
	}

	public void AddPaths(PathsD paths, PathType polytype) {
		AddPaths(paths, polytype, false);
	}

	public void AddPaths(PathsD paths, PathType polytype, boolean isOpen) {
		super.AddPaths(Clipper.ScalePaths64(paths, scale), polytype, isOpen);
	}

	public void AddSubject(PathD path) {
		AddPath(path, PathType.Subject);
	}

	public void AddOpenSubject(PathD path) {
		AddPath(path, PathType.Subject, true);
	}

	public void AddClip(PathD path) {
		AddPath(path, PathType.Clip);
	}

	public void AddSubjects(PathsD paths) {
		AddPaths(paths, PathType.Subject);
	}

	public void AddOpenSubjects(PathsD paths) {
		AddPaths(paths, PathType.Subject, true);
	}

	public void AddClips(PathsD paths) {
		AddPaths(paths, PathType.Clip);
	}

	public boolean Execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed, PathsD solutionOpen) {
		Paths64 solClosed64 = new Paths64(), solOpen64 = new Paths64();

		boolean success = true;
		solutionClosed.clear();
		solutionOpen.clear();
		try {
			ExecuteInternal(clipType, fillRule);
			BuildPaths(solClosed64, solOpen64);
		} catch (Exception e) {
			success = false;
		}

		ClearSolutionOnly();
		if (!success) {
			return false;
		}

		for (Path64 path : solClosed64) {
			solutionClosed.add(Clipper.ScalePathD(path, invScale));
		}
		for (Path64 path : solOpen64) {
			solutionOpen.add(Clipper.ScalePathD(path, invScale));
		}

		return true;
	}

	public boolean Execute(ClipType clipType, FillRule fillRule, PathsD solutionClosed) {
		return Execute(clipType, fillRule, solutionClosed, new PathsD());
	}

	public boolean Execute(ClipType clipType, FillRule fillRule, PolyTreeD polytree, PathsD openPaths) {
		polytree.Clear();
		polytree.setScale(scale);
		openPaths.clear();
		Paths64 oPaths = new Paths64();
		boolean success = true;
		try {
			ExecuteInternal(clipType, fillRule);
			BuildTree(polytree, oPaths);
		} catch (Exception e) {
			success = false;
		}
		ClearSolutionOnly();
		if (!success) {
			return false;
		}
		if (!oPaths.isEmpty()) {
			for (Path64 path : oPaths) {
				openPaths.add(Clipper.ScalePathD(path, invScale));
			}
		}

		return true;
	}

	public boolean Execute(ClipType clipType, FillRule fillRule, PolyTreeD polytree) {
		return Execute(clipType, fillRule, polytree, new PathsD());
	}

}