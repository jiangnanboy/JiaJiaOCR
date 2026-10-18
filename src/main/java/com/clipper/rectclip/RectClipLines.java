package com.clipper.rectclip;

import com.clipper.Clipper;
import com.clipper.Nullable;
import com.clipper.core.Path64;
import com.clipper.core.Paths64;
import com.clipper.core.Point64;
import com.clipper.core.Rect64;
import com.clipper.tangible.RefObject;

/**
 * RectClipLines intersects subject open paths (polylines) with the specified
 * rectangular clipping region.
 * <p>
 * This function is extremely fast when compared to the Library's general
 * purpose Intersect clipper. Where Intersect has roughly O(n³) performance,
 * RectClipLines has O(n) performance.
 * 
 * @since 1.0.6
 */
public class RectClipLines extends RectClip {

	public RectClipLines(Rect64 rect) {
		super(rect);
	}

	public Paths64 Execute(Paths64 paths) {
		Paths64 result = new Paths64();
		if (rect.IsEmpty()) {
			return result;
		}
		for (Path64 path : paths) {
			if (path.size() < 2) {
				continue;
			}
			pathBounds = Clipper.GetBounds(path);
			if (!rect.Intersects(pathBounds)) {
				continue; // the path must be completely outside fRect
			}
			// Apart from that, we can't be sure whether the path
			// is completely outside or completed inside or intersects
			// fRect, simply by comparing path bounds with fRect.
			ExecuteInternal(path);

			for (@Nullable
			OutPt2 op : results) {
				Path64 tmp = GetPath(op);
				if (!tmp.isEmpty()) {
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

	private Path64 GetPath(@Nullable OutPt2 op) {
		Path64 result = new Path64();
		if (op == null || op == op.next) {
			return result;
		}
		op = op.next; // starting at path beginning
		result.add(op.pt);
		OutPt2 op2 = op.next;
		while (op2 != op) {
			result.add(op2.pt);
			op2 = op2.next;
		}
		return result;
	}

	private void ExecuteInternal(Path64 path) {
		results.clear();
		if (path.size() < 2 || rect.IsEmpty()) {
			return;
		}

		RefObject<Location> prev = new RefObject<>(Location.INSIDE);
		RefObject<Integer> i = new RefObject<>(1);
		int highI = path.size() - 1;
		RefObject<Location> loc = new RefObject<>(null);
		if (!GetLocation(rect, path.get(0), loc)) {
			while (i.argValue <= highI && !GetLocation(rect, path.get(i.argValue), prev)) {
				i.argValue++;
			}
			if (i.argValue > highI) {
				for (Point64 pt : path) {
					Add(pt);
				}
			}
			if (prev.argValue == Location.INSIDE) {
				loc.argValue = Location.INSIDE;
			}
			i.argValue = 1;
		}
		if (loc.argValue == Location.INSIDE) {
			Add(path.get(0));
		}

		///////////////////////////////////////////////////
		while (i.argValue <= highI) {
			prev.argValue = loc.argValue;
			GetNextLocation(path, loc, i, highI);
			if (i.argValue > highI) {
				break;
			}
			Point64 prevPt = path.get(i.argValue - 1);

			RefObject<Location> crossingLoc = new RefObject<>(loc.argValue);
			Point64 ip = new Point64();
			if (!GetIntersection(rectPath, path.get(i.argValue), prevPt, crossingLoc, ip)) {
				// ie remaining outside (& crossingLoc still == loc)
				++i.argValue;
				continue;
			}

			////////////////////////////////////////////////////
			// we must be crossing the rect boundary to get here
			////////////////////////////////////////////////////

			if (loc.argValue == Location.INSIDE) // path must be entering rect
			{
				Add(ip);
			} else if (prev.argValue != Location.INSIDE) {
				// passing right through rect. 'ip' here will be the second
				// intersect pt but we'll also need the first intersect pt (ip2)
				crossingLoc.argValue = prev.argValue;
				Point64 ip2 = new Point64();
				GetIntersection(rectPath, prevPt, path.get(i.argValue), crossingLoc, ip2);
				Add(ip2);
				Add(ip);
			} else // path must be exiting rect
			{
				Add(ip);
			}
		}
	}

}
