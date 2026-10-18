package com.clipper.engine;

import java.util.ArrayList;
import java.util.List;

import com.clipper.core.Path64;
import com.clipper.Nullable;

public abstract class PolyPathBase implements Iterable<PolyPathBase> {

	@Nullable
	PolyPathBase parent;
	List<PolyPathBase> children = new ArrayList<>();

	PolyPathBase(@Nullable PolyPathBase parent) {
		this.parent = parent;
	}

	PolyPathBase() {
		this(null);
	}

	@Override
	public final NodeIterator iterator() {
		return new NodeIterator(children);
	}

	private int GetLevel() {
		int result = 0;
		@Nullable
		PolyPathBase pp = parent;
		while (pp != null) {
			++result;
			pp = pp.parent;
		}
		return result;
	}

	/**
	 * Indicates whether the Polygon property represents a hole or the outer bounds
	 * of a polygon.
	 */
	public final boolean getIsHole() {
		int lvl = GetLevel();
		return lvl != 0 && (lvl & 1) == 0;
	}

	/**
	 * Indicates the number of contained children.
	 */
	public final int getCount() {
		return children.size();
	}

	public abstract PolyPathBase AddChild(Path64 p);

	/**
	 * This method clears the Polygon and deletes any contained children.
	 */
	public final void Clear() {
		children.clear();
	}
}