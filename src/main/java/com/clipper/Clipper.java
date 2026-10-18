package com.clipper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.clipper.offset.JoinType;
import com.clipper.rectclip.RectClip;
import com.clipper.rectclip.RectClipLines;
import com.clipper.core.ClipType;
import com.clipper.core.FillRule;
import com.clipper.core.InternalClipper;
import com.clipper.core.Path64;
import com.clipper.core.PathD;
import com.clipper.core.PathType;
import com.clipper.core.Paths64;
import com.clipper.core.PathsD;
import com.clipper.core.Point64;
import com.clipper.core.PointD;
import com.clipper.core.Rect64;
import com.clipper.core.RectD;
import com.clipper.engine.Clipper64;
import com.clipper.engine.ClipperD;
import com.clipper.engine.PointInPolygonResult;
import com.clipper.engine.PolyPath64;
import com.clipper.engine.PolyPathBase;
import com.clipper.engine.PolyPathD;
import com.clipper.engine.PolyTree64;
import com.clipper.engine.PolyTreeD;
import com.clipper.offset.ClipperOffset;
import com.clipper.offset.EndType;

public final class Clipper {

	public static final Rect64 InvalidRect64 = new Rect64(false);

	public static final RectD InvalidRectD = new RectD(false);

	public static Paths64 Intersect(Paths64 subject, Paths64 clip, FillRule fillRule) {
		return BooleanOp(ClipType.Intersection, subject, clip, fillRule);
	}

	public static PathsD Intersect(PathsD subject, PathsD clip, FillRule fillRule) {
		return Intersect(subject, clip, fillRule, 2);
	}

	public static PathsD Intersect(PathsD subject, PathsD clip, FillRule fillRule, int precision) {
		return BooleanOp(ClipType.Intersection, subject, clip, fillRule, precision);
	}

	public static Paths64 Union(Paths64 subject, FillRule fillRule) {
		return BooleanOp(ClipType.Union, subject, null, fillRule);
	}

	public static Paths64 Union(Paths64 subject, Paths64 clip, FillRule fillRule) {
		return BooleanOp(ClipType.Union, subject, clip, fillRule);
	}

	public static PathsD Union(PathsD subject, FillRule fillRule) {
		return BooleanOp(ClipType.Union, subject, null, fillRule);
	}

	public static PathsD Union(PathsD subject, PathsD clip, FillRule fillRule) {
		return Union(subject, clip, fillRule, 2);
	}

	public static PathsD Union(PathsD subject, PathsD clip, FillRule fillRule, int precision) {
		return BooleanOp(ClipType.Union, subject, clip, fillRule, precision);
	}

	public static Paths64 Difference(Paths64 subject, Paths64 clip, FillRule fillRule) {
		return BooleanOp(ClipType.Difference, subject, clip, fillRule);
	}

	public static PathsD Difference(PathsD subject, PathsD clip, FillRule fillRule) {
		return Difference(subject, clip, fillRule, 2);
	}

	public static PathsD Difference(PathsD subject, PathsD clip, FillRule fillRule, int precision) {
		return BooleanOp(ClipType.Difference, subject, clip, fillRule, precision);
	}

	public static Paths64 Xor(Paths64 subject, Paths64 clip, FillRule fillRule) {
		return BooleanOp(ClipType.Xor, subject, clip, fillRule);
	}

	public static PathsD Xor(PathsD subject, PathsD clip, FillRule fillRule) {
		return Xor(subject, clip, fillRule, 2);
	}

	public static PathsD Xor(PathsD subject, PathsD clip, FillRule fillRule, int precision) {
		return BooleanOp(ClipType.Xor, subject, clip, fillRule, precision);
	}

	public static Paths64 BooleanOp(ClipType clipType, Paths64 subject, Paths64 clip, FillRule fillRule) {
		Paths64 solution = new Paths64();
		if (subject == null) {
			return solution;
		}
		Clipper64 c = new Clipper64();
		c.AddPaths(subject, PathType.Subject);
		if (clip != null) {
			c.AddPaths(clip, PathType.Clip);
		}
		c.Execute(clipType, fillRule, solution);
		return solution;
	}

	public static void BooleanOp(ClipType clipType, @Nullable Paths64 subject, @Nullable Paths64 clip, PolyTree64 polytree,
			FillRule fillRule) {
		if (subject == null) {
			return;
		}
		Clipper64 c = new Clipper64();
		c.AddPaths(subject, PathType.Subject);
		if (clip != null) {
			c.AddPaths(clip, PathType.Clip);
		}
		c.Execute(clipType, fillRule, polytree);
	}

	public static PathsD BooleanOp(ClipType clipType, PathsD subject, PathsD clip, FillRule fillRule) {
		return BooleanOp(clipType, subject, clip, fillRule, 2);
	}

	public static PathsD BooleanOp(ClipType clipType, PathsD subject, @Nullable PathsD clip, FillRule fillRule, int precision) {
		PathsD solution = new PathsD();
		ClipperD c = new ClipperD(precision);
		c.AddSubjects(subject);
		if (clip != null) {
			c.AddClips(clip);
		}
		c.Execute(clipType, fillRule, solution);
		return solution;
	}

	public static void BooleanOp(ClipType clipType, @Nullable PathsD subject, @Nullable PathsD clip, PolyTreeD polytree,
			FillRule fillRule) {
		BooleanOp(clipType, subject, clip, polytree, fillRule, 2);
	}

	public static void BooleanOp(ClipType clipType, @Nullable PathsD subject, @Nullable PathsD clip, PolyTreeD polytree, FillRule fillRule,
			int precision) {
		if (subject == null) {
			return;
		}
		ClipperD c = new ClipperD(precision);
		c.AddPaths(subject, PathType.Subject);
		if (clip != null) {
			c.AddPaths(clip, PathType.Clip);
		}
		c.Execute(clipType, fillRule, polytree);
	}

