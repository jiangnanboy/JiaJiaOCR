package com.clipper.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

import com.clipper.tangible.OutObject;
import com.clipper.Clipper;
import com.clipper.Nullable;
import com.clipper.core.ClipType;
import com.clipper.core.FillRule;
import com.clipper.core.InternalClipper;
import com.clipper.core.Path64;
import com.clipper.core.PathType;
import com.clipper.core.Paths64;
import com.clipper.core.Point64;
import com.clipper.core.PointD;
import com.clipper.core.Rect64;
import com.clipper.tangible.RefObject;

/**
 * Subject and Clip paths are passed to a Clipper object via AddSubject,
 * AddOpenSubject and AddClip methods. Clipping operations are then initiated by
 * calling Execute. And Execute can be called multiple times (ie with different
 * ClipTypes & FillRules) without having to reload these paths.
 */
abstract class ClipperBase {

	private ClipType cliptype = ClipType.None;
	private FillRule fillrule = FillRule.EvenOdd;
	private Active actives = null;
	private Active sel = null;
	private List<LocalMinima> minimaList;
	private List<IntersectNode> intersectList;
	private List<Vertex> vertexList;
	private List<OutRec> outrecList;
	private NavigableSet<Long> scanlineSet;
	private List<HorzSegment> horzSegList;
	private List<HorzJoin> horzJoinList;
	private int currentLocMin;
	private long currentBotY;
	private boolean isSortedMinimaList;
	private boolean hasOpenPaths;
	boolean usingPolytree;
	boolean succeeded;
	private boolean preserveCollinear;
	private boolean reverseSolution;

	/**
	 * Path data structure for clipping solutions.
	 */
	static class OutRec {

		int idx;
		@Nullable
		OutRec owner;
		@Nullable
		Active frontEdge;
		@Nullable
		Active backEdge;
		@Nullable
		OutPt pts;
		@Nullable
		PolyPathBase polypath;
		Rect64 bounds = new Rect64();
		Path64 path = new Path64();
		boolean isOpen;
		@Nullable
		public List<Integer> splits = null;
	}

	private static class HorzSegSorter implements Comparator<HorzSegment> {
		@Override
		public int compare(@Nullable HorzSegment hs1, @Nullable HorzSegment hs2) {
			if (hs1 == null || hs2 == null) {
				return 0;
			}
			if (hs1.rightOp == null) {
				return hs2.rightOp == null ? 0 : 1;
			} else if (hs2.rightOp == null) {
				return -1;
			} else {
				return Long.compare(hs1.leftOp.pt.x, hs2.leftOp.pt.x);
			}
		}
	}

	private static class HorzSegment {

		public @Nullable OutPt leftOp;
		public @Nullable OutPt rightOp;
		public boolean leftToRight;

		public HorzSegment(OutPt op) {
			leftOp = op;
			rightOp = null;
			leftToRight = true;
		}
	}

	private static class HorzJoin {

		public @Nullable OutPt op1;
		public @Nullable OutPt op2;

		public HorzJoin(OutPt ltor, OutPt rtol) {
			op1 = ltor;
			op2 = rtol;
		}
	}

	static class Active {

		Point64 bot;
		Point64 top;
		long curX; // current (updated at every new scanline)
		double dx;
		int windDx; // 1 or -1 depending on winding direction
		int windCount;
		int windCount2; // winding count of the opposite polytype
		@Nullable
		OutRec outrec;
		// AEL: 'active edge list' (Vatti's AET - active edge table)
		// a linked list of all edges (from left to right) that are present
		// (or 'active') within the current scanbeam (a horizontal 'beam' that
		// sweeps from bottom to top over the paths in the clipping operation).
		@Nullable
		Active prevInAEL;
		@Nullable
		Active nextInAEL;
		// SEL: 'sorted edge list' (Vatti's ST - sorted table)
		// linked list used when sorting edges into their new positions at the
		// top of scanbeams, but also (re)used to process horizontals.
		@Nullable
		Active prevInSEL;
		@Nullable
		Active nextInSEL;
		@Nullable
		Active jump;
		@Nullable
		Vertex vertexTop;
		LocalMinima localMin = new LocalMinima(); // the bottom of an edge 'bound' (also Vatti)
		boolean isLeftBound;
		JoinWith joinWith = JoinWith.None;

	}

	/**
	 * Vertex data structure for clipping solutions
	 */
	static class OutPt {

		Point64 pt;
		@Nullable
		OutPt next;
		OutPt prev;
		OutRec outrec;
		@Nullable
		HorzSegment horz;

		OutPt(Point64 pt, OutRec outrec) {
			this.pt = pt;
			this.outrec = outrec;
			next = this;
			prev = this;
			horz = null;
		}

	}

	enum JoinWith {
		None, Left, Right
	}

	enum HorzPosition {
		Bottom, Middle, Top
	}

	/**
	 * A structure representing 2 intersecting edges. Intersections must be sorted
	 * so they are processed from the largest y coordinates to the smallest while
	 * keeping edges adjacent.
	 */
	static final class IntersectNode {

		final Point64 pt;
		final Active edge1;
		final Active edge2;

		IntersectNode(Point64 pt, Active edge1, Active edge2) {
			this.pt = pt;
			this.edge1 = edge1;
			this.edge2 = edge2;
		}
	}

	/**
	 * Vertex: a pre-clipping data structure. It is used to separate polygons into
	 * ascending and descending 'bounds' (or sides) that start at local minima and
	 * ascend to a local maxima, before descending again.
	 */
	class Vertex {

		Point64 pt = new Point64();
		@Nullable
		Vertex next;
		@Nullable
		Vertex prev;
		int flags;

		Vertex(Point64 pt, int flags, Vertex prev) {
			this.pt = pt;
			this.flags = flags;
			next = null;
			this.prev = prev;
		}
	}

	class VertexFlags {

		static final int None = 0;
		static final int OpenStart = 1;
		static final int OpenEnd = 2;
		static final int LocalMax = 4;
		static final int LocalMin = 8;

	}

	protected ClipperBase() {
		minimaList = new ArrayList<>();
		intersectList = new ArrayList<>();
		vertexList = new ArrayList<>();
		outrecList = new ArrayList<>();
		scanlineSet = new TreeSet<>();
		horzSegList = new ArrayList<>();
		horzJoinList = new ArrayList<>();
		setPreserveCollinear(true);
	}

	public final boolean getPreserveCollinear() {
		return preserveCollinear;
	}

	/**
	 * When adjacent edges are collinear in closed path solutions, the common vertex
	 * can safely be removed to simplify the solution without altering path shape.
	 * However, because some users prefer to retain these common vertices, this
	 * feature is optional. Nevertheless, when adjacent edges in solutions are
	 * collinear and also create a 'spike' by overlapping, the vertex creating the
	 * spike will be removed irrespective of the PreserveCollinear setting. This
	 * property is enabled by default.
	 */
	public final void setPreserveCollinear(boolean value) {
		preserveCollinear = value;
	}

	public final boolean getReverseSolution() {
		return reverseSolution;
	}

	public final void setReverseSolution(boolean value) {
		reverseSolution = value;
	}

	private static boolean IsOdd(int val) {
		return ((val & 1) != 0);
	}

	private static boolean IsHotEdge(Active ae) {
		return ae.outrec != null;
	}

	private static boolean IsOpen(Active ae) {
		return ae.localMin.isOpen;
	}

	private static boolean IsOpenEnd(Active ae) {
		return ae.localMin.isOpen && IsOpenEnd(ae.vertexTop);
	}

	private static boolean IsOpenEnd(Vertex v) {
		return (v.flags & (VertexFlags.OpenStart | VertexFlags.OpenEnd)) != VertexFlags.None;
	}

	private static Active GetPrevHotEdge(Active ae) {
		Active prev = ae.prevInAEL;
		while (prev != null && (IsOpen(prev) || !IsHotEdge(prev))) {
			prev = prev.prevInAEL;
		}
		return prev;
	}

	private static boolean IsFront(Active ae) {
		return (ae == ae.outrec.frontEdge);
	}

	private static double GetDx(Point64 pt1, Point64 pt2) {
		/*-
		 *  Dx:                             0(90deg)                                    *
		 *                                  |                                           *
		 *               +inf (180deg) <--- o --. -inf (0deg)                           *
		 *******************************************************************************/
		double dy = pt2.y - pt1.y;
		if (dy != 0) {
			return (pt2.x - pt1.x) / dy;
		}
		if (pt2.x > pt1.x) {
			return Double.NEGATIVE_INFINITY;
		}
		return Double.POSITIVE_INFINITY;
	}

	private static long TopX(Active ae, long currentY) {
		if ((currentY == ae.top.y) || (ae.top.x == ae.bot.x)) {
			return ae.top.x;
		}
		if (currentY == ae.bot.y) {
			return ae.bot.x;
		}
		return ae.bot.x + (long) Math.rint(ae.dx * (currentY - ae.bot.y));
	}

	private static boolean IsHorizontal(Active ae) {
		return (ae.top.y == ae.bot.y);
	}

	private static boolean IsHeadingRightHorz(Active ae) {
		return Double.NEGATIVE_INFINITY == ae.dx;
	}

	private static boolean IsHeadingLeftHorz(Active ae) {
		return Double.POSITIVE_INFINITY == ae.dx;
	}

	private static void SwapActives(RefObject<Active> ae1, RefObject<Active> ae2) {
		Active temp = ae1.argValue;
		ae1.argValue = ae2.argValue;
		ae2.argValue = temp;
	}

	private static PathType GetPolyType(Active ae) {
		return ae.localMin.polytype;
	}

	private static boolean IsSamePolyType(Active ae1, Active ae2) {
		return ae1.localMin.polytype == ae2.localMin.polytype;
	}

	private static void SetDx(Active ae) {
		ae.dx = GetDx(ae.bot, ae.top);
	}

	private static Vertex NextVertex(Active ae) {
		if (ae.windDx > 0) {
			return ae.vertexTop.next;
		}
		return ae.vertexTop.prev;
	}

	private static Vertex PrevPrevVertex(Active ae) {
		if (ae.windDx > 0) {
			return ae.vertexTop.prev.prev;
		}
		return ae.vertexTop.next.next;
	}

	private static boolean IsMaxima(Vertex vertex) {
		return ((vertex.flags & VertexFlags.LocalMax) != VertexFlags.None);
	}

	private static boolean IsMaxima(Active ae) {
		return IsMaxima(ae.vertexTop);
	}

	private static Active GetMaximaPair(Active ae) {
		Active ae2 = null;
		ae2 = ae.nextInAEL;
		while (ae2 != null) {
			if (ae2.vertexTop == ae.vertexTop) {
				return ae2; // Found!
			}
			ae2 = ae2.nextInAEL;
		}
		return null;
	}

	private static @Nullable Vertex GetCurrYMaximaVertex_Open(Active ae) {
		@Nullable
		Vertex result = ae.vertexTop;
		if (ae.windDx > 0) {
			while (result.next.pt.y == result.pt.y && ((result.flags & (VertexFlags.OpenEnd | VertexFlags.LocalMax)) == VertexFlags.None)) {
				result = result.next;
			}
		} else {
			while (result.prev.pt.y == result.pt.y && ((result.flags & (VertexFlags.OpenEnd | VertexFlags.LocalMax)) == VertexFlags.None)) {
				result = result.prev;
			}
		}
		if (!IsMaxima(result)) {
			result = null; // not a maxima
		}
		return result;
	}

