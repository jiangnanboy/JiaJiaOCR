package com.clipper.engine;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class NodeIterator implements Iterator<PolyPathBase> {

	List<PolyPathBase> ppbList;
	int position = 0;

	NodeIterator(List<PolyPathBase> childs) {
		ppbList = childs;
	}

	@Override
	public final boolean hasNext() {
		return (position < ppbList.size());
	}

	@Override
	public PolyPathBase next() {
		if (position < 0 || position >= ppbList.size()) {
			throw new NoSuchElementException();
		}
		return ppbList.get(position++);
	}

}