	public static Paths64 InflatePaths(Paths64 paths, double delta, JoinType joinType, EndType endType) {
		return InflatePaths(paths, delta, joinType, endType, 2.0);
	}

	/**
	 * These functions encapsulate {@link ClipperOffset}, the class that performs
	 * both polygon and open path offsetting.
	 * <p>
	 * When using this function to inflate polygons (ie closed paths), it's
	 * important that you select {@link EndType#Polygon}. If instead you select one
	 * of the open path end types (including {@link EndType#Joined}), you'll inflate
	 * the polygon's outline.
	 * <p>
	 * With closed paths (polygons), a positive delta specifies how much outer
	 * polygon contours will expand and how much inner "hole" contours will contract
	 * (and the converse with negative deltas).
	 * <p>
	 * With open paths (polylines), including {@link EndType#Joined}, delta
	 * specifies the width of the inflated line.
	 * <p>
	 * Caution: Offsetting self-intersecting polygons may produce unexpected
	 * results.
	 *
	 * @param paths
	 * @param delta      With closed paths (polygons), a positive <code>delta</code>
	 *                   specifies how much outer polygon contours will expand and
	 *                   how much inner "hole" contours will contract (and the
	 *                   converse with negative deltas).
	 *                   <p>
	 *                   With open paths (polylines), including EndType.Join,
	 *                   <code>delta</code> specifies the width of the inflated
	 *                   line.
	 * @param joinType
	 * @param endType
	 * @param miterLimit sets the maximum distance in multiples of delta that
	 *                   vertices can be offset from their original positions before
	 *                   squaring is applied. (Squaring truncates a miter by
	 *                   'cutting it off' at 1 × delta distance from the original
	 *                   vertex.)
	 *                   <p>
	 *                   The default value for MiterLimit is 2 (ie twice delta).
	 *                   This is also the smallest MiterLimit that's allowed. If
	 *                   mitering was unrestricted (ie without any squaring), then
	 *                   offsets at very acute angles would generate unacceptably
	 *                   long 'spikes'.
	 * @return
	 */
	public static Paths64 InflatePaths(Paths64 paths, double delta, JoinType joinType, EndType endType, double miterLimit) {
		ClipperOffset co = new ClipperOffset(miterLimit);
		co.AddPaths(paths, joinType, endType);
		Paths64 solution = new Paths64();
		co.Execute(delta, solution);
		return solution;
	}

	public static PathsD InflatePaths(PathsD paths, double delta, JoinType joinType, EndType endType, double miterLimit) {
		return InflatePaths(paths, delta, joinType, endType, miterLimit, 2);
	}

	public static PathsD InflatePaths(PathsD paths, double delta, JoinType joinType, EndType endType) {
		return InflatePaths(paths, delta, joinType, endType, 2.0, 2);
	}

	public static PathsD InflatePaths(PathsD paths, double delta, JoinType joinType, EndType endType, double miterLimit, int precision) {
		InternalClipper.CheckPrecision(precision);
		double scale = Math.pow(10, precision);
		Paths64 tmp = ScalePaths64(paths, scale);
		ClipperOffset co = new ClipperOffset(miterLimit);
		co.AddPaths(tmp, joinType, endType);
		co.Execute(delta * scale, tmp); // reuse 'tmp' to receive (scaled) solution
		return ScalePathsD(tmp, 1 / scale);
	}

	public static Paths64 ExecuteRectClip(Rect64 rect, Paths64 paths) {
		return ExecuteRectClip(rect, paths, false);
	}

	public static Paths64 ExecuteRectClip(Rect64 rect, Paths64 paths, boolean convexOnly) {
		if (rect.IsEmpty() || paths.size() == 0) {
			return new Paths64();
		}
		RectClip rc = new RectClip(rect);
		return rc.Execute(paths, convexOnly);
	}

	public static Paths64 ExecuteRectClip(Rect64 rect, Path64 path) {
		return ExecuteRectClip(rect, path, false);
	}

	public static Paths64 ExecuteRectClip(Rect64 rect, Path64 path, boolean convexOnly) {
		if (rect.IsEmpty() || path.size() == 0) {
			return new Paths64();
		}
		Paths64 tmp = new Paths64();
		tmp.add(path);
		return ExecuteRectClip(rect, tmp, convexOnly);
	}

	public static PathsD ExecuteRectClip(RectD rect, PathsD paths) {
		return ExecuteRectClip(rect, paths, 2, false);
	}

	public static PathsD ExecuteRectClip(RectD rect, PathsD paths, int precision, boolean convexOnly) {
		InternalClipper.CheckPrecision(precision);
		if (rect.IsEmpty() || paths.size() == 0) {
			return new PathsD();
		}
		double scale = Math.pow(10, precision);
		Rect64 r = ScaleRect(rect, scale);
		Paths64 tmpPath = ScalePaths64(paths, scale);
		RectClip rc = new RectClip(r);
		tmpPath = rc.Execute(tmpPath, convexOnly);
		return ScalePathsD(tmpPath, 1 / scale);
	}

	public static PathsD ExecuteRectClip(RectD rect, PathD path) {
		return ExecuteRectClip(rect, path, 2, false);
	}

	public static PathsD ExecuteRectClip(RectD rect, PathD path, int precision, boolean convexOnly) {
		if (rect.IsEmpty() || path.size() == 0) {
			return new PathsD();
		}
		PathsD tmp = new PathsD();
		tmp.add(path);
		return ExecuteRectClip(rect, tmp, precision, convexOnly);
	}

	public static Paths64 ExecuteRectClipLines(Rect64 rect, Paths64 paths) {
		if (rect.IsEmpty() || paths.size() == 0) {
			return new Paths64();
		}
		RectClipLines rc = new RectClipLines(rect);
		return rc.Execute(paths);
	}

	public static Paths64 ExecuteRectClipLines(Rect64 rect, Path64 path) {
		if (rect.IsEmpty() || path.size() == 0) {
			return new Paths64();
		}
		Paths64 tmp = new Paths64();
		tmp.add(path);
		return ExecuteRectClipLines(rect, tmp);
	}

