package com.clipper.engine;

/**
 * PolyTree64 is a read-only data structure that receives solutions from
 * clipping operations. It's an alternative to the Paths64 data structure which
 * also receives solutions. However the principle advantage of PolyTree64 over
 * Paths64 is that it also represents the parent-child relationships of the
 * polygons in the solution (where a parent's Polygon will contain all its
 * children Polygons).
 * <p>
 * The PolyTree64 object that's to receive a clipping solution is passed as a
 * parameter to Clipper64.Execute. When the clipping operation finishes, this
 * object will be populated with data representing the clipped solution.
 * <p>
 * A PolyTree64 object is a container for any number of PolyPath64 child
 * objects, each representing a single polygon contour. Direct descendants of
 * PolyTree64 will always be outer polygon contours. PolyPath64 children may in
 * turn contain their own children to any level of nesting. Children of outer
 * polygon contours will always represent holes, and children of holes will
 * always represent nested outer polygon contours.
 * <p>
 * PolyTree64 is a specialised PolyPath64 object that's simply as a container
 * for other PolyPath64 objects and its own polygon property will always be
 * empty.
 * <p>
 * PolyTree64 will never contain open paths (unlike in Clipper1) since open
 * paths can't contain paths. When clipping open paths, these will always be
 * represented in solutions via a separate Paths64 structure.
 */
public class PolyTree64 extends PolyPath64 {
}