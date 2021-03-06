/* 
 * JPUtil
 * Copyright (C) 2019  Javapony/OLEGSHA
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package ru.windcorp.jputil.iterators;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class RangeIterator<E> implements Iterator<E> {
	
	private final Iterator<E> parent;
	private final int from;
	private final int amount;
	
	private int nextIndex = 0;

	public RangeIterator(Iterator<E> iterator, int from, int amount) {
		this.parent = iterator;
		this.from = from;
		this.amount = amount < 0 ? Integer.MAX_VALUE : amount;
	}
	
	public RangeIterator(Iterator<E> iterator, int from) {
		this(iterator, from, -1);
	}

	@Override
	public boolean hasNext() {
		update();
		return nextIndex < from + amount && parent.hasNext();
	}

	@Override
	public E next() {
		update();
		if (nextIndex >= from + amount) {
			throw new NoSuchElementException("RangeIterator about to retrieve element " + nextIndex 
					+ " which exceeds upper boundary " + (from + amount));
		}
		
		E result = parent.next();
		nextIndex++;
		return result;
	}
	
	protected void update() {
		while (nextIndex < from && parent.hasNext()) {
			parent.next();
			nextIndex++;
		}
	}

}