	public static PathsD ExecuteRectClipLines(RectD rect, PathsD paths) {
		return ExecuteRectClipLines(rect, paths, 2);
	}

	public static PathsD ExecuteRectClipLines(RectD rect, PathsD paths, int precision) {
		InternalClipper.CheckPrecision(precision);
		if (rect.IsEmpty() || paths.size() == 0) {
			return new PathsD();
		}
		double scale = Math.pow(10, precision);
		Rect64 r = ScaleRect(rect, scale);
		Paths64 tmpPath = ScalePaths64(paths, scale);
		RectClipLines rc = new RectClipLines(r);
		tmpPath = rc.Execute(tmpPath);
		return ScalePathsD(tmpPath, 1 / scale);
	}

	public static PathsD ExecuteRectClipLines(RectD rect, PathD path) {
		return ExecuteRectClipLines(rect, path, 2);
	}

	public static PathsD ExecuteRectClipLines(RectD rect, PathD path, int precision) {
		if (rect.IsEmpty() || path.size() == 0) {
			return new PathsD();
		}
		PathsD tmp = new PathsD();
		tmp.add(path);
		return ExecuteRectClipLines(rect, tmp, precision);
	}

	public static Paths64 MinkowskiSum(Path64 pattern, Path64 path, boolean isClosed) {
		return Minkowski.Sum(pattern, path, isClosed);
	}

	public static PathsD MinkowskiSum(PathD pattern, PathD path, boolean isClosed) {
		return Minkowski.Sum(pattern, path, isClosed);
	}

	public static Paths64 MinkowskiDiff(Path64 pattern, Path64 path, boolean isClosed) {
		return Minkowski.Diff(pattern, path, isClosed);
	}

	public static PathsD MinkowskiDiff(PathD pattern, PathD path, boolean isClosed) {
		return Minkowski.Diff(pattern, path, isClosed);
	}

	/**
	 * Returns the area of the supplied polygon. It's assumed that the path is
	 * closed and does not self-intersect. Depending on the path's winding
	 * orientation, this value may be positive or negative. If the winding is
	 * clockwise, then the area will be positive and conversely, if winding is
	 * counter-clockwise, then the area will be negative.
	 *
	 * @param path
	 * @return
	 */
	public static double Area(Path64 path) {
		// https://en.wikipedia.org/wiki/Shoelace_formula
		double a = 0.0;
		int cnt = path.size();
		if (cnt < 3) {
			return 0.0;
		}
		Point64 prevPt = path.get(cnt - 1);
		for (Point64 pt : path) {
			a += (double) (prevPt.y + pt.y) * (prevPt.x - pt.x);
			prevPt = pt;
		}
		return a * 0.5;
	}

	/**
	 * Returns the area of the supplied polygon. It's assumed that the path is
	 * closed and does not self-intersect. Depending on the path's winding
	 * orientation, this value may be positive or negative. If the winding is
	 * clockwise, then the area will be positive and conversely, if winding is
	 * counter-clockwise, then the area will be negative.
	 *
	 * @param paths
	 * @return
	 */
	public static double Area(Paths64 paths) {
		double a = 0.0;
		for (Path64 path : paths) {
			a += Area(path);
		}
		return a;
	}

	public static double Area(PathD path) {
		double a = 0.0;
		int cnt = path.size();
		if (cnt < 3) {
			return 0.0;
		}
		PointD prevPt = path.get(cnt - 1);
		for (PointD pt : path) {
			a += (prevPt.y + pt.y) * (prevPt.x - pt.x);
			prevPt = pt;
		}
		return a * 0.5;
	}

	public static double Area(PathsD paths) {
		double a = 0.0;
		for (PathD path : paths) {
			a += Area(path);
		}
		return a;
	}

	/**
	 * This function assesses the winding orientation of closed paths.
	 * <p>
	 * Positive winding paths will be oriented in an anti-clockwise direction in
	 * Cartesian coordinates (where coordinate values increase when heading
	 * rightward and upward). Nevertheless it's common for graphics libraries to use
	 * inverted Y-axes (where Y values decrease heading upward). In these libraries,
	 * paths with Positive winding will be oriented clockwise.
	 * <p>
	 * Note: Self-intersecting polygons have indeterminate orientation since some
	 * path segments will commonly wind in opposite directions to other segments.
	 */
	public static boolean IsPositive(Path64 poly) {
		return Area(poly) >= 0;
	}

	/**
	 * This function assesses the winding orientation of closed paths.
	 * <p>
	 * Positive winding paths will be oriented in an anti-clockwise direction in
	 * Cartesian coordinates (where coordinate values increase when heading
	 * rightward and upward). Nevertheless it's common for graphics libraries to use
	 * inverted Y-axes (where Y values decrease heading upward). In these libraries,
	 * paths with Positive winding will be oriented clockwise.
	 * <p>
	 * Note: Self-intersecting polygons have indeterminate orientation since some
	 * path segments will commonly wind in opposite directions to other segments.
	 */
	public static boolean IsPositive(PathD poly) {
		return Area(poly) >= 0;
	}

	public static String Path64ToString(Path64 path) {
		StringBuilder bld = new StringBuilder();
		for (Point64 pt : path) {
			bld.append(pt.toString());
		}
		return bld.toString() + '\n';
	}

	public static String Paths64ToString(Paths64 paths) {
		StringBuilder bld = new StringBuilder();
		for (Path64 path : paths) {
			bld.append(Path64ToString(path));
		}
		return bld.toString();
	}

	public static String PathDToString(PathD path) {
		StringBuilder bld = new StringBuilder();
		for (PointD pt : path) {
			bld.append(pt.toString());
		}
		return bld.toString() + '\n';
	}

	public static String PathsDToString(PathsD paths) {
		StringBuilder bld = new StringBuilder();
		for (PathD path : paths) {
			bld.append(PathDToString(path));
		}
		return bld.toString();
	}