	private static Vertex GetCurrYMaximaVertex(Active ae) {
		Vertex result = ae.vertexTop;
		if (ae.windDx > 0) {
			while (result.next.pt.y == result.pt.y) {
				result = result.next;
			}
		} else {
			while (result.prev.pt.y == result.pt.y) {
				result = result.prev;
			}
		}
		if (!IsMaxima(result)) {
			result = null; // not a maxima
		}
		return result;
	}

	private static void SetSides(OutRec outrec, Active startEdge, Active endEdge) {
		outrec.frontEdge = startEdge;
		outrec.backEdge = endEdge;
	}

	private static void SwapOutrecs(Active ae1, Active ae2) {
		OutRec or1 = ae1.outrec; // at least one edge has
		OutRec or2 = ae2.outrec; // an assigned outrec
		if (or1 == or2) {
			Active ae = or1.frontEdge;
			or1.frontEdge = or1.backEdge;
			or1.backEdge = ae;
			return;
		}

		if (or1 != null) {
			if (ae1 == or1.frontEdge) {
				or1.frontEdge = ae2;
			} else {
				or1.backEdge = ae2;
			}
		}

		if (or2 != null) {
			if (ae2 == or2.frontEdge) {
				or2.frontEdge = ae1;
			} else {
				or2.backEdge = ae1;
			}
		}

		ae1.outrec = or2;
		ae2.outrec = or1;
	}

	private static void SetOwner(OutRec outrec, OutRec newOwner) {
		// precondition1: newOwner is never null
		while (newOwner.owner != null && newOwner.owner.pts == null) {
			newOwner.owner = newOwner.owner.owner;
		}

		// make sure that outrec isn't an owner of newOwner
		@Nullable
		OutRec tmp = newOwner;
		while (tmp != null && tmp != outrec) {
			tmp = tmp.owner;
		}
		if (tmp != null) {
			newOwner.owner = outrec.owner;
		}
		outrec.owner = newOwner;
	}

	private static double Area(OutPt op) {
		// https://en.wikipedia.org/wiki/Shoelaceformula
		double area = 0.0;
		OutPt op2 = op;
		do {
			area += (op2.prev.pt.y + op2.pt.y) * (op2.prev.pt.x - op2.pt.x);
			op2 = op2.next;
		} while (op2 != op);
		return area * 0.5;
	}

	private static double AreaTriangle(Point64 pt1, Point64 pt2, Point64 pt3) {
		return (pt3.y + pt1.y) * (pt3.x - pt1.x) + (pt1.y + pt2.y) * (pt1.x - pt2.x) + (pt2.y + pt3.y) * (pt2.x - pt3.x);
	}

	private static OutRec GetRealOutRec(OutRec outRec) {
		while ((outRec != null) && (outRec.pts == null)) {
			outRec = outRec.owner;
		}
		return outRec;
	}

	private static void UncoupleOutRec(Active ae) {
		OutRec outrec = ae.outrec;
		if (outrec == null) {
			return;
		}
		outrec.frontEdge.outrec = null;
		outrec.backEdge.outrec = null;
		outrec.frontEdge = null;
		outrec.backEdge = null;
	}

	private static boolean OutrecIsAscending(Active hotEdge) {
		return (hotEdge == hotEdge.outrec.frontEdge);
	}

	private static void SwapFrontBackSides(OutRec outrec) {
		// while this proc. is needed for open paths
		// it's almost never needed for closed paths
		Active ae2 = outrec.frontEdge;
		outrec.frontEdge = outrec.backEdge;
		outrec.backEdge = ae2;
		outrec.pts = outrec.pts.next;
	}

	private static boolean EdgesAdjacentInAEL(IntersectNode inode) {
		return (inode.edge1.nextInAEL == inode.edge2) || (inode.edge1.prevInAEL == inode.edge2);
	}

	protected final void ClearSolutionOnly() {
		while (actives != null) {
			DeleteFromAEL(actives);
		}
		scanlineSet.clear();
		DisposeIntersectNodes();
		outrecList.clear();
		horzSegList.clear();
		horzJoinList.clear();
	}

	public final void Clear() {
		ClearSolutionOnly();
		minimaList.clear();
		vertexList.clear();
		currentLocMin = 0;
		isSortedMinimaList = false;
		hasOpenPaths = false;
	}

	protected final void Reset() {
		if (!isSortedMinimaList) {
			minimaList.sort((locMin1, locMin2) -> Long.compare(locMin2.vertex.pt.y, locMin1.vertex.pt.y));
			isSortedMinimaList = true;
		}

		for (int i = minimaList.size() - 1; i >= 0; i--) {
			scanlineSet.add(minimaList.get(i).vertex.pt.y);
		}

		currentBotY = 0;
		currentLocMin = 0;
		actives = null;
		sel = null;
		succeeded = true;
	}

	/**
	 * @deprecated Has been inlined in Java version since function is much simpler
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private void InsertScanline(long y) {
		scanlineSet.add(y);
	}

	/**
	 * @deprecated Has been inlined in Java version since function is much simpler
	 */
	@SuppressWarnings("unused")
	@Deprecated
	private long PopScanline() {
		if (scanlineSet.isEmpty()) {
			return Long.MAX_VALUE;
		}
		return scanlineSet.pollLast();
	}

	private boolean HasLocMinAtY(long y) {
		return (currentLocMin < minimaList.size() && minimaList.get(currentLocMin).vertex.pt.y == y);
	}

	private LocalMinima PopLocalMinima() {
		return minimaList.get(currentLocMin++);
	}

	private void AddLocMin(Vertex vert, PathType polytype, boolean isOpen) {
		// make sure the vertex is added only once ...
		if ((vert.flags & VertexFlags.LocalMin) != VertexFlags.None) {
			return;
		}
		vert.flags |= VertexFlags.LocalMin;

		LocalMinima lm = new LocalMinima(vert, polytype, isOpen);
		minimaList.add(lm);
	}

	protected final void AddPathsToVertexList(Paths64 paths, PathType polytype, boolean isOpen) {
		for (Path64 path : paths) {
			Vertex v0 = null, prevV = null, currV;
			for (Point64 pt : path) {
				if (v0 == null) {
					v0 = new Vertex(pt, VertexFlags.None, null);
					vertexList.add(v0);
					prevV = v0;
				} else if (prevV.pt.opNotEquals(pt)) { // ie skips duplicates
					currV = new Vertex(pt, VertexFlags.None, prevV);
					vertexList.add(currV);
					prevV.next = currV;
					prevV = currV;
				}
			}
			if (prevV == null || prevV.prev == null) {
				continue;
			}
			if (!isOpen && v0.pt.opEquals(prevV.pt)) {
				prevV = prevV.prev;
			}
			prevV.next = v0;
			v0.prev = prevV;
			if (!isOpen && prevV == prevV.next) {
				continue;
			}

			// OK, we have a valid path
			boolean goingup, goingup0;
			if (isOpen) {
				currV = v0.next;
				while (v0 != currV && currV.pt.y == v0.pt.y) {
					currV = currV.next;
				}
				goingup = currV.pt.y <= v0.pt.y;
				if (goingup) {
					v0.flags = VertexFlags.OpenStart;
					AddLocMin(v0, polytype, true);
				} else {
					v0.flags = VertexFlags.OpenStart | VertexFlags.LocalMax;
				}
			} else { // closed path
				prevV = v0.prev;
				while (!v0.equals(prevV) && prevV.pt.y == v0.pt.y) {
					prevV = prevV.prev;
				}
				if (v0.equals(prevV)) {
					continue; // only open paths can be completely flat
				}
				goingup = prevV.pt.y > v0.pt.y;
			}

			goingup0 = goingup;
			prevV = v0;
			currV = v0.next;
			while (!v0.equals(currV)) {
				if (currV.pt.y > prevV.pt.y && goingup) {
					prevV.flags |= VertexFlags.LocalMax;
					goingup = false;
				} else if (currV.pt.y < prevV.pt.y && !goingup) {
					goingup = true;
					AddLocMin(prevV, polytype, isOpen);
				}
				prevV = currV;
				currV = currV.next;
			}

			if (isOpen) {
				prevV.flags |= VertexFlags.OpenEnd;
				if (goingup) {
					prevV.flags |= VertexFlags.LocalMax;
				} else {
					AddLocMin(prevV, polytype, isOpen);
				}
			} else if (goingup != goingup0) {
				if (goingup0) {
					AddLocMin(prevV, polytype, false);
				} else {
					prevV.flags = prevV.flags | VertexFlags.LocalMax;
				}
			}
		}
	}

	public final void AddSubject(Path64 path) {
		AddPath(path, PathType.Subject);
	}

