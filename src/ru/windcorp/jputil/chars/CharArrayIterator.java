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
package ru.windcorp.jputil.chars;

import java.text.CharacterIterator;

public class CharArrayIterator implements CharacterIterator, Cloneable {

	private final char[] array;
	private int pos;

	public CharArrayIterator(char[] array) {
		this.array = array;
	}

	public CharArrayIterator(String src) {
		this(src.toCharArray());
	}

	@Override
	public char first() {
		pos = 0;
		if (array.length != 0) {
			return array[pos];
		}
		return DONE;
	}

	@Override
	public char last() {
		pos = array.length;
		if (array.length != 0) {
			pos -= 1;
			return array[pos];
		}
		return DONE;
	}

	@Override
	public char current() {
		if (array.length != 0 && pos < array.length) {
			return array[pos];
		}
		return DONE;
	}

	@Override
	public char next() {
		pos += 1;
		if (pos >= array.length) {
			pos = array.length;
			return DONE;
		}
		return current();
	}

	@Override
	public char previous() {
		if (pos == 0) {
			return DONE;
		}
		pos -= 1;
		return current();
	}

	@Override
	public char setIndex(int position) {
		if (position < 0 || position > array.length) {
			throw new IllegalArgumentException("bad position: " + position);
		}

		pos = position;

		if (pos != array.length && array.length != 0) {
			return array[pos];
		}
		return DONE;
	}

	@Override
	public int getBeginIndex() {
		return 0;
	}

	@Override
	public int getEndIndex() {
		return array.length;
	}

	@Override
	public int getIndex() {
		return pos;
	}
	
	@Override
	public CharArrayIterator clone() {
		try {
			return (CharArrayIterator) super.clone();
		} catch (CloneNotSupportedException cnse) {
			return null;
		}
	}

}
