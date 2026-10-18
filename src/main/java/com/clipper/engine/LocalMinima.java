package com.clipper.engine;

import com.clipper.core.PathType;

final class LocalMinima {

	ClipperBase.Vertex vertex;
	PathType polytype = PathType.Subject;
	boolean isOpen = false;

	LocalMinima() {
	}

	LocalMinima(ClipperBase.Vertex vertex, PathType polytype) {
		this(vertex, polytype, false);
	}

	LocalMinima(ClipperBase.Vertex vertex, PathType polytype, boolean isOpen) {
		this.vertex = vertex;
		this.polytype = polytype;
		this.isOpen = isOpen;
	}

	boolean opEquals(LocalMinima o) {
		return vertex == o.vertex;
	}

	boolean opNotEquals(LocalMinima o) {
		return vertex != o.vertex;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LocalMinima) {
			LocalMinima minima = (LocalMinima) obj;
			return this == minima;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return vertex.hashCode();
	}

	@Override
	protected LocalMinima clone() {
		LocalMinima varCopy = new LocalMinima();

		varCopy.vertex = this.vertex;
		varCopy.polytype = this.polytype;
		varCopy.isOpen = this.isOpen;

		return varCopy;
	}
}