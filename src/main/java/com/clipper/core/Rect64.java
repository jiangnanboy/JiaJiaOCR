package com.clipper.core;

public final class Rect64 {

	public long left;
	public long top;
	public long right;
	public long bottom;
	private static final String InvalidRect = "Invalid Rect64 assignment";

	public Rect64() {
	}

	public Rect64(long l, long t, long r, long b) {
		if (r < l || b < t) {
			throw new IllegalArgumentException(InvalidRect);
		}
		left = l;
		top = t;
		right = r;
		bottom = b;
	}

	public Rect64(boolean isValid) {
		if (isValid) {
			left = 0;
			top = 0;
			right = 0;
			bottom = 0;
		} else {
			left = Long.MAX_VALUE;
			top = Long.MAX_VALUE;
			right = Long.MIN_VALUE;
			bottom = Long.MIN_VALUE;
		}
	}

	public Rect64(Rect64 rec) {
		left = rec.left;
		top = rec.top;
		right = rec.right;
		bottom = rec.bottom;
	}

	public long getWidth() {
		return right - left;
	}

	public void setWidth(long value) {
		right = left + value;
	}

	public long getHeight() {
		return bottom - top;
	}

	public void setHeight(long value) {
		bottom = top + value;
	}

	public Path64 AsPath() {
		Path64 result = new Path64(4);
		result.add(new Point64(left, top));
		result.add(new Point64(right, top));
		result.add(new Point64(right, bottom));
		result.add(new Point64(left, bottom));
		return result;
	}

	public boolean IsEmpty() {
		return bottom <= top || right <= left;
	}

	public Point64 MidPoint() {
		return new Point64((left + right) / 2, (top + bottom) / 2);
	}

	public boolean Contains(Point64 pt) {
		return pt.x > left && pt.x < right && pt.y > top && pt.y < bottom;
	}

	public boolean Intersects(Rect64 rec) {
		return (Math.max(left, rec.left) <= Math.min(right, rec.right)) && (Math.max(top, rec.top) <= Math.min(bottom, rec.bottom));
	}

	public boolean Contains(Rect64 rec) {
		return rec.left >= left && rec.right <= right && rec.top >= top && rec.bottom <= bottom;
	}

	@Override
	public Rect64 clone() {
		Rect64 varCopy = new Rect64();

		varCopy.left = this.left;
		varCopy.top = this.top;
		varCopy.right = this.right;
		varCopy.bottom = this.bottom;

		return varCopy;
	}
}