	public static Path64 OffsetPath(Path64 path, long dx, long dy) {
		Path64 result = new Path64(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.x + dx, pt.y + dy));
		}
		return result;
	}

	public static Point64 ScalePoint64(Point64 pt, double scale) {
		Point64 result = new Point64();
		result.x = (long) (pt.x * scale);
		result.y = (long) (pt.y * scale);
		return result;
	}

	public static PointD ScalePointD(Point64 pt, double scale) {
		PointD result = new PointD();
		result.x = pt.x * scale;
		result.y = pt.y * scale;
		return result;
	}

	public static Rect64 ScaleRect(RectD rec, double scale) {
		Rect64 result = new Rect64((long) (rec.left * scale), (long) (rec.top * scale), (long) (rec.right * scale),
				(long) (rec.bottom * scale));
		return result;
	}

	public static Path64 ScalePath(Path64 path, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return path;
		}
		Path64 result = new Path64(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.x * scale, pt.y * scale));
		}
		return result;
	}

	public static Paths64 ScalePaths(Paths64 paths, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return paths;
		}
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			result.add(ScalePath(path, scale));
		}
		return result;
	}

	public static PathD ScalePath(PathD path, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return path;
		}
		PathD result = new PathD(path.size());
		for (PointD pt : path) {
			result.add(new PointD(pt, scale));
		}
		return result;
	}

	public static PathsD ScalePaths(PathsD paths, double scale) {
		if (InternalClipper.IsAlmostZero(scale - 1)) {
			return paths;
		}
		PathsD result = new PathsD(paths.size());
		for (PathD path : paths) {
			result.add(ScalePath(path, scale));
		}
		return result;
	}

	// Unlike ScalePath, both ScalePath64 & ScalePathD also involve type conversion
	public static Path64 ScalePath64(PathD path, double scale) {
		int cnt = path.size();
		Path64 res = new Path64(cnt);
		for (PointD pt : path) {
			res.add(new Point64(pt, scale));
		}
		return res;
	}

	public static Paths64 ScalePaths64(PathsD paths, double scale) {
		int cnt = paths.size();
		Paths64 res = new Paths64(cnt);
		for (PathD path : paths) {
			res.add(ScalePath64(path, scale));
		}
		return res;
	}

	public static PathD ScalePathD(Path64 path, double scale) {
		int cnt = path.size();
		PathD res = new PathD(cnt);
		for (Point64 pt : path) {
			res.add(new PointD(pt, scale));
		}
		return res;
	}

	public static PathsD ScalePathsD(Paths64 paths, double scale) {
		int cnt = paths.size();
		PathsD res = new PathsD(cnt);
		for (Path64 path : paths) {
			res.add(ScalePathD(path, scale));
		}
		return res;
	}

	// The static functions Path64 and PathD convert path types without scaling
	public static Path64 Path64(PathD path) {
		Path64 result = new Path64(path.size());
		for (PointD pt : path) {
			result.add(new Point64(pt));
		}
		return result;
	}

	public static Paths64 Paths64(PathsD paths) {
		Paths64 result = new Paths64(paths.size());
		for (PathD path : paths) {
			result.add(Path64(path));
		}
		return result;
	}

	public static PathsD PathsD(Paths64 paths) {
		PathsD result = new PathsD(paths.size());
		for (Path64 path : paths) {
			result.add(PathD(path));
		}
		return result;
	}

	public static PathD PathD(Path64 path) {
		PathD result = new PathD(path.size());
		for (Point64 pt : path) {
			result.add(new PointD(pt));
		}
		return result;
	}

	public static Path64 TranslatePath(Path64 path, long dx, long dy) {
		Path64 result = new Path64(path.size());
		for (Point64 pt : path) {
			result.add(new Point64(pt.x + dx, pt.y + dy));
		}
		return result;
	}

	public static Paths64 TranslatePaths(Paths64 paths, long dx, long dy) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			result.add(OffsetPath(path, dx, dy));
		}
		return result;
	}

	public static PathD TranslatePath(PathD path, double dx, double dy) {
		PathD result = new PathD(path.size());
		for (PointD pt : path) {
			result.add(new PointD(pt.x + dx, pt.y + dy));
		}
		return result;
	}

	public static PathsD TranslatePaths(PathsD paths, double dx, double dy) {
		PathsD result = new PathsD(paths.size());
		for (PathD path : paths) {
			result.add(TranslatePath(path, dx, dy));
		}
		return result;
	}

	public static Path64 ReversePath(Path64 path) {
		Path64 result = new Path64(path);
		Collections.reverse(result);
		return result;
	}

	public static PathD ReversePath(PathD path) {
		PathD result = new PathD(path);
		Collections.reverse(result);
		return result;
	}

	public static Paths64 ReversePaths(Paths64 paths) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 t : paths) {
			result.add(ReversePath(t));
		}

		return result;
	}

	public static PathsD ReversePaths(PathsD paths) {
		PathsD result = new PathsD(paths.size());
		for (PathD path : paths) {
			result.add(ReversePath(path));
		}
		return result;
	}

	public static Rect64 GetBounds(Path64 path) {
		Rect64 result = InvalidRect64.clone();
		for (Point64 pt : path) {
			if (pt.x < result.left) {
				result.left = pt.x;
			}
			if (pt.x > result.right) {
				result.right = pt.x;
			}
			if (pt.y < result.top) {
				result.top = pt.y;
			}
			if (pt.y > result.bottom) {
				result.bottom = pt.y;
			}
		}
		return result.left == Long.MAX_VALUE ? new Rect64() : result;
	}

	public static Rect64 GetBounds(Paths64 paths) {
		Rect64 result = InvalidRect64.clone();
		for (Path64 path : paths) {
			for (Point64 pt : path) {
				if (pt.x < result.left) {
					result.left = pt.x;
				}
				if (pt.x > result.right) {
					result.right = pt.x;
				}
				if (pt.y < result.top) {
					result.top = pt.y;
				}
				if (pt.y > result.bottom) {
					result.bottom = pt.y;
				}
			}
		}
		return result.left == Long.MAX_VALUE ? new Rect64() : result;
	}

	public static RectD GetBounds(PathD path) {
		RectD result = InvalidRectD.clone();
		for (PointD pt : path) {
			if (pt.x < result.left) {
				result.left = pt.x;
			}
			if (pt.x > result.right) {
				result.right = pt.x;
			}
			if (pt.y < result.top) {
				result.top = pt.y;
			}
			if (pt.y > result.bottom) {
				result.bottom = pt.y;
			}
		}
		return result.left == Double.MAX_VALUE ? new RectD() : result;
	}

	public static RectD GetBounds(PathsD paths) {
		RectD result = InvalidRectD.clone();
		for (PathD path : paths) {
			for (PointD pt : path) {
				if (pt.x < result.left) {
					result.left = pt.x;
				}
				if (pt.x > result.right) {
					result.right = pt.x;
				}
				if (pt.y < result.top) {
					result.top = pt.y;
				}
				if (pt.y > result.bottom) {
					result.bottom = pt.y;
				}
			}
		}
		return result.left == Double.MAX_VALUE ? new RectD() : result;
	}

	public static Path64 MakePath(int[] arr) {
		int len = arr.length / 2;
		Path64 p = new Path64(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static Path64 MakePath(long[] arr) {
		int len = arr.length / 2;
		Path64 p = new Path64(len);
		for (int i = 0; i < len; i++) {
			p.add(new Point64(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static PathD MakePath(double[] arr) {
		int len = arr.length / 2;
		PathD p = new PathD(len);
		for (int i = 0; i < len; i++) {
			p.add(new PointD(arr[i * 2], arr[i * 2 + 1]));
		}
		return p;
	}

	public static double Sqr(double value) {
		return value * value;
	}

	public static boolean PointsNearEqual(PointD pt1, PointD pt2, double distanceSqrd) {
		return Sqr(pt1.x - pt2.x) + Sqr(pt1.y - pt2.y) < distanceSqrd;
	}

	public static PathD StripNearDuplicates(PathD path, double minEdgeLenSqrd, boolean isClosedPath) {
		int cnt = path.size();
		PathD result = new PathD(cnt);
		if (cnt == 0) {
			return result;
		}
		PointD lastPt = path.get(0);
		result.add(lastPt);
		for (int i = 1; i < cnt; i++) {
			if (!PointsNearEqual(lastPt, path.get(i), minEdgeLenSqrd)) {
				lastPt = path.get(i);
				result.add(lastPt);
			}
		}

		if (isClosedPath && PointsNearEqual(lastPt, result.get(0), minEdgeLenSqrd)) {
			result.remove(result.size() - 1);
		}

		return result;
	}

	public static Path64 StripDuplicates(Path64 path, boolean isClosedPath) {
		int cnt = path.size();
		Path64 result = new Path64(cnt);
		if (cnt == 0) {
			return result;
		}
		Point64 lastPt = path.get(0);
		result.add(lastPt);
		for (int i = 1; i < cnt; i++) {
			if (!path.get(i).equals(lastPt)) {
				lastPt = path.get(i);
				result.add(lastPt);
			}
		}
		if (isClosedPath && result.get(0).equals(lastPt)) {
			result.remove(result.size() - 1);
		}
		return result;
	}

	private static void AddPolyNodeToPaths(PolyPath64 polyPath, Paths64 paths) {
		if (!polyPath.getPolygon().isEmpty()) {
			paths.add(polyPath.getPolygon());
		}
		polyPath.iterator().forEachRemaining(p -> AddPolyNodeToPaths((PolyPath64) p, paths));
	}

	public static Paths64 PolyTreeToPaths64(PolyTree64 polyTree) {
		Paths64 result = new Paths64();
		polyTree.iterator().forEachRemaining(p -> AddPolyNodeToPaths((PolyPath64) p, result));
		return result;
	}

	public static void AddPolyNodeToPathsD(PolyPathD polyPath, PathsD paths) {
		if (!polyPath.getPolygon().isEmpty()) {
			paths.add(polyPath.getPolygon());
		}
		polyPath.iterator().forEachRemaining(p -> AddPolyNodeToPathsD((PolyPathD) p, paths));
	}

	public static PathsD PolyTreeToPathsD(PolyTreeD polyTree) {
		PathsD result = new PathsD();
		for (PolyPathBase polyPathBase : polyTree) {
			PolyPathD p = (PolyPathD) polyPathBase;
			AddPolyNodeToPathsD(p, result);
		}

		return result;
	}

	public static double PerpendicDistFromLineSqrd(PointD pt, PointD line1, PointD line2) {
		double a = pt.x - line1.x;
		double b = pt.y - line1.y;
		double c = line2.x - line1.x;
		double d = line2.y - line1.y;
		if (c == 0 && d == 0) {
			return 0;
		}
		return Sqr(a * d - c * b) / (c * c + d * d);
	}

	public static double PerpendicDistFromLineSqrd(Point64 pt, Point64 line1, Point64 line2) {
		double a = (double) pt.x - line1.x;
		double b = (double) pt.y - line1.y;
		double c = (double) line2.x - line1.x;
		double d = (double) line2.y - line1.y;
		if (c == 0 && d == 0) {
			return 0;
		}
		return Sqr(a * d - c * b) / (c * c + d * d);
	}

	public static void RDP(Path64 path, int begin, int end, double epsSqrd, List<Boolean> flags) {
		int idx = 0;
		double maxD = 0;
		while (end > begin && path.get(begin).equals(path.get(end))) {
			flags.set(end--, false);
		}
		for (int i = begin + 1; i < end; ++i) {
			// PerpendicDistFromLineSqrd - avoids expensive Sqrt()
			double d = PerpendicDistFromLineSqrd(path.get(i), path.get(begin), path.get(end));
			if (d <= maxD) {
				continue;
			}
			maxD = d;
			idx = i;
		}
		if (maxD <= epsSqrd) {
			return;
		}
		flags.set(idx, true);
		if (idx > begin + 1) {
			RDP(path, begin, idx, epsSqrd, flags);
		}
		if (idx < end - 1) {
			RDP(path, idx, end, epsSqrd, flags);
		}
	}

	/**
	 * The Ramer-Douglas-Peucker algorithm is very useful in removing path segments
	 * that don't contribute meaningfully to the path's shape. The algorithm's
	 * aggressiveness is determined by the epsilon parameter, with larger values
	 * removing more vertices. (Somewhat simplistically, the algorithm removes
	 * vertices that are less than epsilon distance from imaginary lines passing
	 * through their adjacent vertices.)
	 * <p>
	 * This function can be particularly useful when offsetting paths (ie
	 * inflating/shrinking) where the offsetting process often creates tiny
	 * segments. These segments don't enhance curve quality, but they will slow path
	 * processing (whether during file storage, or when rendering, or in subsequent
	 * offsetting procedures).
	 *
	 * @param path
	 * @param epsilon
	 * @return
	 */
	public static Path64 RamerDouglasPeuckerPath(Path64 path, double epsilon) {
		int len = path.size();
		if (len < 5) {
			return path;
		}
		List<Boolean> flags = new ArrayList<>(Arrays.asList(new Boolean[len]));
		flags.set(0, true);
		flags.set(len - 1, true);
		RDP(path, 0, len - 1, Sqr(epsilon), flags);
		Path64 result = new Path64(len);
		for (int i = 0; i < len; ++i) {
			if (flags.get(i).booleanValue()) {
				result.add(path.get(i));
			}
		}
		return result;
	}

	/**
	 * The Ramer-Douglas-Peucker algorithm is very useful in removing path segments
	 * that don't contribute meaningfully to the path's shape. The algorithm's
	 * aggressiveness is determined by the epsilon parameter, with larger values
	 * removing more vertices. (Somewhat simplistically, the algorithm removes
	 * vertices that are less than epsilon distance from imaginary lines passing
	 * through their adjacent vertices.)
	 * <p>
	 * This function can be particularly useful when offsetting paths (ie
	 * inflating/shrinking) where the offsetting process often creates tiny
	 * segments. These segments don't enhance curve quality, but they will slow path
	 * processing (whether during file storage, or when rendering, or in subsequent
	 * offsetting procedures).
	 *
	 * @param paths
	 * @param epsilon
	 * @return
	 */
	public static Paths64 RamerDouglasPeucker(Paths64 paths, double epsilon) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			result.add(RamerDouglasPeuckerPath(path, epsilon));
		}
		return result;
	}

	public static void RDP(PathD path, int begin, int end, double epsSqrd, List<Boolean> flags) {
		int idx = 0;
		double maxD = 0;
		while (end > begin && path.get(begin).equals(path.get(end))) {
			flags.set(end--, false);
		}
		for (int i = begin + 1; i < end; ++i) {
			// PerpendicDistFromLineSqrd - avoids expensive Sqrt()
			double d = PerpendicDistFromLineSqrd(path.get(i), path.get(begin), path.get(end));
			if (d <= maxD) {
				continue;
			}
			maxD = d;
			idx = i;
		}
		if (maxD <= epsSqrd) {
			return;
		}
		flags.set(idx, true);
		if (idx > begin + 1) {
			RDP(path, begin, idx, epsSqrd, flags);
		}
		if (idx < end - 1) {
			RDP(path, idx, end, epsSqrd, flags);
		}
	}

	public static PathD RamerDouglasPeucker(PathD path, double epsilon) {
		int len = path.size();
		if (len < 5) {
			return path;
		}
		List<Boolean> flags = new ArrayList<>(Arrays.asList(new Boolean[len]));
		flags.set(0, true);
		flags.set(len - 1, true);
		RDP(path, 0, len - 1, Sqr(epsilon), flags);
		PathD result = new PathD(len);
		for (int i = 0; i < len; ++i) {
			if (flags.get(i).booleanValue()) {
				result.add(path.get(i));
			}
		}
		return result;
	}

	public static PathsD RamerDouglasPeucker(PathsD paths, double epsilon) {
		PathsD result = new PathsD(paths.size());
		for (PathD path : paths) {
			result.add(RamerDouglasPeucker(path, epsilon));
		}
		return result;
	}

	private static int GetNext(int current, int high, final /* ref */ boolean[] flags) {
		++current;
		while (current <= high && flags[current]) {
			++current;
		}
		if (current <= high) {
			return current;
		}
		current = 0;
		while (flags[current]) {
			++current;
		}
		return current;
	}

	private static int GetPrior(int current, int high, final /* ref */ boolean[] flags) {
		if (current == 0) {
			current = high;
		} else {
			--current;
		}
		while (current > 0 && flags[current]) {
			--current;
		}
		if (!flags[current]) {
			return current;
		}
		current = high;
		while (flags[current]) {
			--current;
		}
		return current;
	}

	public static Path64 SimplifyPath(Path64 path, double epsilon) {
		return SimplifyPath(path, epsilon, false);
	}

	public static Path64 SimplifyPath(Path64 path, double epsilon, boolean isOpenPath) {
		int len = path.size(), high = len - 1;
		double epsSqr = Sqr(epsilon);
		if (len < 4) {
			return path;
		}

		boolean[] flags = new boolean[len];
		double[] dsq = new double[len];
		int prev = high, curr = 0, start, next, prior2, next2;
		if (isOpenPath) {
			dsq[0] = Double.MAX_VALUE;
			dsq[high] = Double.MAX_VALUE;
		} else {
			dsq[0] = PerpendicDistFromLineSqrd(path.get(0), path.get(high), path.get(1));
			dsq[high] = PerpendicDistFromLineSqrd(path.get(high), path.get(0), path.get(high - 1));
		}
		for (int i = 1; i < high; ++i) {
			dsq[i] = PerpendicDistFromLineSqrd(path.get(i), path.get(i - 1), path.get(i + 1));
		}

		for (;;) {
			if (dsq[curr] > epsSqr) {
				start = curr;
				do {
					curr = GetNext(curr, high, /* ref */ flags);
				} while (curr != start && dsq[curr] > epsSqr);
				if (curr == start) {
					break;
				}
			}

			prev = GetPrior(curr, high, /* ref */ flags);
			next = GetNext(curr, high, /* ref */ flags);
			if (next == prev) {
				break;
			}

			if (dsq[next] < dsq[curr]) {
				flags[next] = true;
				next = GetNext(next, high, /* ref */ flags);
				next2 = GetNext(next, high, /* ref */ flags);
				dsq[curr] = PerpendicDistFromLineSqrd(path.get(curr), path.get(prev), path.get(next));
				if (next != high || !isOpenPath) {
					dsq[next] = PerpendicDistFromLineSqrd(path.get(next), path.get(curr), path.get(next2));
				}
				curr = next;
			} else {
				flags[curr] = true;
				curr = next;
				next = GetNext(next, high, /* ref */ flags);
				prior2 = GetPrior(prev, high, /* ref */ flags);
				dsq[curr] = PerpendicDistFromLineSqrd(path.get(curr), path.get(prev), path.get(next));
				if (prev != 0 || !isOpenPath) {
					dsq[prev] = PerpendicDistFromLineSqrd(path.get(prev), path.get(prior2), path.get(curr));
				}
			}
		}
		Path64 result = new Path64(len);
		for (int i = 0; i < len; i++) {
			if (!flags[i]) {
				result.add(path.get(i));
			}
		}
		return result;
	}

	public static Paths64 SimplifyPaths(Paths64 paths, double epsilon) {
		return SimplifyPaths(paths, epsilon, false);
	}

	public static Paths64 SimplifyPaths(Paths64 paths, double epsilon, boolean isOpenPath) {
		Paths64 result = new Paths64(paths.size());
		for (Path64 path : paths) {
			result.add(SimplifyPath(path, epsilon, isOpenPath));
		}
		return result;
	}

	public static PathD SimplifyPath(PathD path, double epsilon) {
		return SimplifyPath(path, epsilon, false);
	}

	public static PathD SimplifyPath(PathD path, double epsilon, boolean isOpenPath) {
		int len = path.size(), high = len - 1;
		double epsSqr = Sqr(epsilon);
		if (len < 4) {
			return path;
		}

		boolean[] flags = new boolean[len];
		double[] dsq = new double[len];
		int prev = high, curr = 0, start, next, prior2, next2;
		if (isOpenPath) {
			dsq[0] = Double.MAX_VALUE;
			dsq[high] = Double.MAX_VALUE;
		} else {
			dsq[0] = PerpendicDistFromLineSqrd(path.get(0), path.get(high), path.get(1));
			dsq[high] = PerpendicDistFromLineSqrd(path.get(high), path.get(0), path.get(high - 1));
		}
		for (int i = 1; i < high; ++i) {
			dsq[i] = PerpendicDistFromLineSqrd(path.get(i), path.get(i - 1), path.get(i + 1));
		}

		for (;;) {
			if (dsq[curr] > epsSqr) {
				start = curr;
				do {
					curr = GetNext(curr, high, /* ref */ flags);
				} while (curr != start && dsq[curr] > epsSqr);
				if (curr == start) {
					break;
				}
			}

			prev = GetPrior(curr, high, /* ref */ flags);
			next = GetNext(curr, high, /* ref */ flags);
			if (next == prev) {
				break;
			}

			if (dsq[next] < dsq[curr]) {
				flags[next] = true;
				next = GetNext(next, high, /* ref */ flags);
				next2 = GetNext(next, high, /* ref */ flags);
				dsq[curr] = PerpendicDistFromLineSqrd(path.get(curr), path.get(prev), path.get(next));
				if (next != high || !isOpenPath) {
					dsq[next] = PerpendicDistFromLineSqrd(path.get(next), path.get(curr), path.get(next2));
				}
				curr = next;
			} else {
				flags[curr] = true;
				curr = next;
				next = GetNext(next, high, /* ref */ flags);
				prior2 = GetPrior(prev, high, /* ref */ flags);
				dsq[curr] = PerpendicDistFromLineSqrd(path.get(curr), path.get(prev), path.get(next));
				if (prev != 0 || !isOpenPath) {
					dsq[prev] = PerpendicDistFromLineSqrd(path.get(prev), path.get(prior2), path.get(curr));
				}
			}
		}
		PathD result = new PathD(len);
		for (int i = 0; i < len; i++) {
			if (!flags[i]) {
				result.add(path.get(i));
			}
		}
		return result;
	}

	public static PathsD SimplifyPaths(PathsD paths, double epsilon) {
		return SimplifyPaths(paths, epsilon, false);
	}

	public static PathsD SimplifyPaths(PathsD paths, double epsilon, boolean isOpenPath) {
		PathsD result = new PathsD(paths.size());
		for (PathD path : paths) {
			result.add(SimplifyPath(path, epsilon, isOpenPath));
		}
		return result;
	}

	/**
	 * This function removes the vertices between adjacent collinear segments. It
	 * will also remove duplicate vertices (adjacent vertices with identical
	 * coordinates).
	 * <p>
	 * Note: Duplicate vertices will be removed automatically from clipping
	 * solutions, but not collinear edges unless the Clipper object's
	 * PreserveCollinear property had been disabled.
	 */
	public static Path64 TrimCollinear(Path64 path) {
		return TrimCollinear(path, false);
	}

	/**
	 * This function removes the vertices between adjacent collinear segments. It
	 * will also remove duplicate vertices (adjacent vertices with identical
	 * coordinates).
	 * <p>
	 * Note: Duplicate vertices will be removed automatically from clipping
	 * solutions, but not collinear edges unless the Clipper object's
	 * PreserveCollinear property had been disabled.
	 */
	public static Path64 TrimCollinear(Path64 path, boolean isOpen) {
		int len = path.size();
		int i = 0;
		if (!isOpen) {
			while (i < len - 1 && InternalClipper.CrossProduct(path.get(len - 1), path.get(i), path.get(i + 1)) == 0) {
				i++;
			}
			while (i < len - 1 && InternalClipper.CrossProduct(path.get(len - 2), path.get(len - 1), path.get(i)) == 0) {
				len--;
			}
		}

		if (len - i < 3) {
			if (!isOpen || len < 2 || path.get(0).equals(path.get(1))) {
				return new Path64();
			}
			return path;
		}

		Path64 result = new Path64(len - i);
		Point64 last = path.get(i);
		result.add(last);
		for (i++; i < len - 1; i++) {
			if (InternalClipper.CrossProduct(last, path.get(i), path.get(i + 1)) == 0) {
				continue;
			}
			last = path.get(i);
			result.add(last);
		}

		if (isOpen) {
			result.add(path.get(len - 1));
		} else if (InternalClipper.CrossProduct(last, path.get(len - 1), result.get(0)) != 0) {
			result.add(path.get(len - 1));
		} else {
			while (result.size() > 2
					&& InternalClipper.CrossProduct(result.get(result.size() - 1), result.get(result.size() - 2), result.get(0)) == 0) {
				result.remove(result.size() - 1);
			}
			if (result.size() < 3) {
				result.clear();
			}
		}
		return result;
	}

	/**
	 * This function removes the vertices between adjacent collinear segments. It
	 * will also remove duplicate vertices (adjacent vertices with identical
	 * coordinates).
	 * <p>
	 * With floating point paths, the precision parameter indicates the decimal
	 * precision that's required when determining collinearity.
	 * <p>
	 * Note: Duplicate vertices will be removed automatically from clipping
	 * solutions, but not collinear edges unless the Clipper object's
	 * PreserveCollinear property had been disabled.
	 */
	public static PathD TrimCollinear(PathD path, int precision) {
		return TrimCollinear(path, precision, false);
	}

	/**
	 * This function removes the vertices between adjacent collinear segments. It
	 * will also remove duplicate vertices (adjacent vertices with identical
	 * coordinates).
	 * <p>
	 * With floating point paths, the precision parameter indicates the decimal
	 * precision that's required when determining collinearity.
	 * <p>
	 * Note: Duplicate vertices will be removed automatically from clipping
	 * solutions, but not collinear edges unless the Clipper object's
	 * PreserveCollinear property had been disabled.
	 */
	public static PathD TrimCollinear(PathD path, int precision, boolean isOpen) {
		InternalClipper.CheckPrecision(precision);
		double scale = Math.pow(10, precision);
		Path64 p = ScalePath64(path, scale);
		p = TrimCollinear(p, isOpen);
		return ScalePathD(p, 1 / scale);
	}

	public static PointInPolygonResult PointInPolygon(Point64 pt, Path64 polygon) {
		return InternalClipper.PointInPolygon(pt, polygon);
	}

	public static PointInPolygonResult PointInPolygon(PointD pt, PathD polygon) {
		return PointInPolygon(pt, polygon, 2);
	}

	public static PointInPolygonResult PointInPolygon(PointD pt, PathD polygon, int precision) {
		InternalClipper.CheckPrecision(precision);
		double scale = Math.pow(10, precision);
		Point64 p = new Point64(pt, scale);
		Path64 path = ScalePath64(polygon, scale);
		return InternalClipper.PointInPolygon(p, path);
	}

	public static Path64 Ellipse(Point64 center, double radiusX, double radiusY) {
		return Ellipse(center, radiusX, radiusY, 0);
	}

	public static Path64 Ellipse(Point64 center, double radiusX) {
		return Ellipse(center, radiusX, 0, 0);
	}

	public static Path64 Ellipse(Point64 center, double radiusX, double radiusY, int steps) {
		if (radiusX <= 0) {
			return new Path64();
		}
		if (radiusY <= 0) {
			radiusY = radiusX;
		}
		if (steps <= 2) {
			steps = (int) Math.ceil(Math.PI * Math.sqrt((radiusX + radiusY) / 2));
		}

		double si = Math.sin(2 * Math.PI / steps);
		double co = Math.cos(2 * Math.PI / steps);
		double dx = co, dy = si;
		Path64 result = new Path64(steps);
		result.add(new Point64(center.x + radiusX, center.x));
		for (int i = 1; i < steps; ++i) {
			result.add(new Point64(center.x + radiusX * dx, center.y + radiusY * dy));
			double x = dx * co - dy * si;
			dy = dy * co + dx * si;
			dx = x;
		}
		return result;
	}

	public static PathD Ellipse(PointD center, double radiusX, double radiusY) {
		return Ellipse(center, radiusX, radiusY, 0);
	}

	public static PathD Ellipse(PointD center, double radiusX) {
		return Ellipse(center, radiusX, 0, 0);
	}

	public static PathD Ellipse(PointD center, double radiusX, double radiusY, int steps) {
		if (radiusX <= 0) {
			return new PathD();
		}
		if (radiusY <= 0) {
			radiusY = radiusX;
		}
		if (steps <= 2) {
			steps = (int) Math.ceil(Math.PI * Math.sqrt((radiusX + radiusY) / 2));
		}

		double si = Math.sin(2 * Math.PI / steps);
		double co = Math.cos(2 * Math.PI / steps);
		double dx = co, dy = si;
		PathD result = new PathD(steps);
		result.add(new PointD(center.x + radiusX, center.y));
		for (int i = 1; i < steps; ++i) {
			result.add(new PointD(center.x + radiusX * dx, center.y + radiusY * dy));
			double x = dx * co - dy * si;
			dy = dy * co + dx * si;
			dx = x;
		}
		return result;
	}

}