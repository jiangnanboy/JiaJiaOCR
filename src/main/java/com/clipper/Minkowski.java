package com.clipper;

import com.clipper.core.FillRule;
import com.clipper.core.Path64;
import com.clipper.core.PathD;
import com.clipper.core.Paths64;
import com.clipper.core.PathsD;
import com.clipper.core.Point64;

public class Minkowski {

	public static Paths64 Sum(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, true, isClosed), FillRule.NonZero);
	}

	public static PathsD Sum(PathD pattern, PathD path, boolean isClosed) {
		return Sum(pattern, path, isClosed, 2);
	}

	public static PathsD Sum(PathD pattern, PathD path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), true, isClosed), FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	public static Paths64 Diff(Path64 pattern, Path64 path, boolean isClosed) {
		return Clipper.Union(MinkowskiInternal(pattern, path, false, isClosed), FillRule.NonZero);
	}

	public static PathsD Diff(PathD pattern, PathD path, boolean isClosed) {
		return Diff(pattern, path, isClosed, 2);
	}

	public static PathsD Diff(PathD pattern, PathD path, boolean isClosed, int decimalPlaces) {
		double scale = Math.pow(10, decimalPlaces);
		Paths64 tmp = Clipper.Union(
				MinkowskiInternal(Clipper.ScalePath64(pattern, scale), Clipper.ScalePath64(path, scale), false, isClosed),
				FillRule.NonZero);
		return Clipper.ScalePathsD(tmp, 1 / scale);
	}

	private static Paths64 MinkowskiInternal(Path64 pattern, Path64 path, boolean isSum, boolean isClosed) {
		int delta = isClosed ? 0 : 1;
		int patLen = pattern.size(), pathLen = path.size();
		Paths64 tmp = new Paths64(pathLen);

		for (Point64 pathPt : path) {
			Path64 path2 = new Path64(patLen);
			if (isSum) {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opAdd(pathPt, basePt));
				}
			} else {
				for (Point64 basePt : pattern) {
					path2.add(Point64.opSubtract(pathPt, basePt));
				}
			}
			tmp.add(path2);
		}

		Paths64 result = new Paths64((pathLen - delta) * patLen);
		int g = isClosed ? pathLen - 1 : 0;

		int h = patLen - 1;
		for (int i = delta; i < pathLen; i++) {
			for (int j = 0; j < patLen; j++) {
				Path64 quad = new Path64(tmp.get(g).get(h), tmp.get(i).get(h), tmp.get(i).get(j), tmp.get(g).get(j));
				if (!Clipper.IsPositive(quad)) {
					result.add(Clipper.ReversePath(quad));
				} else {
					result.add(quad);
				}
				h = j;
			}
			g = i;
		}
		return result;
	}

}