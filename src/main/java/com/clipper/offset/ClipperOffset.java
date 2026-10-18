package com.clipper.offset;

import com.clipper.Clipper;
import com.clipper.engine.Clipper64;
import com.clipper.engine.PolyTree64;
import com.clipper.tangible.OutObject;
import com.clipper.tangible.RefObject;
import com.clipper.core.ClipType;
import com.clipper.core.FillRule;
import com.clipper.core.InternalClipper;
import com.clipper.core.Path64;
import com.clipper.core.PathD;
import com.clipper.core.Paths64;
import com.clipper.core.Point64;
import com.clipper.core.PointD;
import com.clipper.core.Rect64;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Geometric offsetting refers to the process of creating parallel curves that
 * are offset a specified distance from their primary curves.
 * <p>
 * The ClipperOffset class manages the process of offsetting
 * (inflating/deflating) both open and closed paths using a number of different
 * join types and end types. The library user will rarely need to access this
 * unit directly since it will generally be easier to use the
 * {@link Clipper#InflatePaths(Paths64, double, JoinType, EndType)
 * InflatePaths()} function when doing polygon offsetting.
 * <p>
 * Caution: Offsetting self-intersecting polygons may produce unexpected
 * results.
 * <p>
 * Note: When inflating polygons, it's important that you select
 * {@link EndType#Polygon}. If you select one of the open path end types instead
 * (including EndType.Join), you'll simply inflate the polygon's outline.
 */
public class ClipperOffset {

	private final List<Group> groupList = new ArrayList<>();
	private final PathD normals = new PathD();
	private final Paths64 solution = new Paths64();
	private double groupDelta; // *0.5 for open paths; *-1.0 for negative areas
	private double delta;
	private double absGroupDelta;
	private double mitLimSqr;
	private double stepsPerRad;
	private double stepSin;
	private double stepCos;
	private JoinType joinType;
	private EndType endType;
	private double arcTolerance;
	private boolean mergeGroups;
	private double miterLimit;
	private boolean preserveCollinear;
	private boolean reverseSolution;

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear) {
		this(miterLimit, arcTolerance, preserveCollinear, false);
	}

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit, double arcTolerance) {
		this(miterLimit, arcTolerance, false, false);
	}

	/**
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset(double miterLimit) {
		this(miterLimit, 0.25, false, false);
	}

	/**
	 * Creates a ClipperOffset object, using default parameters.
	 *
	 * @see #ClipperOffset(double, double, boolean, boolean)
	 */
	public ClipperOffset() {
		this(2.0, 0.25, false, false);
	}

	/**
	 * Creates a ClipperOffset object, using the supplied parameters.
	 *
	 * @param miterLimit        This property sets the maximum distance in multiples
	 *                          of groupDelta that vertices can be offset from
	 *                          their original positions before squaring is applied.
	 *                          (Squaring truncates a miter by 'cutting it off' at 1
	 *                          × groupDelta distance from the original vertex.)
	 *                          <p>
	 *                          The default value for <code>miterLimit</code> is 2
	 *                          (i.e. twice groupDelta). This is also the smallest
	 *                          MiterLimit that's allowed. If mitering was
	 *                          unrestricted (ie without any squaring), then offsets
	 *                          at very acute angles would generate unacceptably
	 *                          long 'spikes'.
	 * @param arcTolerance      Since flattened paths can never perfectly represent
	 *                          arcs (see Trigonometry), this property specifies a
	 *                          maximum acceptable imperfection for rounded curves
	 *                          during offsetting.
	 *                          <p>
	 *                          It's important to make arcTolerance a sensible
	 *                          fraction of the offset groupDelta (arc radius).
	 *                          Large tolerances relative to the offset groupDelta
	 *                          will produce poor arc approximations but, just as
	 *                          importantly, very small tolerances will slow
	 *                          offsetting performance without noticeably improving
	 *                          curve quality.
	 *                          <p>
	 *                          arcTolerance is only relevant when offsetting with
	 *                          {@link JoinType#Round} and / or
	 *                          {@link EndType#Round} (see
	 *                          {{@link #AddPath(Path64, JoinType, EndType)
	 *                          AddPath()} and
	 *                          {@link #AddPaths(Paths64, JoinType, EndType)
	 *                          AddPaths()}. The default arcTolerance is 0.25.
	 * @param preserveCollinear When adjacent edges are collinear in closed path
	 *                          solutions, the common vertex can safely be removed
	 *                          to simplify the solution without altering path
	 *                          shape. However, because some users prefer to retain
	 *                          these common vertices, this feature is optional.
	 *                          Nevertheless, when adjacent edges in solutions are
	 *                          collinear and also create a 'spike' by overlapping,
	 *                          the vertex creating the spike will be removed
	 *                          irrespective of the PreserveCollinear setting. This
	 *                          property is false by default.
	 * @param reverseSolution   reverses the solution's orientation
	 */
	public ClipperOffset(double miterLimit, double arcTolerance, boolean preserveCollinear, boolean reverseSolution) {
		setMiterLimit(miterLimit);
		setArcTolerance(arcTolerance);
		setMergeGroups(true);
		setPreserveCollinear(preserveCollinear);
		setReverseSolution(reverseSolution);
	}

	public final void Clear() {
		groupList.clear();
	}

	public final void AddPath(Path64 path, JoinType joinType, EndType endType) {
		int cnt = path.size();
		if (cnt == 0) {
			return;
		}
		Paths64 pp = new Paths64(Arrays.asList(path));
		AddPaths(pp, joinType, endType);
	}

	public final void AddPaths(Paths64 paths, JoinType joinType, EndType endType) {
		int cnt = paths.size();
		if (cnt == 0) {
			return;
		}
		groupList.add(new Group(paths, joinType, endType));
	}

	private void ExecuteInternal(double delta) {
		solution.clear();
		if (groupList.isEmpty()) {
			return;
		}

		if (Math.abs(delta) < 0.5) {
			for (Group group : groupList) {
				for (Path64 path : group.inPaths) {
					solution.add(path);
				}
			}
		} else {
			this.delta = delta;
			this.mitLimSqr = (getMiterLimit() <= 1 ? 2.0 : 2.0 / Clipper.Sqr(getMiterLimit()));

			for (Group group : groupList) {
				DoGroupOffset(group);
			}
		}
	}

	public final void Execute(double delta, Paths64 solution) {
		solution.clear();
		ExecuteInternal(delta);

		// clean up self-intersections ...
		Clipper64 c = new Clipper64();
		c.setPreserveCollinear(getPreserveCollinear());
		c.setReverseSolution(getReverseSolution() != groupList.get(0).pathsReversed);
		c.AddSubject(this.solution);
		if (groupList.get(0).pathsReversed) {
			c.Execute(ClipType.Union, FillRule.Negative, solution);
		} else {
			c.Execute(ClipType.Union, FillRule.Positive, solution);
		}
	}

	public void Execute(double delta, PolyTree64 polytree) {
		polytree.Clear();
		ExecuteInternal(delta);

		// clean up self-intersections ...
		Clipper64 c = new Clipper64();
		c.setPreserveCollinear(getPreserveCollinear());
		// the solution should retain the orientation of the input
		c.setReverseSolution(getReverseSolution() != groupList.get(0).pathsReversed);
		c.AddSubject(this.solution);
		if (groupList.get(0).pathsReversed) {
			c.Execute(ClipType.Union, FillRule.Negative, polytree);
		} else {
			c.Execute(ClipType.Union, FillRule.Positive, polytree);
		}
	}

	public final double getArcTolerance() {
		return arcTolerance;
	}

	public final void setArcTolerance(double value) {
		arcTolerance = value;
	}

	public final boolean getMergeGroups() {
		return mergeGroups;
	}

	public final void setMergeGroups(boolean value) {
		mergeGroups = value;
	}

	public final double getMiterLimit() {
		return miterLimit;
	}

	public final void setMiterLimit(double value) {
		miterLimit = value;
	}

	public final boolean getPreserveCollinear() {
		return preserveCollinear;
	}

	public final void setPreserveCollinear(boolean value) {
		preserveCollinear = value;
	}

	public final boolean getReverseSolution() {
		return reverseSolution;
	}

	public final void setReverseSolution(boolean value) {
		reverseSolution = value;
	}

	private static PointD GetUnitNormal(Point64 pt1, Point64 pt2) {
		double dx = (pt2.x - pt1.x);
		double dy = (pt2.y - pt1.y);
		if ((dx == 0) && (dy == 0)) {
			return new PointD();
		}

		double f = 1.0 / Math.sqrt(dx * dx + dy * dy);
		dx *= f;
		dy *= f;

		return new PointD(dy, -dx);
	}

	private static void GetBoundsAndLowestPolyIdx(Paths64 paths, OutObject<Integer> index, OutObject<Rect64> recRef) {
		final Rect64 rec = new Rect64(false); // ie invalid rect
		recRef.argValue = rec;
		long lpX = Long.MIN_VALUE;
		index.argValue = -1;
		for (int i = 0; i < paths.size(); i++) {
			for (Point64 pt : paths.get(i)) {
				if (pt.y >= rec.bottom) {
					if (pt.y > rec.bottom || pt.x < lpX) {
						index.argValue = i;
						lpX = pt.x;
						rec.bottom = pt.y;
					}
				} else if (pt.y < rec.top) {
					rec.top = pt.y;
				}
				if (pt.x > rec.right) {
					rec.right = pt.x;
				} else if (pt.x < rec.left) {
					rec.left = pt.y;
				}
			}
		}
	}

	private static PointD TranslatePoint(PointD pt, double dx, double dy) {
		return new PointD(pt.x + dx, pt.y + dy);
	}

	private static PointD ReflectPoint(PointD pt, PointD pivot) {
		return new PointD(pivot.x + (pivot.x - pt.x), pivot.y + (pivot.y - pt.y));
	}

	private static boolean AlmostZero(double value) {
		return AlmostZero(value, 0.001);
	}

	private static boolean AlmostZero(double value, double epsilon) {
		return Math.abs(value) < epsilon;
	}

	private static double Hypotenuse(double x, double y) {
		return Math.sqrt(x * x + y * y);
	}

	private static PointD NormalizeVector(PointD vec) {
		double h = Hypotenuse(vec.x, vec.y);
		if (AlmostZero(h)) {
			return new PointD(0, 0);
		}
		double inverseHypot = 1 / h;
		return new PointD(vec.x * inverseHypot, vec.y * inverseHypot);
	}

	private static PointD GetAvgUnitVector(PointD vec1, PointD vec2) {
		return NormalizeVector(new PointD(vec1.x + vec2.x, vec1.y + vec2.y));
	}

	private static PointD IntersectPoint(PointD pt1a, PointD pt1b, PointD pt2a, PointD pt2b) {
		if (InternalClipper.IsAlmostZero(pt1a.x - pt1b.x)) { // vertical
			if (InternalClipper.IsAlmostZero(pt2a.x - pt2b.x)) {
				return new PointD(0, 0);
			}
			double m2 = (pt2b.y - pt2a.y) / (pt2b.x - pt2a.x);
			double b2 = pt2a.y - m2 * pt2a.x;
			return new PointD(pt1a.x, m2 * pt1a.x + b2);
		}

		if (InternalClipper.IsAlmostZero(pt2a.x - pt2b.x)) { // vertical
			double m1 = (pt1b.y - pt1a.y) / (pt1b.x - pt1a.x);
			double b1 = pt1a.y - m1 * pt1a.x;
			return new PointD(pt2a.x, m1 * pt2a.x + b1);
		} else {
			double m1 = (pt1b.y - pt1a.y) / (pt1b.x - pt1a.x);
			double b1 = pt1a.y - m1 * pt1a.x;
			double m2 = (pt2b.y - pt2a.y) / (pt2b.x - pt2a.x);
			double b2 = pt2a.y - m2 * pt2a.x;
			if (InternalClipper.IsAlmostZero(m1 - m2)) {
				return new PointD(0, 0);
			}
			double x = (b2 - b1) / (m1 - m2);
			return new PointD(x, m1 * x + b1);
		}
	}

	private Point64 GetPerpendic(Point64 pt, PointD norm) {
		return new Point64(pt.x + norm.x * groupDelta, pt.y + norm.y * groupDelta);
	}

	private PointD GetPerpendicD(Point64 pt, PointD norm) {
		return new PointD(pt.x + norm.x * groupDelta, pt.y + norm.y * groupDelta);
	}

	private void DoSquare(Group group, Path64 path, int j, int k) {
		PointD vec;
		if (j == k) {
			vec = new PointD(normals.get(0).y, -normals.get(0).x);
		} else {
			vec = GetAvgUnitVector(new PointD(-normals.get(k).y, normals.get(k).x), new PointD(normals.get(j).y, -normals.get(j).x));
		}

		// now offset the original vertex delta units along unit vector
		PointD ptQ = new PointD(path.get(j));
		ptQ = TranslatePoint(ptQ, absGroupDelta * vec.x, absGroupDelta * vec.y);

		// get perpendicular vertices
		PointD pt1 = TranslatePoint(ptQ, groupDelta * vec.y, groupDelta * -vec.x);
		PointD pt2 = TranslatePoint(ptQ, groupDelta * -vec.y, groupDelta * vec.x);
		// get 2 vertices along one edge offset
		PointD pt3 = GetPerpendicD(path.get(k), normals.get(k));

		if (j == k) {
			PointD pt4 = new PointD(pt3.x + vec.x * groupDelta, pt3.y + vec.y * groupDelta);
			PointD pt = IntersectPoint(pt1, pt2, pt3, pt4);
			// get the second intersect point through reflecion
			group.outPath.add(new Point64(ReflectPoint(pt, ptQ)));
			group.outPath.add(new Point64(pt));
		} else {
			PointD pt4 = GetPerpendicD(path.get(j), normals.get(k));
			PointD pt = IntersectPoint(pt1, pt2, pt3, pt4);
			group.outPath.add(new Point64(pt));
			// get the second intersect point through reflecion
			group.outPath.add(new Point64(ReflectPoint(pt, ptQ)));
		}
	}

	private void DoMiter(Group group, Path64 path, int j, int k, double cosA) {
		double q = groupDelta / (cosA + 1);
		group.outPath.add(new Point64(path.get(j).x + (normals.get(k).x + normals.get(j).x) * q,
				path.get(j).y + (normals.get(k).y + normals.get(j).y) * q));
	}

	private void DoRound(Group group, Path64 path, int j, int k, double angle) {
		Point64 pt = path.get(j);
		PointD offsetVec = new PointD(normals.get(k).x * groupDelta, normals.get(k).y * groupDelta);
		if (j == k) {
			offsetVec.Negate();
		}
		group.outPath.add(new Point64(pt.x + offsetVec.x, pt.y + offsetVec.y));
		if (angle > -Math.PI + 0.01) // avoid 180deg concave
		{
			int steps = (int) Math.ceil(stepsPerRad * Math.abs(angle));
			for (int i = 1; i < steps; i++) // ie 1 less than steps
			{
				offsetVec = new PointD(offsetVec.x * stepCos - stepSin * offsetVec.y, offsetVec.x * stepSin + offsetVec.y * stepCos);
				group.outPath.add(new Point64(pt.x + offsetVec.x, pt.y + offsetVec.y));
			}
		}
		group.outPath.add(GetPerpendic(pt, normals.get(j)));
	}

	private void BuildNormals(Path64 path) {
		int cnt = path.size();
		normals.clear();

		for (int i = 0; i < cnt - 1; i++) {
			normals.add(GetUnitNormal(path.get(i), path.get(i + 1)));
		}
		normals.add(GetUnitNormal(path.get(cnt - 1), path.get(0)));
	}

	private void OffsetPoint(Group group, Path64 path, int j, RefObject<Integer> k) {
		// Let A = change in angle where edges join
		// A == 0: ie no change in angle (flat join)
		// A == PI: edges 'spike'
		// sin(A) < 0: right turning
		// cos(A) < 0: change in angle is more than 90 degree
		double sinA = InternalClipper.CrossProduct(normals.get(j), normals.get(k.argValue));
		double cosA = InternalClipper.DotProduct(normals.get(j), normals.get(k.argValue));
		if (sinA > 1.0) {
			sinA = 1.0;
		} else if (sinA < -1.0) {
			sinA = -1.0;
		}

		if (cosA > 0.99) // almost straight - less than 8 degrees
		{
			group.outPath.add(GetPerpendic(path.get(j), normals.get(k.argValue)));
			if (cosA < 0.9998) { // greater than 1 degree (#424)
				group.outPath.add(GetPerpendic(path.get(j), normals.get(j))); // (#418)
			}
		} else if (cosA > -0.99 && (sinA * groupDelta < 0)) // is concave
		{
			group.outPath.add(GetPerpendic(path.get(j), normals.get(k.argValue)));
			// this extra point is the only (simple) way to ensure that
			// path reversals are fully cleaned with the trailing clipper
			group.outPath.add(path.get(j)); // (#405)
			group.outPath.add(GetPerpendic(path.get(j), normals.get(j)));
		} else if (joinType == JoinType.Round) {
			DoRound(group, path, j, k.argValue, Math.atan2(sinA, cosA));
		} else if (joinType == JoinType.Miter) {
			// miter unless the angle is so acute the miter would exceeds ML
			if (cosA > mitLimSqr - 1) {
				DoMiter(group, path, j, k.argValue, cosA);
			} else {
				DoSquare(group, path, j, k.argValue);
			}
		}
		// don't bother squaring angles that deviate < ~20 degrees because
		// squaring will be indistinguishable from mitering and just be a lot slower
		else if (cosA > 0.9) {
			DoMiter(group, path, j, k.argValue, cosA);
		} else {
			DoSquare(group, path, j, k.argValue);
		}
		k.argValue = j;
	}

	private void OffsetPolygon(Group group, Path64 path) {
		group.outPath = new Path64();
		int cnt = path.size();
		RefObject<Integer> prev = new RefObject<>(cnt - 1);
		for (int i = 0; i < cnt; i++) {
			OffsetPoint(group, path, i, prev);
		}
		group.outPaths.add(group.outPath);
	}

	private void OffsetOpenJoined(Group group, Path64 path) {
		OffsetPolygon(group, path);
		path = Clipper.ReversePath(path);
		BuildNormals(path);
		OffsetPolygon(group, path);
	}

	private void OffsetOpenPath(Group group, Path64 path) {
		group.outPath = new Path64();
		int highI = path.size() - 1;

		// do the line start cap
		switch (this.endType) {
			case Butt :
				group.outPath
						.add(new Point64(path.get(0).x - normals.get(0).x * groupDelta, path.get(0).y - normals.get(0).y * groupDelta));
				group.outPath.add(GetPerpendic(path.get(0), normals.get(0)));
				break;
			case Round :
				DoRound(group, path, 0, 0, Math.PI);
				break;
			default :
				DoSquare(group, path, 0, 0);
				break;
		}

		// offset the left side going forward
		RefObject<Integer> k = new RefObject<>(0);
		for (int i = 1; i < highI; i++) {
			OffsetPoint(group, path, i, k);
		}

		// reverse normals ...
		for (int i = highI; i > 0; i--) {
			normals.set(i, new PointD(-normals.get(i - 1).x, -normals.get(i - 1).y));
		}
		normals.set(0, normals.get(highI));

		// do the line end cap
		switch (this.endType) {
			case Butt :
				group.outPath.add(new Point64(path.get(highI).x - normals.get(highI).x * groupDelta,
						path.get(highI).y - normals.get(highI).y * groupDelta));
				group.outPath.add(GetPerpendic(path.get(highI), normals.get(highI)));
				break;
			case Round :
				DoRound(group, path, highI, highI, Math.PI);
				break;
			default :
				DoSquare(group, path, highI, highI);
				break;
		}

		// offset the left side going back
		k.argValue = 0;
		for (int i = highI; i > 0; i--) {
			OffsetPoint(group, path, i, k);
		}

		group.outPaths.add(group.outPath);
	}

	private void DoGroupOffset(Group group) {
		if (group.endType == EndType.Polygon) {
			// the lowermost polygon must be an outer polygon. So we can use that as the
			// designated orientation for outer polygons (needed for tidy-up clipping)
			OutObject<Integer> lowestIdx = new OutObject<>();
			OutObject<Rect64> grpBounds = new OutObject<>();
			GetBoundsAndLowestPolyIdx(group.inPaths, lowestIdx, grpBounds);
			if (lowestIdx.argValue < 0) {
				return;
			}
			double area = Clipper.Area(group.inPaths.get(lowestIdx.argValue));
			// if (area == 0) return; // this is probably unhelpful (#430)
			group.pathsReversed = (area < 0);
			if (group.pathsReversed) {
				this.groupDelta = -this.delta;
			} else {
				this.groupDelta = this.delta;
			}
		} else {
			group.pathsReversed = false;
			this.groupDelta = Math.abs(this.delta) * 0.5;
		}
		this.absGroupDelta = Math.abs(this.groupDelta);
		this.joinType = group.joinType;
		this.endType = group.endType;

		// calculate a sensible number of steps (for 360 deg for the given offset
		if (group.joinType == JoinType.Round || group.endType == EndType.Round) {
			// arcTol - when fArcTolerance is undefined (0), the amount of
			// curve imprecision that's allowed is based on the size of the
			// offset (delta). Obviously very large offsets will almost always
			// require much less precision. See also offset_triginometry2.svg
			double arcTol = arcTolerance > 0.01 ? arcTolerance : Math.log10(2 + this.absGroupDelta) * InternalClipper.DEFAULT_ARC_TOLERANCE;
			double stepsPer360 = Math.PI / Math.acos(1 - arcTol / absGroupDelta);
			stepSin = Math.sin((2 * Math.PI) / stepsPer360);
			stepCos = Math.cos((2 * Math.PI) / stepsPer360);
			if (groupDelta < 0.0) {
				stepSin = -stepSin;
			}
			stepsPerRad = stepsPer360 / (2 * Math.PI);
		}

		boolean isJoined = (group.endType == EndType.Joined) || (group.endType == EndType.Polygon);

		for (Path64 p : group.inPaths) {
			Path64 path = Clipper.StripDuplicates(p, isJoined);
			int cnt = path.size();
			if ((cnt == 0) || ((cnt < 3) && (this.endType == EndType.Polygon))) {
				continue;
			}

			if (cnt == 1) {
				group.outPath = new Path64();
				// single vertex so build a circle or square ...
				if (group.endType == EndType.Round) {
					double r = this.absGroupDelta;
					group.outPath = Clipper.Ellipse(path.get(0), r, r);
				} else {
					int d = (int) Math.ceil(this.groupDelta);
					Rect64 r = new Rect64(path.get(0).x - d, path.get(0).y - d, path.get(0).x - d, path.get(0).y - d);
					group.outPath = r.AsPath();
				}
				group.outPaths.add(group.outPath);
			} else {
				if (cnt == 2 && group.endType == EndType.Joined) {
					if (group.joinType == JoinType.Round) {
						this.endType = EndType.Round;
					} else {
						this.endType = EndType.Square;
					}
				}
				BuildNormals(path);
				if (this.endType == EndType.Polygon) {
					OffsetPolygon(group, path);
				} else if (this.endType == EndType.Joined) {
					OffsetOpenJoined(group, path);
				} else {
					OffsetOpenPath(group, path);
				}
			}
		}
		solution.addAll(group.outPaths);
		group.outPaths.clear();
	}

}