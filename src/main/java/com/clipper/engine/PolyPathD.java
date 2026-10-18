package com.clipper.engine;

import com.clipper.Clipper;
import com.clipper.core.Path64;
import com.clipper.core.PathD;
import com.clipper.Nullable;

public class PolyPathD extends PolyPathBase {

	private PathD polygon;
	private double scale;

	PolyPathD() {
		this(null);
	}

	PolyPathD(@Nullable PolyPathBase parent) {
		super(parent);
	}

	@Override
	public PolyPathBase AddChild(Path64 p) {
		PolyPathD newChild = new PolyPathD(this);
		newChild.setScale(scale);
		newChild.setPolygon(Clipper.ScalePathD(p, scale));
		children.add(newChild);
		return newChild;
	}

	public final PolyPathD get(int index) {
		if (index < 0 || index >= children.size()) {
			throw new IllegalStateException();
		}
		return (PolyPathD) children.get(index);
	}

	public final double Area() {
		double result = getPolygon() == null ? 0 : Clipper.Area(getPolygon());
		for (PolyPathBase polyPathBase : children) {
			PolyPathD child = (PolyPathD) polyPathBase;
			result += child.Area();
		}
		return result;
	}

	public final PathD getPolygon() {
		return polygon;
	}

	private void setPolygon(PathD value) {
		polygon = value;
	}

	public double getScale() {
		return scale;
	}

	public final void setScale(double value) {
		scale = value;
	}
}