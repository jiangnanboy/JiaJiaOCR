package com.clipper.rectclip;

import java.util.ArrayList;
import java.util.List;

import com.clipper.Nullable;
import com.clipper.Clipper;
import com.clipper.core.InternalClipper;
import com.clipper.core.Path64;
import com.clipper.core.Paths64;
import com.clipper.core.Point64;
import com.clipper.core.Rect64;
import com.clipper.engine.PointInPolygonResult;
import com.clipper.tangible.OutObject;
import com.clipper.tangible.RefObject;

/**
 * RectClip intersects subject polygons with the specified rectangular clipping
 * region. Polygons may be simple or complex (self-intersecting).
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClip has O(n) performance.
 *
 * @since 1.0.6
 */
public class RectClip {

	protected static class OutPt2 {
		@Nullable
		OutPt2 next;
		@Nullable
		OutPt2 prev;

		Point64 pt;
		int ownerIdx;
		@Nullable
		public List<@Nullable OutPt2> edge;

		public OutPt2(Point64 pt) {
			this.pt = pt;
		}
	}

	protected enum Location {
		LEFT, TOP, RIGHT, BOTTOM, INSIDE
	}

	protected final Rect64 rect;
	protected final Point64 mp;
	protected final Path64 rectPath;
	protected Rect64 pathBounds;
	protected List<@Nullable OutPt2> results;
	protected List<@Nullable OutPt2>[] edges;
	protected int currIdx = -1;

	@SuppressWarnings("unchecked")
	public RectClip(Rect64 rect) {
		currIdx = -1;
		this.rect = rect;
		mp = rect.MidPoint();
		rectPath = this.rect.AsPath();
		results = new ArrayList<>();
		edges = new List[8];
		for (int i = 0; i < 8; i++) {
			edges[i] = new ArrayList<>();
		}
	}

	protected OutPt2 Add(Point64 pt) {
		return Add(pt, false);
	}

	protected OutPt2 Add(Point64 pt, boolean startingNewPath) {
		// this method is only called by InternalExecute.
		// Later splitting and rejoining won't create additional op's,
		// though they will change the (non-storage) fResults count.
		int currIdx = results.size();
		OutPt2 result;
		if ((currIdx == 0) || startingNewPath) {
			result = new OutPt2(pt);
			results.add(result);
			result.ownerIdx = currIdx;
			result.prev = result;
			result.next = result;
		} else {
			currIdx--;
			@Nullable
			OutPt2 prevOp = results.get(currIdx);
			if (prevOp.pt == pt) {
				return prevOp;
			}
			result = new OutPt2(pt);
			result.ownerIdx = currIdx;
			result.next = prevOp.next;
			prevOp.next.prev = result;
			prevOp.next = result;
			result.prev = prevOp;
			results.set(currIdx, result);
		}
		return result;
	}

	@SuppressWarnings("incomplete-switch")
	private static boolean Path1ContainsPath2(Path64 path1, Path64 path2) {
		// nb: occasionally, due to rounding, path1 may
		// appear (momentarily) inside or outside path2.
		int ioCount = 0;
		for (Point64 pt : path2) {
			PointInPolygonResult pip = InternalClipper.PointInPolygon(pt, path1);
			switch (pip) {
				case IsInside :
					ioCount--;
					break;
				case IsOutside :
					ioCount++;
					break;
			}
			if (Math.abs(ioCount) > 1) {
				break;
			}
		}
		return ioCount <= 0;
	}

	private static boolean IsClockwise(Location prev, Location curr, Point64 prevPt, Point64 currPt, Point64 rectMidPoint) {
		if (AreOpposites(prev, curr)) {
			return InternalClipper.CrossProduct(prevPt, rectMidPoint, currPt) < 0;
		} else {
			return HeadingClockwise(prev, curr);
		}
	}

	private static boolean AreOpposites(Location prev, Location curr) {
		return Math.abs(prev.ordinal() - curr.ordinal()) == 2;
	}

	private static boolean HeadingClockwise(Location prev, Location curr) {
		return (prev.ordinal() + 1) % 4 == curr.ordinal();
	}

