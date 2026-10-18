package com.clipper.core;

import java.util.Arrays;

public final class RectD {

	public double left;
	public double top;
	public double right;
	public double bottom;
	private static final String InvalidRect = "Invalid RectD assignment";

	public RectD() {
	}

	public RectD(double l, double t, double r, double b) {
		if (r < l || b < t) {
			throw new IllegalArgumentException(InvalidRect);
		}
		left = l;
		top = t;
		right = r;
		bottom = b;
	}

	public RectD(RectD rec) {
		left = rec.left;
		top = rec.top;
		right = rec.right;
		bottom = rec.bottom;
	}

	public RectD(boolean isValid) {
		if (isValid) {
			left = 0;
			top = 0;
			right = 0;
			bottom = 0;
		} else {
			left = Double.MAX_VALUE;
			top = Double.MAX_VALUE;
			right = -Double.MAX_VALUE;
			bottom = -Double.MAX_VALUE;
		}
	}

	public double getWidth() {
		return right - left;
	}

	public void setWidth(double value) {
		right = left + value;
	}

	public double getHeight() {
		return bottom - top;
	}

	public void setHeight(double value) {
		bottom = top + value;
	}

	public boolean IsEmpty() {
		return bottom <= top || right <= left;
	}

	public PointD MidPoint() {
		return new PointD((left + right) / 2, (top + bottom) / 2);
	}

	public boolean Contains(PointD pt) {
		return pt.x > left && pt.x < right && pt.y > top && pt.y < bottom;
	}

	public boolean Contains(RectD rec) {
		return rec.left >= left && rec.right <= right && rec.top >= top && rec.bottom <= bottom;
	}

	public boolean Intersects(RectD rec) {
		return (Math.max(left, rec.left) < Math.min(right, rec.right)) && (Math.max(top, rec.top) < Math.min(bottom, rec.bottom));
	}

	public PathD AsPath() {
		PathD result = new PathD(
				Arrays.asList(new PointD(left, top), new PointD(right, top), new PointD(right, bottom), new PointD(left, bottom)));
		return result;
	}

	@Override
	public RectD clone() {
		RectD varCopy = new RectD();

		varCopy.left = this.left;
		varCopy.top = this.top;
		varCopy.right = this.right;
		varCopy.bottom = this.bottom;

		return varCopy;
	}
}