	/**
	 * Adds one or more closed subject paths (polygons) to the Clipper object.
	 */
	public final void AddSubject(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Subject));
	}

	/**
	 * Adds one or more open subject paths (polylines) to the Clipper object.
	 */
	public final void AddOpenSubject(Path64 path) {
		AddPath(path, PathType.Subject, true);
	}

	public final void AddOpenSubject(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Subject, true));
	}

	/**
	 * Adds one or more clip polygons to the Clipper object.
	 */
	public final void AddClip(Path64 path) {
		AddPath(path, PathType.Clip);
	}

	public final void AddClip(Paths64 paths) {
		paths.forEach(path -> AddPath(path, PathType.Clip));
	}

	public final void AddPath(Path64 path, PathType polytype) {
		AddPath(path, polytype, false);
	}

	public final void AddPath(Path64 path, PathType polytype, boolean isOpen) {
		Paths64 tmp = new Paths64();
		tmp.add(path);
		AddPaths(tmp, polytype, isOpen);
	}

	public final void AddPaths(Paths64 paths, PathType polytype) {
		AddPaths(paths, polytype, false);
	}

	public final void AddPaths(Paths64 paths, PathType polytype, boolean isOpen) {
		if (isOpen) {
			hasOpenPaths = true;
		}
		isSortedMinimaList = false;
		AddPathsToVertexList(paths, polytype, isOpen);
	}

	private boolean IsContributingClosed(Active ae) {
		switch (fillrule) {
			case Positive :
				if (ae.windCount != 1) {
					return false;
				}
				break;
			case Negative :
				if (ae.windCount != -1) {
					return false;
				}
				break;
			case NonZero :
				if (Math.abs(ae.windCount) != 1) {
					return false;
				}
				break;
			case EvenOdd :
				break;
		}

		switch (cliptype) {
			case Intersection :
				switch (fillrule) {
					case Positive :
						return ae.windCount2 > 0;
					case Negative :
						return ae.windCount2 < 0;
					default :
						return ae.windCount2 != 0;
				}
			case Union :
				switch (fillrule) {
					case Positive :
						return ae.windCount2 <= 0;
					case Negative :
						return ae.windCount2 >= 0;
					default :
						return ae.windCount2 == 0;
				}

			case Difference :
				boolean result;
				switch (fillrule) {
					case Positive :
						result = ae.windCount2 <= 0;
						break;
					case Negative :
						result = ae.windCount2 >= 0;
						break;
					default :
						result = ae.windCount2 == 0;
						break;
				}
				return (GetPolyType(ae) == PathType.Subject) == result;
			case Xor :
				return true; // XOr is always contributing unless open
			default :
				return false;
		}
	}

	private boolean IsContributingOpen(Active ae) {
		boolean isInClip, isInSubj;
		switch (fillrule) {
			case Positive :
				isInSubj = ae.windCount > 0;
				isInClip = ae.windCount2 > 0;
				break;
			case Negative :
				isInSubj = ae.windCount < 0;
				isInClip = ae.windCount2 < 0;
				break;
			default :
				isInSubj = ae.windCount != 0;
				isInClip = ae.windCount2 != 0;
				break;
		}

		switch (cliptype) {
			case Intersection :
				return isInClip;
			case Union :
				return !isInSubj && !isInClip;
			default :
				return !isInClip;
		}
	}

	private void SetWindCountForClosedPathEdge(Active ae) {
		/*
		 * Wind counts refer to polygon regions not edges, so here an edge's WindCnt
		 * indicates the higher of the wind counts for the two regions touching the
		 * edge. (nb: Adjacent regions can only ever have their wind counts differ by
		 * one. Also, open paths have no meaningful wind directions or counts.)
		 */

		Active ae2 = ae.prevInAEL;
		// find the nearest closed path edge of the same PolyType in AEL (heading left)
		PathType pt = GetPolyType(ae);
		while (ae2 != null && (GetPolyType(ae2) != pt || IsOpen(ae2))) {
			ae2 = ae2.prevInAEL;
		}

		if (ae2 == null) {
			ae.windCount = ae.windDx;
			ae2 = actives;
		} else if (fillrule == FillRule.EvenOdd) {
			ae.windCount = ae.windDx;
			ae.windCount2 = ae2.windCount2;
			ae2 = ae2.nextInAEL;
		} else {
			// NonZero, positive, or negative filling here ...
			// when e2's WindCnt is in the SAME direction as its WindDx,
			// then polygon will fill on the right of 'e2' (and 'e' will be inside)
			// nb: neither e2.WindCnt nor e2.WindDx should ever be 0.
			if (ae2.windCount * ae2.windDx < 0) {
				// opposite directions so 'ae' is outside 'ae2' ...
				if (Math.abs(ae2.windCount) > 1) {
					// outside prev poly but still inside another.
					if (ae2.windDx * ae.windDx < 0) {
						// reversing direction so use the same WC
						ae.windCount = ae2.windCount;
					} else {
						// otherwise keep 'reducing' the WC by 1 (i.e. towards 0) ...
						ae.windCount = ae2.windCount + ae.windDx;
					}
				} else {
					// now outside all polys of same polytype so set own WC ...
					ae.windCount = (IsOpen(ae) ? 1 : ae.windDx);
				}
			} else // 'ae' must be inside 'ae2'
			if (ae2.windDx * ae.windDx < 0) {
				// reversing direction so use the same WC
				ae.windCount = ae2.windCount;
			} else {
				// otherwise keep 'increasing' the WC by 1 (i.e. away from 0) ...
				ae.windCount = ae2.windCount + ae.windDx;
			}

			ae.windCount2 = ae2.windCount2;
			ae2 = ae2.nextInAEL; // i.e. get ready to calc WindCnt2
		}

		// update windCount2 ...
		if (fillrule == FillRule.EvenOdd) {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) != pt && !IsOpen(ae2)) {
					ae.windCount2 = (ae.windCount2 == 0 ? 1 : 0);
				}
				ae2 = ae2.nextInAEL;
			}
		} else {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) != pt && !IsOpen(ae2)) {
					ae.windCount2 += ae2.windDx;
				}
				ae2 = ae2.nextInAEL;
			}
		}
	}

	private void SetWindCountForOpenPathEdge(Active ae) {
		Active ae2 = actives;
		if (fillrule == FillRule.EvenOdd) {
			int cnt1 = 0, cnt2 = 0;
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) == PathType.Clip) {
					cnt2++;
				} else if (!IsOpen(ae2)) {
					cnt1++;
				}
				ae2 = ae2.nextInAEL;
			}

			ae.windCount = (IsOdd(cnt1) ? 1 : 0);
			ae.windCount2 = (IsOdd(cnt2) ? 1 : 0);
		} else {
			while (!ae2.equals(ae)) {
				if (GetPolyType(ae2) == PathType.Clip) {
					ae.windCount2 += ae2.windDx;
				} else if (!IsOpen(ae2)) {
					ae.windCount += ae2.windDx;
				}
				ae2 = ae2.nextInAEL;
			}
		}
	}

	private static boolean IsValidAelOrder(Active resident, Active newcomer) {
		if (newcomer.curX != resident.curX) {
			return newcomer.curX > resident.curX;
		}

		// get the turning direction a1.top, a2.bot, a2.top
		double d = InternalClipper.CrossProduct(resident.top, newcomer.bot, newcomer.top);
		if (d != 0) {
			return (d < 0);
		}

		// edges must be collinear to get here

		// for starting open paths, place them according to
		// the direction they're about to turn
		if (!IsMaxima(resident) && (resident.top.y > newcomer.top.y)) {
			return InternalClipper.CrossProduct(newcomer.bot, resident.top, NextVertex(resident).pt) <= 0;
		}

		if (!IsMaxima(newcomer) && (newcomer.top.y > resident.top.y)) {
			return InternalClipper.CrossProduct(newcomer.bot, newcomer.top, NextVertex(newcomer).pt) >= 0;
		}

		long y = newcomer.bot.y;
		boolean newcomerIsLeft = newcomer.isLeftBound;

		if (resident.bot.y != y || resident.localMin.vertex.pt.y != y) {
			return newcomer.isLeftBound;
		}
		// resident must also have just been inserted
		if (resident.isLeftBound != newcomerIsLeft) {
			return newcomerIsLeft;
		}
		if (InternalClipper.CrossProduct(PrevPrevVertex(resident).pt, resident.bot, resident.top) == 0) {
			return true;
		}
		// compare turning direction of the alternate bound
		return (InternalClipper.CrossProduct(PrevPrevVertex(resident).pt, newcomer.bot, PrevPrevVertex(newcomer).pt) > 0) == newcomerIsLeft;
	}

	private void InsertLeftEdge(Active ae) {
		Active ae2;

		if (actives == null) {
			ae.prevInAEL = null;
			ae.nextInAEL = null;
			actives = ae;
		} else if (!IsValidAelOrder(actives, ae)) {
			ae.prevInAEL = null;
			ae.nextInAEL = actives;
			actives.prevInAEL = ae;
			actives = ae;
		} else {
			ae2 = actives;
			while (ae2.nextInAEL != null && IsValidAelOrder(ae2.nextInAEL, ae)) {
				ae2 = ae2.nextInAEL;
			}
			// don't separate joined edges
			if (ae2.joinWith == JoinWith.Right) {
				ae2 = ae2.nextInAEL;
			}
			ae.nextInAEL = ae2.nextInAEL;
			if (ae2.nextInAEL != null) {
				ae2.nextInAEL.prevInAEL = ae;
			}
			ae.prevInAEL = ae2;
			ae2.nextInAEL = ae;
		}
	}

	private /* static */ void InsertRightEdge(Active ae, Active ae2) {
		ae2.nextInAEL = ae.nextInAEL;
		if (ae.nextInAEL != null) {
			ae.nextInAEL.prevInAEL = ae2;
		}
		ae2.prevInAEL = ae;
		ae.nextInAEL = ae2;
	}

	private void InsertLocalMinimaIntoAEL(long botY) {
		LocalMinima localMinima;
		Active leftBound, rightBound = null;
		// Add any local minima (if any) at BotY ...
		// NB horizontal local minima edges should contain locMin.vertex.prev
		while (HasLocMinAtY(botY)) {
			localMinima = PopLocalMinima();
			if ((localMinima.vertex.flags & VertexFlags.OpenStart) != VertexFlags.None) {
				leftBound = null;
			} else {
				leftBound = new Active();
				leftBound.bot = localMinima.vertex.pt;
				leftBound.curX = localMinima.vertex.pt.x;
				leftBound.windDx = -1;
				leftBound.vertexTop = localMinima.vertex.prev;
				leftBound.top = localMinima.vertex.prev.pt;
				leftBound.outrec = null;
				leftBound.localMin = localMinima;
				SetDx(leftBound);
			}

			if ((localMinima.vertex.flags & VertexFlags.OpenEnd) != VertexFlags.None) {
				rightBound = null;
			} else {
				rightBound = new Active();
				rightBound.bot = localMinima.vertex.pt;
				rightBound.curX = localMinima.vertex.pt.x;
				rightBound.windDx = 1;
				rightBound.vertexTop = localMinima.vertex.next;
				rightBound.top = localMinima.vertex.next.pt;
				rightBound.outrec = null;
				rightBound.localMin = localMinima;
				SetDx(rightBound);
			}

			// Currently LeftB is just the descending bound and RightB is the ascending.
			// Now if the LeftB isn't on the left of RightB then we need swap them.
			if (leftBound != null && rightBound != null) {
				if (IsHorizontal(leftBound)) {
					if (IsHeadingRightHorz(leftBound)) {
						RefObject<Active> tempRefleftBound = new RefObject<>(leftBound);
						RefObject<Active> tempRefrightBound = new RefObject<>(rightBound);
						SwapActives(tempRefleftBound, tempRefrightBound);
						rightBound = tempRefrightBound.argValue;
						leftBound = tempRefleftBound.argValue;
					}
				} else if (IsHorizontal(rightBound)) {
					if (IsHeadingLeftHorz(rightBound)) {
						RefObject<Active> tempRefleftBound2 = new RefObject<>(leftBound);
						RefObject<Active> tempRefrightBound2 = new RefObject<>(rightBound);
						SwapActives(tempRefleftBound2, tempRefrightBound2);
						rightBound = tempRefrightBound2.argValue;
						leftBound = tempRefleftBound2.argValue;
					}
				} else if (leftBound.dx < rightBound.dx) {
					RefObject<Active> tempRefleftBound3 = new RefObject<>(leftBound);
					RefObject<Active> tempRefrightBound3 = new RefObject<>(rightBound);
					SwapActives(tempRefleftBound3, tempRefrightBound3);
					rightBound = tempRefrightBound3.argValue;
					leftBound = tempRefleftBound3.argValue;
				}
				// so when leftBound has windDx == 1, the polygon will be oriented
				// counter-clockwise in Cartesian coords (clockwise with inverted y).
			} else if (leftBound == null) {
				leftBound = rightBound;
				rightBound = null;
			}

			boolean contributing;
			leftBound.isLeftBound = true;
			InsertLeftEdge(leftBound);

			if (IsOpen(leftBound)) {
				SetWindCountForOpenPathEdge(leftBound);
				contributing = IsContributingOpen(leftBound);
			} else {
				SetWindCountForClosedPathEdge(leftBound);
				contributing = IsContributingClosed(leftBound);
			}

			if (rightBound != null) {
				rightBound.windCount = leftBound.windCount;
				rightBound.windCount2 = leftBound.windCount2;
				InsertRightEdge(leftBound, rightBound); ///////

				if (contributing) {
					AddLocalMinPoly(leftBound, rightBound, leftBound.bot, true);
					if (!IsHorizontal(leftBound)) {
						CheckJoinLeft(leftBound, leftBound.bot);
					}
				}

				while (rightBound.nextInAEL != null && IsValidAelOrder(rightBound.nextInAEL, rightBound)) {
					IntersectEdges(rightBound, rightBound.nextInAEL, rightBound.bot);
					SwapPositionsInAEL(rightBound, rightBound.nextInAEL);
				}

				if (IsHorizontal(rightBound)) {
					PushHorz(rightBound);
				} else {
					CheckJoinRight(rightBound, rightBound.bot);
					scanlineSet.add(rightBound.top.y);
				}
			} else if (contributing) {
				StartOpenPath(leftBound, leftBound.bot);
			}

			if (IsHorizontal(leftBound)) {
				PushHorz(leftBound);
			} else {
				scanlineSet.add(leftBound.top.y);
			}
		} // while (HasLocMinAtY())
	}

	private void PushHorz(Active ae) {
		ae.nextInSEL = sel;
		sel = ae;
	}

	private boolean PopHorz(OutObject<Active> ae) {
		ae.argValue = sel;
		if (sel == null) {
			return false;
		}
		sel = sel.nextInSEL;
		return true;
	}

	private OutPt AddLocalMinPoly(Active ae1, Active ae2, Point64 pt) {
		return AddLocalMinPoly(ae1, ae2, pt, false);
	}

	private OutPt AddLocalMinPoly(Active ae1, Active ae2, Point64 pt, boolean isNew) {
		OutRec outrec = NewOutRec();
		ae1.outrec = outrec;
		ae2.outrec = outrec;

		if (IsOpen(ae1)) {
			outrec.owner = null;
			outrec.isOpen = true;
			if (ae1.windDx > 0) {
				SetSides(outrec, ae1, ae2);
			} else {
				SetSides(outrec, ae2, ae1);
			}
		} else {
			outrec.isOpen = false;
			Active prevHotEdge = GetPrevHotEdge(ae1);
			// e.windDx is the winding direction of the **input** paths
			// and unrelated to the winding direction of output polygons.
			// Output orientation is determined by e.outrec.frontE which is
			// the ascending edge (see AddLocalMinPoly).
			if (prevHotEdge != null) {
				if (usingPolytree) {
					SetOwner(outrec, prevHotEdge.outrec);
				}
				outrec.owner = prevHotEdge.outrec;
				if (OutrecIsAscending(prevHotEdge) == isNew) {
					SetSides(outrec, ae2, ae1);
				} else {
					SetSides(outrec, ae1, ae2);
				}
			} else {
				outrec.owner = null;
				if (isNew) {
					SetSides(outrec, ae1, ae2);
				} else {
					SetSides(outrec, ae2, ae1);
				}
			}
		}

		OutPt op = new OutPt(pt, outrec);
		outrec.pts = op;
		return op;
	}

	private OutPt AddLocalMaxPoly(Active ae1, Active ae2, Point64 pt) {
		if (IsJoined(ae1)) {
			Split(ae1, pt);
		}
		if (IsJoined(ae2)) {
			Split(ae2, pt);
		}

		if (IsFront(ae1) == IsFront(ae2)) {
			if (IsOpenEnd(ae1)) {
				SwapFrontBackSides(ae1.outrec);
			} else if (IsOpenEnd(ae2)) {
				SwapFrontBackSides(ae2.outrec);
			} else {
				succeeded = false;
				return null;
			}
		}

		OutPt result = AddOutPt(ae1, pt);
		if (ae1.outrec == ae2.outrec) {
			OutRec outrec = ae1.outrec;
			outrec.pts = result;

			if (usingPolytree) {
				Active e = GetPrevHotEdge(ae1);
				if (e == null) {
					outrec.owner = null;
				} else {
					SetOwner(outrec, e.outrec);
					// nb: outRec.owner here is likely NOT the real
					// owner but this will be fixed in DeepCheckOwner()
				}
			}
			UncoupleOutRec(ae1);
		}
		// and to preserve the winding orientation of outrec ...
		else if (IsOpen(ae1)) {
			if (ae1.windDx < 0) {
				JoinOutrecPaths(ae1, ae2);
			} else {
				JoinOutrecPaths(ae2, ae1);
			}
		} else if (ae1.outrec.idx < ae2.outrec.idx) {
			JoinOutrecPaths(ae1, ae2);
		} else {
			JoinOutrecPaths(ae2, ae1);
		}

		return result;
	}

	private static void JoinOutrecPaths(Active ae1, Active ae2) {
		// join ae2 outrec path onto ae1 outrec path and then delete ae2 outrec path
		// pointers. (NB Only very rarely do the joining ends share the same coords.)
		OutPt p1Start = ae1.outrec.pts;
		OutPt p2Start = ae2.outrec.pts;
		OutPt p1End = p1Start.next;
		OutPt p2End = p2Start.next;
		if (IsFront(ae1)) {
			p2End.prev = p1Start;
			p1Start.next = p2End;
			p2Start.next = p1End;
			p1End.prev = p2Start;
			ae1.outrec.pts = p2Start;
			// nb: if IsOpen(e1) then e1 & e2 must be a 'maximaPair'
			ae1.outrec.frontEdge = ae2.outrec.frontEdge;
			if (ae1.outrec.frontEdge != null) {
				ae1.outrec.frontEdge.outrec = ae1.outrec;
			}
		} else {
			p1End.prev = p2Start;
			p2Start.next = p1End;
			p1Start.next = p2End;
			p2End.prev = p1Start;

			ae1.outrec.backEdge = ae2.outrec.backEdge;
			if (ae1.outrec.backEdge != null) {
				ae1.outrec.backEdge.outrec = ae1.outrec;
			}
		}

		// after joining, the ae2.OutRec must contains no vertices ...
		ae2.outrec.frontEdge = null;
		ae2.outrec.backEdge = null;
		ae2.outrec.pts = null;
		SetOwner(ae2.outrec, ae1.outrec);

		if (IsOpenEnd(ae1)) {
			ae2.outrec.pts = ae1.outrec.pts;
			ae1.outrec.pts = null;
		}

		// and ae1 and ae2 are maxima and are about to be dropped from the Actives list.
		ae1.outrec = null;
		ae2.outrec = null;
	}

	private static OutPt AddOutPt(Active ae, Point64 pt) {

		// Outrec.OutPts: a circular doubly-linked-list of POutPt where ...
		// opFront[.Prev]* ~~~> opBack & opBack == opFront.Next
		OutRec outrec = ae.outrec;
		boolean toFront = IsFront(ae);
		OutPt opFront = outrec.pts;
		OutPt opBack = opFront.next;

		if (toFront && (pt.opEquals(opFront.pt))) {
			return opFront;
		} else if (!toFront && (pt.opEquals(opBack.pt))) {
			return opBack;
		}

		OutPt newOp = new OutPt(pt, outrec);
		opBack.prev = newOp;
		newOp.prev = opFront;
		newOp.next = opBack;
		opFront.next = newOp;
		if (toFront) {
			outrec.pts = newOp;
		}
		return newOp;
	}

	private OutRec NewOutRec() {
		OutRec result = new OutRec();
		result.idx = outrecList.size();
		outrecList.add(result);
		return result;
	}

	private OutPt StartOpenPath(Active ae, Point64 pt) {
		OutRec outrec = NewOutRec();
		outrec.isOpen = true;
		if (ae.windDx > 0) {
			outrec.frontEdge = ae;
			outrec.backEdge = null;
		} else {
			outrec.frontEdge = null;
			outrec.backEdge = ae;
		}

		ae.outrec = outrec;
		OutPt op = new OutPt(pt, outrec);
		outrec.pts = op;
		return op;
	}

	private void UpdateEdgeIntoAEL(Active ae) {
		ae.bot = ae.top;
		ae.vertexTop = NextVertex(ae);
		ae.top = ae.vertexTop.pt;
		ae.curX = ae.bot.x;
		SetDx(ae);

		if (IsJoined(ae)) {
			Split(ae, ae.bot);
		}

		if (IsHorizontal(ae)) {
			return;
		}
		scanlineSet.add(ae.top.y);

		CheckJoinLeft(ae, ae.bot);
		CheckJoinRight(ae, ae.bot);
	}

	private static Active FindEdgeWithMatchingLocMin(Active e) {
		Active result = e.nextInAEL;
		while (result != null) {
			if (result.localMin.opEquals(e.localMin)) {
				return result;
			}
			if (!IsHorizontal(result) && e.bot.opNotEquals(result.bot)) {
				result = null;
			} else {
				result = result.nextInAEL;
			}
		}
		result = e.prevInAEL;
		while (result != null) {
			if (result.localMin.opEquals(e.localMin)) {
				return result;
			}
			if (!IsHorizontal(result) && e.bot.opNotEquals(result.bot)) {
				return null;
			}
			result = result.prevInAEL;
		}
		return result;
	}

	private OutPt IntersectEdges(Active ae1, Active ae2, Point64 pt) {
		OutPt resultOp = null;

		// MANAGE OPEN PATH INTERSECTIONS SEPARATELY ...
		if (hasOpenPaths && (IsOpen(ae1) || IsOpen(ae2))) {
			if (IsOpen(ae1) && IsOpen(ae2)) {
				return null;
			}
			// the following line avoids duplicating quite a bit of code
			if (IsOpen(ae2)) {
				RefObject<Active> tempRefae1 = new RefObject<>(ae1);
				RefObject<Active> tempRefae2 = new RefObject<>(ae2);
				SwapActives(tempRefae1, tempRefae2);
				ae2 = tempRefae2.argValue;
				ae1 = tempRefae1.argValue;
			}
			if (IsJoined(ae2)) {
				Split(ae2, pt); // needed for safety
			}

			if (cliptype == ClipType.Union) {
				if (!IsHotEdge(ae2)) {
					return null;
				}
			} else if (ae2.localMin.polytype == PathType.Subject) {
				return null;
			}

			switch (fillrule) {
				case Positive :
					if (ae2.windCount != 1) {
						return null;
					}
					break;
				case Negative :
					if (ae2.windCount != -1) {
						return null;
					}
					break;
				default :
					if (Math.abs(ae2.windCount) != 1) {
						return null;
					}
					break;
			}

			// toggle contribution ...
			if (IsHotEdge(ae1)) {
				resultOp = AddOutPt(ae1, pt);
				if (IsFront(ae1)) {
					ae1.outrec.frontEdge = null;
				} else {
					ae1.outrec.backEdge = null;
				}
				ae1.outrec = null;
			}

			// horizontal edges can pass under open paths at a LocMins
			else if (pt.opEquals(ae1.localMin.vertex.pt) && !IsOpenEnd(ae1.localMin.vertex)) {
				// find the other side of the LocMin and
				// if it's 'hot' join up with it ...
				Active ae3 = FindEdgeWithMatchingLocMin(ae1);
				if (ae3 != null && IsHotEdge(ae3)) {
					ae1.outrec = ae3.outrec;
					if (ae1.windDx > 0) {
						SetSides(ae3.outrec, ae1, ae3);
					} else {
						SetSides(ae3.outrec, ae3, ae1);
					}
					return ae3.outrec.pts;
				}

				resultOp = StartOpenPath(ae1, pt);
			} else {
				resultOp = StartOpenPath(ae1, pt);
			}

			return resultOp;
		}

		// MANAGING CLOSED PATHS FROM HERE ON
		if (IsJoined(ae1)) {
			Split(ae1, pt);
		}
		if (IsJoined(ae2)) {
			Split(ae2, pt);
		}

		// UPDATE WINDING COUNTS...

		int oldE1WindCount, oldE2WindCount;
		if (ae1.localMin.polytype == ae2.localMin.polytype) {
			if (fillrule == FillRule.EvenOdd) {
				oldE1WindCount = ae1.windCount;
				ae1.windCount = ae2.windCount;
				ae2.windCount = oldE1WindCount;
			} else {
				if (ae1.windCount + ae2.windDx == 0) {
					ae1.windCount = -ae1.windCount;
				} else {
					ae1.windCount += ae2.windDx;
				}
				if (ae2.windCount - ae1.windDx == 0) {
					ae2.windCount = -ae2.windCount;
				} else {
					ae2.windCount -= ae1.windDx;
				}
			}
		} else {
			if (fillrule != FillRule.EvenOdd) {
				ae1.windCount2 += ae2.windDx;
			} else {
				ae1.windCount2 = (ae1.windCount2 == 0 ? 1 : 0);
			}
			if (fillrule != FillRule.EvenOdd) {
				ae2.windCount2 -= ae1.windDx;
			} else {
				ae2.windCount2 = (ae2.windCount2 == 0 ? 1 : 0);
			}
		}

		switch (fillrule) {
			case Positive :
				oldE1WindCount = ae1.windCount;
				oldE2WindCount = ae2.windCount;
				break;
			case Negative :
				oldE1WindCount = -ae1.windCount;
				oldE2WindCount = -ae2.windCount;
				break;
			default :
				oldE1WindCount = Math.abs(ae1.windCount);
				oldE2WindCount = Math.abs(ae2.windCount);
				break;
		}

		boolean e1WindCountIs0or1 = oldE1WindCount == 0 || oldE1WindCount == 1;
		boolean e2WindCountIs0or1 = oldE2WindCount == 0 || oldE2WindCount == 1;

		if ((!IsHotEdge(ae1) && !e1WindCountIs0or1) || (!IsHotEdge(ae2) && !e2WindCountIs0or1)) {
			return null;
		}

		// NOW PROCESS THE INTERSECTION ...

		// if both edges are 'hot' ...
		if (IsHotEdge(ae1) && IsHotEdge(ae2)) {
			if ((oldE1WindCount != 0 && oldE1WindCount != 1) || (oldE2WindCount != 0 && oldE2WindCount != 1)
					|| (ae1.localMin.polytype != ae2.localMin.polytype && cliptype != ClipType.Xor)) {
				resultOp = AddLocalMaxPoly(ae1, ae2, pt);
			} else if (IsFront(ae1) || (ae1.outrec == ae2.outrec)) {
				// this 'else if' condition isn't strictly needed but
				// it's sensible to split polygons that ony touch at
				// a common vertex (not at common edges).
				resultOp = AddLocalMaxPoly(ae1, ae2, pt);
				AddLocalMinPoly(ae1, ae2, pt);
			} else {
				// can't treat as maxima & minima
				resultOp = AddOutPt(ae1, pt);
				AddOutPt(ae2, pt);
				SwapOutrecs(ae1, ae2);
			}
		}

		// if one or other edge is 'hot' ...
		else if (IsHotEdge(ae1)) {
			resultOp = AddOutPt(ae1, pt);
			SwapOutrecs(ae1, ae2);
		} else if (IsHotEdge(ae2)) {
			resultOp = AddOutPt(ae2, pt);
			SwapOutrecs(ae1, ae2);
		}

		// neither edge is 'hot'
		else {
			long e1Wc2, e2Wc2;
			switch (fillrule) {
				case Positive :
					e1Wc2 = ae1.windCount2;
					e2Wc2 = ae2.windCount2;
					break;
				case Negative :
					e1Wc2 = -ae1.windCount2;
					e2Wc2 = -ae2.windCount2;
					break;
				default :
					e1Wc2 = Math.abs(ae1.windCount2);
					e2Wc2 = Math.abs(ae2.windCount2);
					break;
			}

			if (!IsSamePolyType(ae1, ae2)) {
				resultOp = AddLocalMinPoly(ae1, ae2, pt);
			} else if (oldE1WindCount == 1 && oldE2WindCount == 1) {
				resultOp = null;
				switch (cliptype) {
					case Union :
						if (e1Wc2 > 0 && e2Wc2 > 0) {
							return null;
						}
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;

					case Difference :
						if (((GetPolyType(ae1) == PathType.Clip) && (e1Wc2 > 0) && (e2Wc2 > 0))
								|| ((GetPolyType(ae1) == PathType.Subject) && (e1Wc2 <= 0) && (e2Wc2 <= 0))) {
							resultOp = AddLocalMinPoly(ae1, ae2, pt);
						}

						break;

					case Xor :
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;

					default : // ClipType.Intersection:
						if (e1Wc2 <= 0 || e2Wc2 <= 0) {
							return null;
						}
						resultOp = AddLocalMinPoly(ae1, ae2, pt);
						break;
				}
			}
		}

		return resultOp;
	}

	private void DeleteFromAEL(Active ae) {
		Active prev = ae.prevInAEL;
		Active next = ae.nextInAEL;
		if (prev == null && next == null && (!actives.equals(ae))) {
			return; // already deleted
		}
		if (prev != null) {
			prev.nextInAEL = next;
		} else {
			actives = next;
		}
		if (next != null) {
			next.prevInAEL = prev;
		}
		// delete &ae;
	}

	private void AdjustCurrXAndCopyToSEL(long topY) {
		Active ae = actives;
		sel = ae;
		while (ae != null) {
			ae.prevInSEL = ae.prevInAEL;
			ae.nextInSEL = ae.nextInAEL;
			ae.jump = ae.nextInSEL;
			if (ae.joinWith == JoinWith.Left) {
				ae.curX = ae.prevInAEL.curX; // this also avoids complications
			} else {
				ae.curX = TopX(ae, topY);
			}
			// NB don't update ae.curr.y yet (see AddNewIntersectNode)
			ae = ae.nextInAEL;
		}
	}

	protected final void ExecuteInternal(ClipType ct, FillRule fillRule) {
		if (ct == ClipType.None) {
			return;
		}
		fillrule = fillRule;
		cliptype = ct;
		Reset();
		if (scanlineSet.isEmpty()) {
			return;
		}
		long y = scanlineSet.pollLast();
		while (succeeded) {
			InsertLocalMinimaIntoAEL(y);
			Active ae = null;
			OutObject<Active> tempOutae = new OutObject<>();
			while (PopHorz(tempOutae)) {
				ae = tempOutae.argValue;
				DoHorizontal(ae);
			}
			if (!horzSegList.isEmpty()) {
				ConvertHorzSegsToJoins();
				horzSegList.clear();
			}
			currentBotY = y; // bottom of scanbeam
			if (scanlineSet.isEmpty()) {
				break; // y new top of scanbeam
			}
			y = scanlineSet.pollLast();
			DoIntersections(y);
			DoTopOfScanbeam(y);
			OutObject<Active> tempOutae2 = new OutObject<>();
			while (PopHorz(tempOutae2)) {
				ae = tempOutae2.argValue;
				DoHorizontal(ae);
			}
		}

		if (succeeded) {
			ProcessHorzJoins();
		}
	}

	private void DoIntersections(long topY) {
		if (BuildIntersectList(topY)) {
			ProcessIntersectList();
			DisposeIntersectNodes();
		}
	}

	private void DisposeIntersectNodes() {
		intersectList.clear();
	}

	private void AddNewIntersectNode(Active ae1, Active ae2, long topY) {
		Point64 ip = new Point64();
		if (!InternalClipper.GetIntersectPt(ae1.bot, ae1.top, ae2.bot, ae2.top, ip)) {
			ip = new Point64(ae1.curX, topY);
		}

		if (ip.y > currentBotY || ip.y < topY) {
			double absDx1 = Math.abs(ae1.dx);
			double absDx2 = Math.abs(ae2.dx);
			if (absDx1 > 100 && absDx2 > 100) {
				if (absDx1 > absDx2) {
					ip = InternalClipper.GetClosestPtOnSegment(ip, ae1.bot, ae1.top);
				} else {
					ip = InternalClipper.GetClosestPtOnSegment(ip, ae2.bot, ae2.top);
				}
			} else if (absDx1 > 100) {
				ip = InternalClipper.GetClosestPtOnSegment(ip, ae1.bot, ae1.top);
			} else if (absDx2 > 100) {
				ip = InternalClipper.GetClosestPtOnSegment(ip, ae2.bot, ae2.top);
			} else {
				if (ip.y < topY) {
					ip.y = topY;
				} else {
					ip.y = currentBotY;
				}
				if (absDx1 < absDx2) {
					ip.x = TopX(ae1, ip.y);
				} else {
					ip.x = TopX(ae2, ip.y);
				}
			}
		}
		IntersectNode node = new IntersectNode(ip, ae1, ae2);
		intersectList.add(node);
	}

	private Active ExtractFromSEL(Active ae) {
		Active res = ae.nextInSEL;
		if (res != null) {
			res.prevInSEL = ae.prevInSEL;
		}
		ae.prevInSEL.nextInSEL = res;
		return res;
	}

	private static void Insert1Before2InSEL(Active ae1, Active ae2) {
		ae1.prevInSEL = ae2.prevInSEL;
		if (ae1.prevInSEL != null) {
			ae1.prevInSEL.nextInSEL = ae1;
		}
		ae1.nextInSEL = ae2;
		ae2.prevInSEL = ae1;
	}

	private boolean BuildIntersectList(long topY) {
		if (actives == null || actives.nextInAEL == null) {
			return false;
		}

		// Calculate edge positions at the top of the current scanbeam, and from this
		// we will determine the intersections required to reach these new positions.
		AdjustCurrXAndCopyToSEL(topY);

		// Find all edge intersections in the current scanbeam using a stable merge
		// sort that ensures only adjacent edges are intersecting. Intersect info is
		// stored in FIntersectList ready to be processed in ProcessIntersectList.
		// Re merge sorts see https://stackoverflow.com/a/46319131/359538

		Active left = sel, right, lEnd, rEnd, currBase, prevBase, tmp;

		while (left.jump != null) {
			prevBase = null;
			while (left != null && left.jump != null) {
				currBase = left;
				right = left.jump;
				lEnd = right;
				rEnd = right.jump;
				left.jump = rEnd;
				while (left != lEnd && right != rEnd) {
					if (right.curX < left.curX) {
						tmp = right.prevInSEL;
						for (;;) {
							AddNewIntersectNode(tmp, right, topY);
							if (left.equals(tmp)) {
								break;
							}
							tmp = tmp.prevInSEL;
						}

						tmp = right;
						right = ExtractFromSEL(tmp);
						lEnd = right;
						Insert1Before2InSEL(tmp, left);
						if (left.equals(currBase)) {
							currBase = tmp;
							currBase.jump = rEnd;
							if (prevBase == null) {
								sel = currBase;
							} else {
								prevBase.jump = currBase;
							}
						}
					} else {
						left = left.nextInSEL;
					}
				}

				prevBase = currBase;
				left = rEnd;
			}
			left = sel;
		}

		return !intersectList.isEmpty();
	}

	private void ProcessIntersectList() {
		// We now have a list of intersections required so that edges will be
		// correctly positioned at the top of the scanbeam. However, it's important
		// that edge intersections are processed from the bottom up, but it's also
		// crucial that intersections only occur between adjacent edges.

		// First we do a quicksort so intersections proceed in a bottom up order ...
		intersectList.sort((a, b) -> {
			if (a.pt.y == b.pt.y) {
				if (a.pt.x == b.pt.x) {
					return 0;
				}
				return (a.pt.x < b.pt.x) ? -1 : 1;
			}
			return (a.pt.y > b.pt.y) ? -1 : 1;
		});

		// Now as we process these intersections, we must sometimes adjust the order
		// to ensure that intersecting edges are always adjacent ...
		for (int i = 0; i < intersectList.size(); ++i) {
			if (!EdgesAdjacentInAEL(intersectList.get(i))) {
				int j = i + 1;
				while (!EdgesAdjacentInAEL(intersectList.get(j))) {
					j++;
				}
				// swap
				Collections.swap(intersectList, i, j);
			}

			IntersectNode node = intersectList.get(i);
			IntersectEdges(node.edge1, node.edge2, node.pt);
			SwapPositionsInAEL(node.edge1, node.edge2);

			node.edge1.curX = node.pt.x;
			node.edge2.curX = node.pt.x;
			CheckJoinLeft(node.edge2, node.pt, true);
			CheckJoinRight(node.edge1, node.pt, true);
		}
	}

	private void SwapPositionsInAEL(Active ae1, Active ae2) {
		// preconditon: ae1 must be immediately to the left of ae2
		Active next = ae2.nextInAEL;
		if (next != null) {
			next.prevInAEL = ae1;
		}
		Active prev = ae1.prevInAEL;
		if (prev != null) {
			prev.nextInAEL = ae2;
		}
		ae2.prevInAEL = prev;
		ae2.nextInAEL = ae1;
		ae1.prevInAEL = ae2;
		ae1.nextInAEL = next;
		if (ae2.prevInAEL == null) {
			actives = ae2;
		}
	}

	private static boolean ResetHorzDirection(Active horz, @Nullable Vertex vertexMax, OutObject<Long> leftX, OutObject<Long> rightX) {
		if (horz.bot.x == horz.top.x) {
			// the horizontal edge is going nowhere ...
			leftX.argValue = horz.curX;
			rightX.argValue = horz.curX;
			Active ae = horz.nextInAEL;
			while (ae != null && ae.vertexTop != vertexMax) {
				ae = ae.nextInAEL;
			}
			return ae != null;
		}

		if (horz.curX < horz.top.x) {
			leftX.argValue = horz.curX;
			rightX.argValue = horz.top.x;
			return true;
		}
		leftX.argValue = horz.top.x;
		rightX.argValue = horz.curX;
		return false; // right to left
	}

	private static boolean HorzIsSpike(Active horz) {
		Point64 nextPt = NextVertex(horz).pt;
		return (horz.bot.x < horz.top.x) != (horz.top.x < nextPt.x);
	}

	private void TrimHorz(Active horzEdge, boolean preserveCollinear) {
		boolean wasTrimmed = false;
		Point64 pt = NextVertex(horzEdge).pt;

		while (pt.y == horzEdge.top.y) {
			// always trim 180 deg. spikes (in closed paths)
			// but otherwise break if preserveCollinear = true
			if (preserveCollinear && (pt.x < horzEdge.top.x) != (horzEdge.bot.x < horzEdge.top.x)) {
				break;
			}

			horzEdge.vertexTop = NextVertex(horzEdge);
			horzEdge.top = pt;
			wasTrimmed = true;
			if (IsMaxima(horzEdge)) {
				break;
			}
			pt = NextVertex(horzEdge).pt;
		}
		if (wasTrimmed) {
			SetDx(horzEdge); // +/-infinity
		}
	}

	private void AddToHorzSegList(OutPt op) {
		if (op.outrec.isOpen) {
			return;
		}
		horzSegList.add(new HorzSegment(op));
	}

	private OutPt GetLastOp(Active hotEdge) {
		OutRec outrec = hotEdge.outrec;
		return (hotEdge == outrec.frontEdge) ? outrec.pts : outrec.pts.next;
	}

	private void DoHorizontal(Active horz)
	/*-
	 * Notes: Horizontal edges (HEs) at scanline intersections (i.e. at the top or  *
	 * bottom of a scanbeam) are processed as if layered.The order in which HEs     *
	 * are processed doesn't matter. HEs intersect with the bottom vertices of      *
	 * other HEs[#] and with non-horizontal edges [*]. Once these intersections     *
	 * are completed, intermediate HEs are 'promoted' to the next edge in their     *
	 * bounds, and they in turn may be intersected[%] by other HEs.                 *
	 *                                                                              *
	 * eg: 3 horizontals at a scanline:    /   |                     /           /  *
	 *              |                     /    |     (HE3)o ========%========== o   *
	 *              o ======= o(HE2)     /     |         /         /                *
	 *          o ============#=========*======*========#=========o (HE1)           *
	 *         /              |        /       |       /                            *
	 *******************************************************************************/
	{
		Point64 pt;
		boolean horzIsOpen = IsOpen(horz);
		long Y = horz.bot.y;

		@Nullable
		Vertex vertexMax = horzIsOpen ? GetCurrYMaximaVertex_Open(horz) : GetCurrYMaximaVertex(horz);

		// remove 180 deg.spikes and also simplify
		// consecutive horizontals when PreserveCollinear = true
		if (vertexMax != null && !horzIsOpen && vertexMax != horz.vertexTop) {
			TrimHorz(horz, getPreserveCollinear());
		}

		long leftX;
		OutObject<Long> tempOutleftX = new OutObject<>();
		long rightX;
		OutObject<Long> tempOutrightX = new OutObject<>();
		boolean isLeftToRight = ResetHorzDirection(horz, vertexMax, tempOutleftX, tempOutrightX);
		rightX = tempOutrightX.argValue;
		leftX = tempOutleftX.argValue;

		if (IsHotEdge(horz)) {
			OutPt op = AddOutPt(horz, new Point64(horz.curX, Y));
			AddToHorzSegList(op);
		}
		@Nullable
		OutRec currOutrec = horz.outrec;

		for (;;) {
			// loops through consec. horizontal edges (if open)
			@Nullable
			Active ae = isLeftToRight ? horz.nextInAEL : horz.prevInAEL;

			while (ae != null) {
				if (ae.vertexTop == vertexMax) {
					// do this first!!
					if (IsHotEdge(horz) && IsJoined(ae)) {
						Split(ae, ae.top);
					}

					if (IsHotEdge(horz)) {
						while (horz.vertexTop != vertexMax) {
							AddOutPt(horz, horz.top);
							UpdateEdgeIntoAEL(horz);
						}
						if (isLeftToRight) {
							AddLocalMaxPoly(horz, ae, horz.top);
						} else {
							AddLocalMaxPoly(ae, horz, horz.top);
						}
					}
					DeleteFromAEL(ae);
					DeleteFromAEL(horz);
					return;
				}

				// if horzEdge is a maxima, keep going until we reach
				// its maxima pair, otherwise check for break conditions
				if (vertexMax != horz.vertexTop || IsOpenEnd(horz)) {
					// otherwise stop when 'ae' is beyond the end of the horizontal line
					if ((isLeftToRight && ae.curX > rightX) || (!isLeftToRight && ae.curX < leftX)) {
						break;
					}

					if (ae.curX == horz.top.x && !IsHorizontal(ae)) {
						pt = NextVertex(horz).pt;

						// to maximize the possibility of putting open edges into
						// solutions, we'll only break if it's past HorzEdge's end
						if (IsOpen(ae) && !IsSamePolyType(ae, horz) && !IsHotEdge(ae)) {
							if ((isLeftToRight && (TopX(ae, pt.y) > pt.x)) || (!isLeftToRight && (TopX(ae, pt.y) < pt.x))) {
								break;
							}
						}
						// otherwise for edges at horzEdge's end, only stop when horzEdge's
						// outslope is greater than e's slope when heading right or when
						// horzEdge's outslope is less than e's slope when heading left.
						else if ((isLeftToRight && (TopX(ae, pt.y) >= pt.x)) || (!isLeftToRight && (TopX(ae, pt.y) <= pt.x))) {
							break;
						}
					}
				}

				pt = new Point64(ae.curX, Y);

				if (isLeftToRight) {
					IntersectEdges(horz, ae, pt);
					SwapPositionsInAEL(horz, ae);
					horz.curX = ae.curX;
					ae = horz.nextInAEL;
				} else {
					IntersectEdges(ae, horz, pt);
					SwapPositionsInAEL(ae, horz);
					horz.curX = ae.curX;
					ae = horz.prevInAEL;
				}

				if (IsHotEdge(horz) && (horz.outrec != currOutrec)) {
					currOutrec = horz.outrec;
					AddToHorzSegList(GetLastOp(horz));
				}

			} // we've reached the end of this horizontal

			// check if we've finished looping
			// through consecutive horizontals
			if (horzIsOpen && IsOpenEnd(horz)) {
				// ie open at top
				if (IsHotEdge(horz)) {
					AddOutPt(horz, horz.top);
					if (IsFront(horz)) {
						horz.outrec.frontEdge = null;
					} else {
						horz.outrec.backEdge = null;
					}
					horz.outrec = null;
				}
				DeleteFromAEL(horz);
				return;
			}

			if (NextVertex(horz).pt.y != horz.top.y) {
				break;
			}

			// still more horizontals in bound to process ...
			if (IsHotEdge(horz)) {
				AddOutPt(horz, horz.top);
			}
			UpdateEdgeIntoAEL(horz);

			if (getPreserveCollinear() && !horzIsOpen && HorzIsSpike(horz)) {
				TrimHorz(horz, true);
			}

			OutObject<Long> tempOutleftX2 = new OutObject<>();
			OutObject<Long> tempOutrightX2 = new OutObject<>();
			isLeftToRight = ResetHorzDirection(horz, vertexMax, tempOutleftX2, tempOutrightX2);
			rightX = tempOutrightX2.argValue;
			leftX = tempOutleftX2.argValue;

		} // end for loop and end of (possible consecutive) horizontals

		if (IsHotEdge(horz)) {
			AddOutPt(horz, horz.top);
		}
		UpdateEdgeIntoAEL(horz); // this is the end of an intermediate horiz.
	}

	private void DoTopOfScanbeam(long y) {
		sel = null; // sel is reused to flag horizontals (see PushHorz below)
		Active ae = actives;
		while (ae != null) {
			// NB 'ae' will never be horizontal here
			if (ae.top.y == y) {
				ae.curX = ae.top.x;
				if (IsMaxima(ae)) {
					ae = DoMaxima(ae); // TOP OF BOUND (MAXIMA)
					continue;
				}

				// INTERMEDIATE VERTEX ...
				if (IsHotEdge(ae)) {
					AddOutPt(ae, ae.top);
				}
				UpdateEdgeIntoAEL(ae);
				if (IsHorizontal(ae)) {
					PushHorz(ae); // horizontals are processed later
				}
			} else { // i.e. not the top of the edge
				ae.curX = TopX(ae, y);
			}

			ae = ae.nextInAEL;
		}
	}

	private Active DoMaxima(Active ae) {
		Active prevE = null;
		Active nextE, maxPair = null;
		prevE = ae.prevInAEL;
		nextE = ae.nextInAEL;

		if (IsOpenEnd(ae)) {
			if (IsHotEdge(ae)) {
				AddOutPt(ae, ae.top);
			}
			if (!IsHorizontal(ae)) {
				if (IsHotEdge(ae)) {
					if (IsFront(ae)) {
						ae.outrec.frontEdge = null;
					} else {
						ae.outrec.backEdge = null;
					}
					ae.outrec = null;
				}
				DeleteFromAEL(ae);
			}
			return nextE;
		}

		maxPair = GetMaximaPair(ae);
		if (maxPair == null) {
			return nextE; // eMaxPair is horizontal
		}

		if (IsJoined(ae)) {
			Split(ae, ae.top);
		}
		if (IsJoined(maxPair)) {
			Split(maxPair, maxPair.top);
		}

		// only non-horizontal maxima here.
		// process any edges between maxima pair ...
		while (!nextE.equals(maxPair)) {
			IntersectEdges(ae, nextE, ae.top);
			SwapPositionsInAEL(ae, nextE);
			nextE = ae.nextInAEL;
		}

		if (IsOpen(ae)) {
			if (IsHotEdge(ae)) {
				AddLocalMaxPoly(ae, maxPair, ae.top);
			}
			DeleteFromAEL(maxPair);
			DeleteFromAEL(ae);
			return (prevE != null ? prevE.nextInAEL : actives);
		}

		// here ae.nextInAel == ENext == EMaxPair ...
		if (IsHotEdge(ae)) {
			AddLocalMaxPoly(ae, maxPair, ae.top);
		}

		DeleteFromAEL(ae);
		DeleteFromAEL(maxPair);
		return (prevE != null ? prevE.nextInAEL : actives);
	}

	private static boolean IsJoined(Active e) {
		return e.joinWith != JoinWith.None;
	}

	private void Split(Active e, Point64 currPt) {
		if (e.joinWith == JoinWith.Right) {
			e.joinWith = JoinWith.None;
			e.nextInAEL.joinWith = JoinWith.None;
			AddLocalMinPoly(e, e.nextInAEL, currPt, true);
		} else {
			e.joinWith = JoinWith.None;
			e.prevInAEL.joinWith = JoinWith.None;
			AddLocalMinPoly(e.prevInAEL, e, currPt, true);
		}
	}

	private void CheckJoinLeft(Active e, Point64 pt) {
		CheckJoinLeft(e, pt, false);
	}

	private void CheckJoinLeft(Active e, Point64 pt, boolean checkCurrX) {
		@Nullable
		Active prev = e.prevInAEL;
		if (prev == null || IsOpen(e) || IsOpen(prev) || !IsHotEdge(e) || !IsHotEdge(prev) || pt.y < e.top.y + 2 || pt.y < prev.top.y + 2) {
			return;
		}

		if (checkCurrX) {
			if (Clipper.PerpendicDistFromLineSqrd(pt, prev.bot, prev.top) > 0.25) {
				return;
			}
		} else if (e.curX != prev.curX) {
			return;
		}
		if (InternalClipper.CrossProduct(e.top, pt, prev.top) != 0) {
			return;
		}

		if (e.outrec.idx == prev.outrec.idx) {
			AddLocalMaxPoly(prev, e, pt);
		} else if (e.outrec.idx < prev.outrec.idx) {
			JoinOutrecPaths(e, prev);
		} else {
			JoinOutrecPaths(prev, e);
		}
		prev.joinWith = JoinWith.Right;
		e.joinWith = JoinWith.Left;
	}

	private void CheckJoinRight(Active e, Point64 pt) {
		CheckJoinRight(e, pt, false);
	}

	private void CheckJoinRight(Active e, Point64 pt, boolean checkCurrX) {
		@Nullable
		Active next = e.nextInAEL;
		if (IsOpen(e) || !IsHotEdge(e) || IsJoined(e) || next == null || IsOpen(next) || !IsHotEdge(next) || pt.y < e.top.y + 2
				|| pt.y < next.top.y + 2) {
			return;
		}

		if (checkCurrX) {
			if (Clipper.PerpendicDistFromLineSqrd(pt, next.bot, next.top) > 0.25) {
				return;
			}
		} else if (e.curX != next.curX) {
			return;
		}
		if (InternalClipper.CrossProduct(e.top, pt, next.top) != 0) {
			return;
		}

		if (e.outrec.idx == next.outrec.idx) {
			AddLocalMaxPoly(e, next, pt);
		} else if (e.outrec.idx < next.outrec.idx) {
			JoinOutrecPaths(e, next);
		} else {
			JoinOutrecPaths(next, e);
		}
		e.joinWith = JoinWith.Right;
		next.joinWith = JoinWith.Left;
	}

	private static void FixOutRecPts(OutRec outrec) {
		OutPt op = outrec.pts;
		do {
			op.outrec = outrec;
			op = op.next;
		} while (op != outrec.pts);
	}

	private static boolean SetHorzSegHeadingForward(HorzSegment hs, OutPt opP, OutPt opN) {
		if (opP.pt.x == opN.pt.x) {
			return false;
		}
		if (opP.pt.x < opN.pt.x) {
			hs.leftOp = opP;
			hs.rightOp = opN;
			hs.leftToRight = true;
		} else {
			hs.leftOp = opN;
			hs.rightOp = opP;
			hs.leftToRight = false;
		}
		return true;
	}

	private static boolean UpdateHorzSegment(HorzSegment hs) {
		OutPt op = hs.leftOp;
		OutRec outrec = GetRealOutRec(op.outrec);
		boolean outrecHasEdges = outrec.frontEdge != null;
		long currY = op.pt.y;
		OutPt opP = op, opN = op;
		if (outrecHasEdges) {
			OutPt opA = outrec.pts, opZ = opA.next;
			while (opP != opZ && opP.prev.pt.y == currY) {
				opP = opP.prev;
			}
			while (opN != opA && opN.next.pt.y == currY) {
				opN = opN.next;
			}
		} else {
			while (opP.prev != opN && opP.prev.pt.y == currY) {
				opP = opP.prev;
			}
			while (opN.next != opP && opN.next.pt.y == currY) {
				opN = opN.next;
			}
		}
		boolean result = SetHorzSegHeadingForward(hs, opP, opN) && hs.leftOp.horz == null;

		if (result) {
			hs.leftOp.horz = hs;
		} else {
			hs.rightOp = null; // (for sorting)
		}
		return result;
	}

	private static OutPt DuplicateOp(OutPt op, boolean insertAfter) {
		OutPt result = new OutPt(op.pt, op.outrec);
		if (insertAfter) {
			result.next = op.next;
			result.next.prev = result;
			result.prev = op;
			op.next = result;
		} else {
			result.prev = op.prev;
			result.prev.next = result;
			result.next = op;
			op.prev = result;
		}
		return result;
	}

	private void ConvertHorzSegsToJoins() {
		int k = 0;
		for (HorzSegment hs : horzSegList) {
			if (UpdateHorzSegment(hs)) {
				k++;
			}
		}
		if (k < 2) {
			return;
		}
		horzSegList.sort(new HorzSegSorter());

		for (int i = 0; i < k - 1; i++) {
			HorzSegment hs1 = horzSegList.get(i);
			// for each HorzSegment, find others that overlap
			for (int j = i + 1; j < k; j++) {
				HorzSegment hs2 = horzSegList.get(j);
				if (hs2.leftOp.pt.x >= hs1.rightOp.pt.x) {
					break;
				}
				if (hs2.leftToRight == hs1.leftToRight || (hs2.rightOp.pt.x <= hs1.leftOp.pt.x)) {
					continue;
				}
				long currY = hs1.leftOp.pt.y;
				if ((hs1).leftToRight) {
					while (hs1.leftOp.next.pt.y == currY && hs1.leftOp.next.pt.x <= hs2.leftOp.pt.x) {
						hs1.leftOp = hs1.leftOp.next;
					}
					while (hs2.leftOp.prev.pt.y == currY && hs2.leftOp.prev.pt.x <= hs1.leftOp.pt.x) {
						(hs2).leftOp = (hs2).leftOp.prev;
					}
					HorzJoin join = new HorzJoin(DuplicateOp((hs1).leftOp, true), DuplicateOp((hs2).leftOp, false));
					horzJoinList.add(join);
				} else {
					while (hs1.leftOp.prev.pt.y == currY && hs1.leftOp.prev.pt.x <= hs2.leftOp.pt.x) {
						hs1.leftOp = hs1.leftOp.prev;
					}
					while (hs2.leftOp.next.pt.y == currY && hs2.leftOp.next.pt.x <= (hs1).leftOp.pt.x) {
						hs2.leftOp = (hs2).leftOp.next;
					}
					HorzJoin join = new HorzJoin(DuplicateOp((hs2).leftOp, true), DuplicateOp((hs1).leftOp, false));
					horzJoinList.add(join);
				}
			}
		}
	}

	private static Rect64 GetBounds(OutPt op) {
		Rect64 result = new Rect64(op.pt.x, op.pt.y, op.pt.x, op.pt.y);
		OutPt op2 = op.next;
		while (op2 != op) {
			if (op2.pt.x < result.left) {
				result.left = op2.pt.x;
			} else if (op2.pt.x > result.right) {
				result.right = op2.pt.x;
			}
			if (op2.pt.y < result.top) {
				result.top = op2.pt.y;
			} else if (op2.pt.y > result.bottom) {
				result.bottom = op2.pt.y;
			}
			op2 = op2.next;
		}
		return result;
	}

	private static PointInPolygonResult PointInOpPolygon(Point64 pt, OutPt op) {
		if (op == op.next || op.prev == op.next) {
			return PointInPolygonResult.IsOutside;
		}
		OutPt op2 = op;
		do {
			if (op.pt.y != pt.y) {
				break;
			}
			op = op.next;
		} while (op != op2);
		if (op.pt.y == pt.y) { // not a proper polygon
			return PointInPolygonResult.IsOutside;
		}

		// must be above or below to get here
		boolean isAbove = op.pt.y < pt.y, startingAbove = isAbove;
		int val = 0;

		op2 = op.next;
		while (op2 != op) {
			if (isAbove) {
				while (op2 != op && op2.pt.y < pt.y) {
					op2 = op2.next;
				}
			} else {
				while (op2 != op && op2.pt.y > pt.y) {
					op2 = op2.next;
				}
			}
			if (op2 == op) {
				break;
			}

			// must have touched or crossed the pt.y horizonal
			// and this must happen an even number of times

			if (op2.pt.y == pt.y) // touching the horizontal
			{
				if (op2.pt.x == pt.x || (op2.pt.y == op2.prev.pt.y && (pt.x < op2.prev.pt.x) != (pt.x < op2.pt.x))) {
					return PointInPolygonResult.IsOn;
				}
				op2 = op2.next;
				if (op2 == op) {
					break;
				}
				continue;
			}

			if (op2.pt.x <= pt.x || op2.prev.pt.x <= pt.x) {
				if ((op2.prev.pt.x < pt.x && op2.pt.x < pt.x)) {
					val = 1 - val; // toggle val
				} else {
					double d = InternalClipper.CrossProduct(op2.prev.pt, op2.pt, pt);
					if (d == 0) {
						return PointInPolygonResult.IsOn;
					}
					if ((d < 0) == isAbove) {
						val = 1 - val;
					}
				}
			}
			isAbove = !isAbove;
			op2 = op2.next;
		}

		if (isAbove != startingAbove) {
			double d = InternalClipper.CrossProduct(op2.prev.pt, op2.pt, pt);
			if (d == 0) {
				return PointInPolygonResult.IsOn;
			}
			if ((d < 0) == isAbove) {
				val = 1 - val;
			}
		}

		if (val == 0) {
			return PointInPolygonResult.IsOutside;
		} else {
			return PointInPolygonResult.IsInside;
		}
	}

	private static boolean Path1InsidePath2(OutPt op1, OutPt op2) {
		// we need to make some accommodation for rounding errors
		// so we won't jump if the first vertex is found outside
		int outsideCnt = 0;
		OutPt op = op1;
		do {
			PointInPolygonResult result = PointInOpPolygon(op.pt, op2);
			if (result == PointInPolygonResult.IsOutside) {
				++outsideCnt;
			} else if (result == PointInPolygonResult.IsInside) {
				--outsideCnt;
			}
			op = op.next;
		} while (op != op1 && Math.abs(outsideCnt) < 2);
		if (Math.abs(outsideCnt) > 1) {
			return (outsideCnt < 0);
		}
		// since path1's location is still equivocal, check its midpoint
		Point64 mp = GetBounds(op).MidPoint();
		return PointInOpPolygon(mp, op2) == PointInPolygonResult.IsInside;
	}

	private void ProcessHorzJoins() {
		for (HorzJoin j : horzJoinList) {
			OutRec or1 = GetRealOutRec(j.op1.outrec);
			OutRec or2 = GetRealOutRec(j.op2.outrec);

			OutPt op1b = j.op1.next;
			OutPt op2b = j.op2.prev;
			j.op1.next = j.op2;
			j.op2.prev = j.op1;
			op1b.prev = op2b;
			op2b.next = op1b;

			if (or1 == or2) {
				or2 = new OutRec();
				or2.pts = op1b;
				FixOutRecPts(or2);
				if (or1.pts.outrec == or2) {
					or1.pts = j.op1;
					or1.pts.outrec = or1;
				}

				if (usingPolytree) {
					if (Path1InsidePath2(or2.pts, or1.pts)) {
						SetOwner(or2, or1);
					} else if (Path1InsidePath2(or1.pts, or2.pts)) {
						SetOwner(or1, or2);
					} else {
						or2.owner = or1;
					}
				} else {
					or2.owner = or1;
				}

				outrecList.add(or2);
			} else {
				or2.pts = null;
				if (usingPolytree) {
					SetOwner(or2, or1);
				} else {
					or2.owner = or1;
				}
			}
		}
	}

	private static boolean PtsReallyClose(Point64 pt1, Point64 pt2) {
		return (Math.abs(pt1.x - pt2.x) < 2) && (Math.abs(pt1.y - pt2.y) < 2);
	}

	private static boolean IsVerySmallTriangle(OutPt op) {
		return op.next.next == op.prev
				&& (PtsReallyClose(op.prev.pt, op.next.pt) || PtsReallyClose(op.pt, op.next.pt) || PtsReallyClose(op.pt, op.prev.pt));
	}

	private static boolean IsValidClosedPath(@Nullable OutPt op) {
		return (op != null && op.next != op && (op.next != op.prev || !IsVerySmallTriangle(op)));
	}

	private static @Nullable OutPt DisposeOutPt(OutPt op) {
		@Nullable
		OutPt result = (op.next == op ? null : op.next);
		op.prev.next = op.next;
		op.next.prev = op.prev;
		// op == null;
		return result;
	}

	private void CleanCollinear(OutRec outrec) {
		outrec = GetRealOutRec(outrec);

		if (outrec == null || outrec.isOpen) {
			return;
		}

		if (!IsValidClosedPath(outrec.pts)) {
			outrec.pts = null;
			return;
		}

		OutPt startOp = outrec.pts;
		OutPt op2 = startOp;
		for (;;) {
			// NB if preserveCollinear == true, then only remove 180 deg. spikes
			if ((InternalClipper.CrossProduct(op2.prev.pt, op2.pt, op2.next.pt) == 0)
					&& ((op2.pt.opEquals(op2.prev.pt)) || (op2.pt.opEquals(op2.next.pt)) || !getPreserveCollinear()
							|| (InternalClipper.DotProduct(op2.prev.pt, op2.pt, op2.next.pt) < 0))) {
				if (op2.equals(outrec.pts)) {
					outrec.pts = op2.prev;
				}
				op2 = DisposeOutPt(op2);
				if (!IsValidClosedPath(op2)) {
					outrec.pts = null;
					return;
				}
				startOp = op2;
				continue;
			}
			op2 = op2.next;
			if (op2.equals(startOp)) {
				break;
			}
		}
		FixSelfIntersects(outrec);
	}

	private void DoSplitOp(OutRec outrec, OutPt splitOp) {
		// splitOp.prev <=> splitOp &&
		// splitOp.next <=> splitOp.next.next are intersecting
		OutPt prevOp = splitOp.prev;
		OutPt nextNextOp = splitOp.next.next;
		outrec.pts = prevOp;
//		OutPt result = prevOp;

		PointD tmp = new PointD();
		InternalClipper.GetIntersectPoint(prevOp.pt, splitOp.pt, splitOp.next.pt, nextNextOp.pt, tmp);
		Point64 ip = new Point64(tmp);

		double area1 = Area(prevOp);
		double absArea1 = Math.abs(area1);

		if (absArea1 < 2) {
			outrec.pts = null;
			return;
		}

		// nb: area1 is the path's area *before* splitting, whereas area2 is
		// the area of the triangle containing splitOp & splitOp.next.
		// So the only way for these areas to have the same sign is if
		// the split triangle is larger than the path containing prevOp or
		// if there's more than one self=intersection.
		double area2 = AreaTriangle(ip, splitOp.pt, splitOp.next.pt);
		double absArea2 = Math.abs(area2);

		// de-link splitOp and splitOp.next from the path
		// while inserting the intersection point
		if (ip.opEquals(prevOp.pt) || ip.opEquals(nextNextOp.pt)) {
			nextNextOp.prev = prevOp;
			prevOp.next = nextNextOp;
		} else {
			OutPt newOp2 = new OutPt(ip, outrec);
			newOp2.prev = prevOp;
			newOp2.next = nextNextOp;
			nextNextOp.prev = newOp2;
			prevOp.next = newOp2;
		}

		if (absArea2 > 1 && (absArea2 > absArea1 || ((area2 > 0) == (area1 > 0)))) {
			OutRec newOutRec = NewOutRec();
			newOutRec.owner = outrec.owner;
			splitOp.outrec = newOutRec;
			splitOp.next.outrec = newOutRec;

			if (usingPolytree) {
				if (outrec.splits == null) {
					outrec.splits = new ArrayList<>();
				}
				outrec.splits.add(newOutRec.idx);
			}

			OutPt newOp = new OutPt(ip, newOutRec);
			newOp.prev = splitOp.next;
			newOp.next = splitOp;
			newOutRec.pts = newOp;
			splitOp.prev = newOp;
			splitOp.next.next = newOp;
		}
		// else { splitOp = null; splitOp.next = null; }
	}

	private void FixSelfIntersects(OutRec outrec) {
		OutPt op2 = outrec.pts;
		for (;;) {
			// triangles can't self-intersect
			if (op2.prev == op2.next.next) {
				break;
			}
			if (InternalClipper.SegsIntersect(op2.prev.pt, op2.pt, op2.next.pt, op2.next.next.pt)) {
				DoSplitOp(outrec, op2);
				if (outrec.pts == null) {
					return;
				}
				op2 = outrec.pts;
				continue;
			} else {
				op2 = op2.next;
			}
			if (op2 == outrec.pts) {
				break;
			}
		}
	}

	public static boolean BuildPath(@Nullable OutPt op, boolean reverse, boolean isOpen, Path64 path) {
		if (op == null || op.next == op || (!isOpen && op.next == op.prev)) {
			return false;
		}
		path.clear();

		Point64 lastPt;
		OutPt op2;
		if (reverse) {
			lastPt = op.pt;
			op2 = op.prev;
		} else {
			op = op.next;
			lastPt = op.pt;
			op2 = op.next;
		}
		path.add(lastPt);

		while (op2 != op) {
			if (op2.pt.opNotEquals(lastPt)) {
				lastPt = op2.pt;
				path.add(lastPt);
			}
			if (reverse) {
				op2 = op2.prev;
			} else {
				op2 = op2.next;
			}
		}

		if (path.size() == 3 && IsVerySmallTriangle(op2)) {
			return false;
		} else {
			return true;
		}
	}

	protected final boolean BuildPaths(Paths64 solutionClosed, Paths64 solutionOpen) {
		solutionClosed.clear();
		solutionOpen.clear();

		int i = 0;
		// outrecList.Count is not static here because
		// CleanCollinear can indirectly add additional OutRec
		while (i < outrecList.size()) {
			OutRec outrec = outrecList.get(i++);
			if (outrec.pts == null) {
				continue;
			}

			Path64 path = new Path64();
			if (outrec.isOpen) {
				if (BuildPath(outrec.pts, getReverseSolution(), true, path)) {
					solutionOpen.add(path);
				}
			} else {
				CleanCollinear(outrec);
				// closed paths should always return a Positive orientation
				// except when reverseSolution == true
				if (BuildPath(outrec.pts, getReverseSolution(), false, path)) {
					solutionClosed.add(path);
				}
			}
		}
		return true;
	}

	public static Rect64 GetBounds(Path64 path) {
		if (path.isEmpty()) {
			return new Rect64();
		}
		Rect64 result = Clipper.InvalidRect64.clone();
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
		return result;
	}

	private boolean CheckBounds(OutRec outrec) {
		if (outrec.pts == null) {
			return false;
		}
		if (!outrec.bounds.IsEmpty()) {
			return true;
		}
		CleanCollinear(outrec);
		if (outrec.pts == null || !BuildPath(outrec.pts, getReverseSolution(), false, outrec.path)) {
			return false;
		}
		outrec.bounds = GetBounds(outrec.path);
		return true;
	}

	private void RecursiveCheckOwners(OutRec outrec, PolyPathBase polypath) {
		// pre-condition: outrec will have valid bounds
		// post-condition: if a valid path, outrec will have a polypath

		if (outrec.polypath != null || outrec.bounds.IsEmpty()) {
			return;
		}

		while (outrec.owner != null && (outrec.owner.pts == null || !CheckBounds(outrec.owner))) {
			outrec.owner = outrec.owner.owner;
		}

		if (outrec.owner != null && outrec.owner.polypath == null) {
			RecursiveCheckOwners(outrec.owner, polypath);
		}

		while (outrec.owner != null) {
			if (outrec.owner.bounds.Contains(outrec.bounds) && Path1InsidePath2(outrec.pts, outrec.owner.pts)) {
				break; // found - owner contain outrec!
			} else {
				outrec.owner = outrec.owner.owner;
			}
		}

		if (outrec.owner != null) {
			outrec.polypath = outrec.owner.polypath.AddChild(outrec.path);
		} else {
			outrec.polypath = polypath.AddChild(outrec.path);
		}
	}

	private void DeepCheckOwners(OutRec outrec, PolyPathBase polypath) {
		RecursiveCheckOwners(outrec, polypath);

		while (outrec.owner != null && outrec.owner.splits != null) {
			@Nullable
			OutRec split = null;
			for (int i : outrec.owner.splits) {
				split = GetRealOutRec(outrecList.get(i));
				if (split != null && split != outrec && split != outrec.owner && CheckBounds(split) && split.bounds.Contains(outrec.bounds)
						&& Path1InsidePath2(outrec.pts, split.pts)) {
					RecursiveCheckOwners(split, polypath);
					outrec.owner = split; // found in split
					break; // inner 'for' loop
				} else {
					split = null;
				}
			}
			if (split == null) {
				break;
			}
		}
	}

	protected void BuildTree(PolyPathBase polytree, Paths64 solutionOpen) {
		polytree.Clear();
		solutionOpen.clear();

		int i = 0;
		// outrecList.Count is not static here because
		// CheckBounds below can indirectly add additional
		// OutRec (via FixOutRecPts & CleanCollinear)
		while (i < outrecList.size()) {
			OutRec outrec = outrecList.get(i++);
			if (outrec.pts == null) {
				continue;
			}

			if (outrec.isOpen) {
				Path64 openPath = new Path64();
				if (BuildPath(outrec.pts, getReverseSolution(), true, openPath)) {
					solutionOpen.add(openPath);
				}
				continue;
			}
			if (CheckBounds(outrec)) {
				DeepCheckOwners(outrec, polytree);
			}
		}
	}

	public final Rect64 GetBounds() {
		Rect64 bounds = Clipper.InvalidRect64.clone();
		for (Vertex t : vertexList) {
			Vertex v = t;
			do {
				if (v.pt.x < bounds.left) {
					bounds.left = v.pt.x;
				}
				if (v.pt.x > bounds.right) {
					bounds.right = v.pt.x;
				}
				if (v.pt.y < bounds.top) {
					bounds.top = v.pt.y;
				}
				if (v.pt.y > bounds.bottom) {
					bounds.bottom = v.pt.y;
				}
				v = v.next;
			} while (v != t);
		}
		return bounds.IsEmpty() ? new Rect64(0, 0, 0, 0) : bounds;
	}

}