	private static Location GetAdjacentLocation(Location loc, boolean isClockwise) {
		int delta = (isClockwise) ? 1 : 3;
		return Location.values()[(loc.ordinal() + delta) % 4];
	}

	private static @Nullable OutPt2 UnlinkOp(OutPt2 op) {
		if (op.next == op) {
			return null;
		}
		op.prev.next = op.next;
		op.next.prev = op.prev;
		return op.next;
	}

	private static @Nullable OutPt2 UnlinkOpBack(OutPt2 op) {
		if (op.next == op) {
			return null;
		}
		op.prev.next = op.next;
		op.next.prev = op.prev;
		return op.prev;
	}

	private static /* unsigned */ int GetEdgesForPt(Point64 pt, Rect64 rec) {
		int result = 0; // unsigned
		if (pt.x == rec.left) {
			result = 1;
		} else if (pt.x == rec.right) {
			result = 4;
		}
		if (pt.y == rec.top) {
			result += 2;
		} else if (pt.y == rec.bottom) {
			result += 8;
		}
		return result;
	}

	private static boolean IsHeadingClockwise(Point64 pt1, Point64 pt2, int edgeIdx) {
		switch (edgeIdx) {
			case 0 :
				return pt2.y < pt1.y;
			case 1 :
				return pt2.x > pt1.x;
			case 2 :
				return pt2.y > pt1.y;
			default :
				return pt2.x < pt1.x;
		}
	}

	private static boolean HasHorzOverlap(Point64 left1, Point64 right1, Point64 left2, Point64 right2) {
		return (left1.x < right2.x) && (right1.x > left2.x);
	}

	private static boolean HasVertOverlap(Point64 top1, Point64 bottom1, Point64 top2, Point64 bottom2) {
		return (top1.y < bottom2.y) && (bottom1.y > top2.y);
	}

	private static void AddToEdge(List<@Nullable OutPt2> edge, OutPt2 op) {
		if (op.edge != null) {
			return;
		}
		op.edge = edge;
		edge.add(op);
	}

	private static void UncoupleEdge(OutPt2 op) {
		if (op.edge == null) {
			return;
		}
		for (int i = 0; i < op.edge.size(); i++) {
			@Nullable
			OutPt2 op2 = op.edge.get(i);
			if (op2 == op) {
				op.edge.set(i, null);
				break;
			}
		}
		op.edge = null;
	}

	private static void SetNewOwner(OutPt2 op, int newIdx) {
		op.ownerIdx = newIdx;
		OutPt2 op2 = op.next;
		while (op2 != op) {
			op2.ownerIdx = newIdx;
			op2 = op2.next;
		}
	}

	private void AddCorner(Location prev, Location curr) {
		if (HeadingClockwise(prev, curr)) {
			Add(rectPath.get(prev.ordinal()));
		} else {
			Add(rectPath.get(curr.ordinal()));
		}
	}

	private void AddCorner(RefObject<Location> loc, boolean isClockwise) {
		if (isClockwise) {
			Add(rectPath.get(loc.argValue.ordinal()));
			loc.argValue = GetAdjacentLocation(loc.argValue, true);
		} else {
			loc.argValue = GetAdjacentLocation(loc.argValue, false);
			Add(rectPath.get(loc.argValue.ordinal()));
		}
	}

	protected static boolean GetLocation(Rect64 rec, Point64 pt, OutObject<Location> loc) {
		if (pt.x == rec.left && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.LEFT;
			return false; // pt on rec
		}
		if (pt.x == rec.right && pt.y >= rec.top && pt.y <= rec.bottom) {
			loc.argValue = Location.RIGHT;
			return false; // pt on rec
		}
		if (pt.y == rec.top && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.TOP;
			return false; // pt on rec
		}
		if (pt.y == rec.bottom && pt.x >= rec.left && pt.x <= rec.right) {
			loc.argValue = Location.BOTTOM;
			return false; // pt on rec
		}
		if (pt.x < rec.left) {
			loc.argValue = Location.LEFT;
		} else if (pt.x > rec.right) {
			loc.argValue = Location.RIGHT;
		} else if (pt.y < rec.top) {
			loc.argValue = Location.TOP;
		} else if (pt.y > rec.bottom) {
			loc.argValue = Location.BOTTOM;
		} else {
			loc.argValue = Location.INSIDE;
		}
		return true;
	}

