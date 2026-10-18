package com.clipper.engine;

/**
 * PolyTreeD is a read-only data structure that receives solutions from clipping
 * operations. It's an alternative to the PathsD data structure which also
 * receives solutions. However the principle advantage of PolyTreeD over PathsD
 * is that it also represents the parent-child relationships of the polygons in
 * the solution (where a parent's Polygon will contain all its children
 * Polygons).
 * <p>
 * The PolyTreeD object that's to receive a clipping solution is passed as a
 * parameter to ClipperD.Execute. When the clipping operation finishes, this
 * object will be populated with data representing the clipped solution.
 * <p>
 * A PolyTreeD object is a container for any number of PolyPathD child objects,
 * each representing a single polygon contour. PolyTreeD's top level children
 * will always be outer polygon contours. PolyPathD children may in turn contain
 * their own children to any level of nesting. Children of outer polygon
 * contours will always represent holes, and children of holes will always
 * represent nested outer polygon contours.
 * <p>
 * PolyTreeD is a specialised PolyPathD object that's simply as a container for
 * other PolyPathD objects and its own polygon property will always be empty.
 * <p>
 * PolyTreeD will never contain open paths (unlike in Clipper1) since open paths
 * can't contain (own) other paths. When clipping open paths, these will always
 * be represented in solutions via a separate PathsD structure.
 */
public class PolyTreeD extends PolyPathD {
}