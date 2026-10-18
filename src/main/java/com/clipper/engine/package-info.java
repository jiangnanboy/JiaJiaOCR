/**
 * The Clipper64 and ClipperD classes in this unit encapsulate all the logic
 * that performs path clipping. Clipper64 clips Paths64 paths, and ClipperD
 * clips PathsD paths.
 * <p>
 * For complex clipping operations (on open paths, and when using PolyTrees,
 * etc.), you'll need to implement these classes directly. But for simpler
 * clipping operations, the clipping functions in the Clipper Unit will be
 * easier to use.
 * <p>
 * The PolyTree64 and PolyTreeD classes are optional data structures that, like
 * Paths64 and PathsD, receive polygon solutions from clipping operations. This
 * Polytree structure reflects polygon ownership (which polygons contain other
 * polygons). But using Polytrees will slow clipping, usually by 10-50%.
 */
package com.clipper.engine;