	protected static boolean GetIntersection(Path64 rectPath, Point64 p, Point64 p2, RefObject<Location> loc, /* out */ Point64 ip) {
		/*
		 * Gets the pt of intersection between rectPath and segment(p, p2) that's
		 * closest to 'p'. When result == false, loc will remain unchanged.
		 */
		switch (loc.argValue) {
			case LEFT :
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
				} else if (p.y < rectPath.get(0).y && InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;

			case RIGHT :
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
				} else if (p.y < rectPath.get(0).y && InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;

			case TOP :
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
				} else if (p.x < rectPath.get(0).x && InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (p.x > rectPath.get(1).x && InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else {
					return false;
				}
				break;

			case BOTTOM :
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
				} else if (p.x < rectPath.get(3).x && InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (p.x > rectPath.get(2).x && InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else {
					return false;
				}
				break;

			case INSIDE :
				if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(3), ip);
					loc.argValue = Location.LEFT;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(0), rectPath.get(1), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(0), rectPath.get(1), ip);
					loc.argValue = Location.TOP;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(1), rectPath.get(2), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(1), rectPath.get(2), ip);
					loc.argValue = Location.RIGHT;
				} else if (InternalClipper.SegsIntersect(p, p2, rectPath.get(2), rectPath.get(3), true)) {
					InternalClipper.GetIntersectPt(p, p2, rectPath.get(2), rectPath.get(3), ip);
					loc.argValue = Location.BOTTOM;
				} else {
					return false;
				}
				break;
		}
		return true;
	}

	protected void GetNextLocation(Path64 path, RefObject<Location> loc, RefObject<Integer> i, int highI) {
		switch (loc.argValue) {
			case LEFT : {
				while (i.argValue <= highI && path.get(i.argValue).x <= rect.left) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case TOP : {
				while (i.argValue <= highI && path.get(i.argValue).y <= rect.top) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case RIGHT : {
				while (i.argValue <= highI && path.get(i.argValue).x >= rect.right) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).y >= rect.bottom) {
					loc.argValue = Location.BOTTOM;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case BOTTOM : {
				while (i.argValue <= highI && path.get(i.argValue).y >= rect.bottom) {
					i.argValue++;
				}
				if (i.argValue > highI) {
					break;
				}
				if (path.get(i.argValue).y <= rect.top) {
					loc.argValue = Location.TOP;
				} else if (path.get(i.argValue).x <= rect.left) {
					loc.argValue = Location.LEFT;
				} else if (path.get(i.argValue).x >= rect.right) {
					loc.argValue = Location.RIGHT;
				} else {
					loc.argValue = Location.INSIDE;
				}
			}
				break;

			case INSIDE : {
				while (i.argValue <= highI) {
					if (path.get(i.argValue).x < rect.left) {
						loc.argValue = Location.LEFT;
					} else if (path.get(i.argValue).x > rect.right) {
						loc.argValue = Location.RIGHT;
					} else if (path.get(i.argValue).y > rect.bottom) {
						loc.argValue = Location.BOTTOM;
					} else if (path.get(i.argValue).y < rect.top) {
						loc.argValue = Location.TOP;
					} else {
						Add(path.get(i.argValue));
						i.argValue++;
						continue;
					}
					break;
				}
			}
				break;
		} // switch
	}

	private void ExecuteInternal(Path64 path) {
		if (path.size() < 3 || this.rect.IsEmpty()) {
			return;
		}
		List<Location> startLocs = new ArrayList<>();

		Location firstCross = Location.INSIDE;
		RefObject<Location> crossingLoc = new RefObject<>(firstCross);
		RefObject<Location> prev = new RefObject<>(firstCross);

		RefObject<Integer> i = new RefObject<>(0);
		int highI = path.size() - 1;
		RefObject<Location> loc = new RefObject<>(null);
		if (!GetLocation(this.rect, path.get(highI), loc)) {
			i.argValue = highI - 1;
			while (i.argValue >= 0 && !GetLocation(this.rect, path.get(i.argValue), prev)) {
				i.argValue--;
			}
			if (i.argValue < 0) {
				for (Point64 pt : path) {
					Add(pt);
				}
				return;
			}
			if (prev.argValue == Location.INSIDE) {
				loc.argValue = Location.INSIDE;
			}
		}
		Location startingLoc = loc.argValue;

		///////////////////////////////////////////////////
		i.argValue = 0;
		while (i.argValue <= highI) {
			prev.argValue = loc.argValue;
			Location prevCrossLoc = crossingLoc.argValue;
			GetNextLocation(path, loc, i, highI);
			if (i.argValue > highI) {
				break;
			}

			Point64 prevPt = (i.argValue == 0) ? path.get(highI) : path.get(i.argValue - 1);
			crossingLoc.argValue = loc.argValue;
			Point64 ip = new Point64();
			if (!GetIntersection(rectPath, path.get(i.argValue), prevPt, crossingLoc, ip)) {
				// ie remaining outside
				if (prevCrossLoc == Location.INSIDE) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						startLocs.add(prev.argValue);
						prev.argValue = GetAdjacentLocation(prev.argValue, isClockw);
					} while (prev.argValue != loc.argValue);
					crossingLoc.argValue = prevCrossLoc; // still not crossed
				} else if (prev.argValue != Location.INSIDE && prev.argValue != loc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, loc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						AddCorner(prev, isClockw);
					} while (prev.argValue != loc.argValue);
				}
				++i.argValue;
				continue;
			}

			////////////////////////////////////////////////////
			// we must be crossing the rect boundary to get here
			////////////////////////////////////////////////////

			if (loc.argValue == Location.INSIDE) // path must be entering rect
			{
				if (firstCross == Location.INSIDE) {
					firstCross = crossingLoc.argValue;
					startLocs.add(prev.argValue);
				} else if (prev.argValue != crossingLoc.argValue) {
					boolean isClockw = IsClockwise(prev.argValue, crossingLoc.argValue, prevPt, path.get(i.argValue), mp);
					do {
						AddCorner(prev, isClockw);
					} while (prev.argValue != crossingLoc.argValue);
				}
			} else if (prev.argValue != Location.INSIDE) {
				// passing right through rect. 'ip' here will be the second
				// intersect pt but we'll also need the first intersect pt (ip2)
				loc.argValue = prev.argValue;
				Point64 ip2 = new Point64();
				GetIntersection(rectPath, prevPt, path.get(i.argValue), loc, ip2);
				if (prevCrossLoc != Location.INSIDE) {
					AddCorner(prevCrossLoc, loc.argValue);
				}

				if (firstCross == Location.INSIDE) {
					firstCross = loc.argValue;
					startLocs.add(prev.argValue);
				}

				loc.argValue = crossingLoc.argValue;
				Add(ip2);
				if (ip.opEquals(ip2)) {
					// it's very likely that path[i] is on rect
					GetLocation(rect, path.get(i.argValue), loc);
					AddCorner(crossingLoc.argValue, loc.argValue);
					crossingLoc.argValue = loc.argValue;
					continue;
				}
			} else // path must be exiting rect
			{
				loc.argValue = crossingLoc.argValue;
				if (firstCross == Location.INSIDE) {
					firstCross = crossingLoc.argValue;
				}
			}

			Add(ip);
		} // while i <= highI
			///////////////////////////////////////////////////

		if (firstCross == Location.INSIDE) {
			// path never intersects
			if (startingLoc != Location.INSIDE) {
				if (pathBounds.Contains(this.rect) && Path1ContainsPath2(path, this.rectPath)) {
					for (int j = 0; j < 4; j++) {
						Add(this.rectPath.get(j));
						AddToEdge(edges[j * 2], results.get(0));
					}
				}
			}
		} else if (loc.argValue != Location.INSIDE && (loc.argValue != firstCross || startLocs.size() > 2)) {
			if (startLocs.size() > 0) {
				prev.argValue = loc.argValue;
				for (Location loc2 : startLocs) {
					if (prev.argValue == loc2) {
						continue;
					}
					AddCorner(prev, HeadingClockwise(prev.argValue, loc2));
					prev.argValue = loc2;
				}
				loc.argValue = prev.argValue;
			}
			if (loc.argValue != firstCross) {
				AddCorner(loc, HeadingClockwise(loc.argValue, firstCross));
			}
		}
	}

	public Paths64 Execute(Paths64 paths, boolean convexOnly) {
		Paths64 result = new Paths64();
		if (rect.IsEmpty()) {
			return result;
		}
		for (Path64 path : paths) {
			if (path.size() < 3) {
				continue;
			}
			pathBounds = Clipper.GetBounds(path);
			if (!rect.Intersects(pathBounds)) {
				continue; // the path must be completely outside fRect
			} else if (rect.Contains(pathBounds)) {
				// the path must be completely inside rect_
				result.add(path);
				continue;
			}
			ExecuteInternal(path);
			if (!convexOnly) {
				CheckEdges();
				for (int i = 0; i < 4; ++i) {
					TidyEdgePair(i, edges[i * 2], edges[i * 2 + 1]);
				}
			}

			for (@Nullable
			OutPt2 op : results) {
				Path64 tmp = GetPath(op);
				if (tmp.size() > 0) {
					result.add(tmp);
				}
			}

			// clean up after every loop
			results.clear();
			for (int i = 0; i < 8; i++) {
				edges[i].clear();
			}
		}
		return result;
	}

	private void CheckEdges() {
		for (int i = 0; i < results.size(); i++) {
			@Nullable
			OutPt2 op = results.get(i), op2 = op;
			if (op == null) {
				continue;
			}
			do {
				if (InternalClipper.CrossProduct(op2.prev.pt, op2.pt, op2.next.pt) == 0) {
					if (op2 == op) {
						op2 = UnlinkOpBack(op2);
						if (op2 == null) {
							break;
						}
						op = op2.prev;
					} else {
						op2 = UnlinkOpBack(op2);
						if (op2 == null) {
							break;
						}
					}
				} else {
					op2 = op2.next;
				}
			} while (op2 != op);

			if (op2 == null) {
				results.set(i, null);
				continue;
			}
			results.set(i, op2); // safety first

			/* unsigned */ int edgeSet1 = GetEdgesForPt(op.prev.pt, rect);
			op2 = op;
			do {
				/* unsigned */ int edgeSet2 = GetEdgesForPt(op2.pt, rect);
				if (edgeSet2 != 0 && op2.edge == null) {
					/* unsigned */ int combinedSet = (edgeSet1 & edgeSet2);
					for (int j = 0; j < 4; ++j) {
						if ((combinedSet & (1 << j)) != 0) {
							if (IsHeadingClockwise(op2.prev.pt, op2.pt, j)) {
								AddToEdge(edges[j * 2], op2);
							} else {
								AddToEdge(edges[j * 2 + 1], op2);
							}
						}
					}
				}
				edgeSet1 = edgeSet2;
				op2 = op2.next;
			} while (op2 != op);
		}
	}

	private void TidyEdgePair(int idx, List<@Nullable OutPt2> cw, List<@Nullable OutPt2> ccw) {
		if (ccw.isEmpty()) {
			return;
		}
		boolean isHorz = ((idx == 1) || (idx == 3));
		boolean cwIsTowardLarger = ((idx == 1) || (idx == 2));
		int i = 0, j = 0;
		@Nullable
		OutPt2 p1, p2, p1a, p2a, op, op2;

		while (i < cw.size()) {
			p1 = cw.get(i);
			if (p1 == null || p1.next == p1.prev) {
				cw.get(i++).edge = null;
				j = 0;
				continue;
			}

			int jLim = ccw.size();
			while (j < jLim && (ccw.get(j) == null || ccw.get(j).next == ccw.get(j).prev)) {
				++j;
			}

			if (j == jLim) {
				++i;
				j = 0;
				continue;
			}

			if (cwIsTowardLarger) {
				// p1 >>>> p1a;
				// p2 <<<< p2a;
				p1 = cw.get(i).prev;
				p1a = cw.get(i);
				p2 = ccw.get(j);
				p2a = ccw.get(j).prev;
			} else {
				// p1 <<<< p1a;
				// p2 >>>> p2a;
				p1 = cw.get(i);
				p1a = cw.get(i).prev;
				p2 = ccw.get(j).prev;
				p2a = ccw.get(j);
			}

			if ((isHorz && !HasHorzOverlap(p1.pt, p1a.pt, p2.pt, p2a.pt)) || (!isHorz && !HasVertOverlap(p1.pt, p1a.pt, p2.pt, p2a.pt))) {
				++j;
				continue;
			}

			// to get here we're either splitting or rejoining
			boolean isRejoining = cw.get(i).ownerIdx != ccw.get(j).ownerIdx;

			if (isRejoining) {
				results.set(p2.ownerIdx, null);
				SetNewOwner(p2, p1.ownerIdx);
			}

			// do the split or re-join
			if (cwIsTowardLarger) {
				// p1 >> | >> p1a;
				// p2 << | << p2a;
				p1.next = p2;
				p2.prev = p1;
				p1a.prev = p2a;
				p2a.next = p1a;
			} else {
				// p1 << | << p1a;
				// p2 >> | >> p2a;
				p1.prev = p2;
				p2.next = p1;
				p1a.next = p2a;
				p2a.prev = p1a;
			}

			if (!isRejoining) {
				int newIdx = results.size();
				results.add(p1a);
				SetNewOwner(p1a, newIdx);
			}

			if (cwIsTowardLarger) {
				op = p2;
				op2 = p1a;
			} else {
				op = p1;
				op2 = p2a;
			}
			results.set(op.ownerIdx, op);
			results.set(op2.ownerIdx, op2);

			// and now lots of work to get ready for the next loop

			boolean opIsLarger, op2IsLarger;
			if (isHorz) // X
			{
				opIsLarger = op.pt.x > op.prev.pt.x;
				op2IsLarger = op2.pt.x > op2.prev.pt.x;
			} else // Y
			{
				opIsLarger = op.pt.y > op.prev.pt.y;
				op2IsLarger = op2.pt.y > op2.prev.pt.y;
			}

			if ((op.next == op.prev) || (op.pt == op.prev.pt)) {
				if (op2IsLarger == cwIsTowardLarger) {
					cw.set(i, op2);
					ccw.set(j++, null);
				} else {
					ccw.set(j, op2);
					cw.set(i++, null);
				}
			} else if ((op2.next == op2.prev) || (op2.pt == op2.prev.pt)) {
				if (opIsLarger == cwIsTowardLarger) {
					cw.set(i, op);
					ccw.set(j++, null);
				} else {
					ccw.set(j, op);
					cw.set(i++, null);
				}
			} else if (opIsLarger == op2IsLarger) {
				if (opIsLarger == cwIsTowardLarger) {
					cw.set(i, op);
					UncoupleEdge(op2);
					AddToEdge(cw, op2);
					ccw.set(j++, null);
				} else {
					cw.set(i++, null);
					ccw.set(j, op2);
					UncoupleEdge(op);
					AddToEdge(ccw, op);
					j = 0;
				}
			} else {
				if (opIsLarger == cwIsTowardLarger) {
					cw.set(i, op);
				} else {
					ccw.set(j, op);
				}
				if (op2IsLarger == cwIsTowardLarger) {
					cw.set(i, op2);
				} else {
					ccw.set(j, op2);
				}
			}
		}
	}

	private Path64 GetPath(@Nullable OutPt2 op) {
		Path64 result = new Path64();
		if (op == null || op.prev == op.next) {
			return result;
		}
		@Nullable
		OutPt2 op2 = op.next;
		while (op2 != null && op2 != op) {
			if (InternalClipper.CrossProduct(op2.prev.pt, op2.pt, op2.next.pt) == 0) {
				op = op2.prev;
				op2 = UnlinkOp(op2);
			} else {
				op2 = op2.next;
			}
		}
		if (op2 == null) {
			return new Path64();
		}

		result.add(op.pt);
		op2 = op.next;
		while (op2 != op) {
			result.add(op2.pt);
			op2 = op2.next;
		}
		return result;
	}

}
