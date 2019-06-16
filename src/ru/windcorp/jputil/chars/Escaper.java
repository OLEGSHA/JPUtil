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

import ru.windcorp.jputil.ArrayUtil;

public class Escaper {
	
	public static class EscaperBuilder {
		private char escapeChar = '\\';
		private char unicodeEscapeChar = 'u';
		private char[] safes = null;
		private char[] unsafes = null;
		
		private boolean preferUnicode = false;
		private boolean strict = true;
		
		public EscaperBuilder withEscapeChar(char escapeChar) {
			this.escapeChar = escapeChar;
			return this;
		}
		
		public EscaperBuilder withUnicodeEscapeChar(char unicodeEscapeChar) {
			this.unicodeEscapeChar = unicodeEscapeChar;
			return this;
		}
		
		public EscaperBuilder withChars(char[] safes, char[] unsafes) {
			this.safes = safes;
			this.unsafes = unsafes;
			return this;
		}
		
		public EscaperBuilder withChars(String safes, String unsafes) {
			this.safes = safes.toCharArray();
			this.unsafes = unsafes.toCharArray();
			return this;
		}
		
		public EscaperBuilder withChars(char[] chars) {
			this.safes = this.unsafes = chars;
			return this;
		}
		
		public EscaperBuilder withChars(String chars) {
			this.safes = this.unsafes = chars.toCharArray();
			return this;
		}
		
		public EscaperBuilder withSafes(char[] safes) {
			this.safes = safes;
			return this;
		}
		
		public EscaperBuilder withSafes(String safes) {
			this.safes = safes.toCharArray();
			return this;
		}
		
		public EscaperBuilder withUnsafes(char[] unsafes) {
			this.unsafes = unsafes;
			return this;
		}
		
		public EscaperBuilder withUnsafes(String unsafes) {
			this.unsafes = unsafes.toCharArray();
			return this;
		}
		
		public EscaperBuilder preferUnicode(boolean preferUnicode) {
			this.preferUnicode = preferUnicode;
			return this;
		}
		
		public EscaperBuilder strict(boolean strict) {
			this.strict = strict;
			return this;
		}
		
		public Escaper build() {
			return new Escaper(escapeChar, unicodeEscapeChar, safes, unsafes, preferUnicode, strict);
		}
		
	}
	
	public static final Escaper JAVA = new Escaper('\\', 'u', "tbnrf'\"".toCharArray(), "\t\b\n\r\f\'\"".toCharArray(), true, true);
	
	private final char escapeChar;
	private final char unicodeEscapeChar;
	private final char[] safes;
	private final char[] unsafes;
	
	private final boolean preferUnicode;
	private final boolean strict;
	
	protected Escaper(
			char escapeChar, char unicodeEscapeChar,
			char[] safes, char[] unsafes,
			boolean preferUnicode, boolean strict) {
		this.escapeChar = escapeChar;
		this.unicodeEscapeChar = unicodeEscapeChar;
		this.safes = safes;
		this.unsafes = unsafes;
		this.preferUnicode = preferUnicode;
		this.strict = strict;
		
		int duplicate;
		if ((duplicate = ArrayUtil.hasDuplicates(safes)) != -1)
			throw new IllegalArgumentException("Duplicate safe character '" + safes[duplicate] + "'");
		
		if ((duplicate = ArrayUtil.hasDuplicates(unsafes)) != -1)
			throw new IllegalArgumentException("Duplicate unsafe character '" + unsafes[duplicate] + "'");
		
		for (char c : safes) {
			if (c == escapeChar) throw new IllegalArgumentException("Safe characters contain escape chatacter");
			if (c == unicodeEscapeChar) throw new IllegalArgumentException("Safe characters contain Unicode escape chatacter");
		}
		
		for (char c : unsafes) {
			if (c == escapeChar) throw new IllegalArgumentException("Unsafe characters contain escape chatacter (escape character is escaped automatically)");
			if (c == unicodeEscapeChar) throw new IllegalArgumentException("Unsafe characters contain Unicode escape chatacter");
		}
	}
	
	public static EscaperBuilder create() {
		return new EscaperBuilder();
	}

	public char[] escape(CharacterIterator src, int length) {
		int end;
		if (length < 0) {
			end = src.getEndIndex();
		} else {
			end = src.getIndex() + length;
			if (end > src.getEndIndex()) 
				throw new IllegalArgumentException("length = " + length + ", " + (src.getEndIndex() - src.getIndex()) + " characters available");
		}

		int start = src.getIndex();
		int resultLength = 0;
		
		while (src.getIndex() < end) {
			resultLength += getEscapeLength(src.current());
			src.next();
		}
		
		char[] result = new char[resultLength];
		int offset = 0;
		src.setIndex(start);
		
		while (src.getIndex() < end) {
			offset += insertEscapeSequence(src.current(), result, offset);
			src.next();
		}
		
		return result;
	}
	
	public char[] escape(CharacterIterator src) {
		return escape(src, -1);
	}
	
	public char[] escape(char[] src) {
		return escape(new CharArrayIterator(src), src.length);
	}
	
	public char[] escape(String src) {
		return escape(new CharArrayIterator(src), src.length());
	}
	
	public int getEscapeLength(char c) {
		if (c == escapeChar || ArrayUtil.firstIndexOf(unsafes, c) >= 0)
			return 2;
		else {
			if (preferUnicode && !isRegular(c))
				return 6;
			else
				return 1;
		}
	}
	
	public char[] escape(char c) {
		char[] result = new char[getEscapeLength(c)];
		insertEscapeSequence(c, result, 0);
		return result;
	}
	
	protected int insertEscapeSequence(char c, char[] dest, int offset) {
		if (c == escapeChar) {
			dest[  offset] = escapeChar;
			dest[++offset] = escapeChar;
			return 2;
		}
		
		int index = ArrayUtil.firstIndexOf(unsafes, c);
		
		if (index >= 0) {
			dest[  offset] = escapeChar;
			dest[++offset] = safes[index];
			return 2;
		} else {
			if (preferUnicode && !isRegular(c)) {
				dest[  offset] = escapeChar;
				dest[++offset] = unicodeEscapeChar;
				dest[++offset] = StringUtil.hexDigit(c >>= (4 * 3));
				dest[++offset] = StringUtil.hexDigit(c >>= (4 * 2));
				dest[++offset] = StringUtil.hexDigit(c >>= (4 * 1));
				dest[++offset] = StringUtil.hexDigit(c >>  (4 * 0));
				return 6;
			} else {
				dest[offset] = c;
				return 1;
			}
		}
	}
	
	public char[] unescape(CharacterIterator src, int length) throws EscapeException {
		int end;
		if (length < 0) {
			end = src.getEndIndex();
		} else {
			end = src.getIndex() + length;
			if (end > src.getEndIndex()) 
				throw new IllegalArgumentException("length = " + length + ", " + (src.getEndIndex() - src.getIndex()) + " characters available");
		}

		int start = src.getIndex();
		int resultLength = 0;
		
		while (src.getIndex() < end) {
			skipOneSequence(src);
			src.next();
			resultLength++;
		}
		
		char[] result = new char[resultLength];
		int pos = 0;
		src.setIndex(start);

		while (src.getIndex() < end) {
			result[pos++] = unescapeOneSequence(src);
			src.next();
		}
		
		return result;
	}
	
	public char[] unescape(CharacterIterator src) throws EscapeException {
		return unescape(src, -1);
	}
	
	public char[] unescape(char[] src) throws EscapeException {
		return unescape(new CharArrayIterator(src), src.length);
	}
	
	public char[] unescape(String src) throws EscapeException {
		return unescape(new CharArrayIterator(src), src.length());
	}
	
	public void skipOneSequence(CharacterIterator src) {
		if (src.current() == escapeChar) {
			if (src.next() == unicodeEscapeChar) {
				src.setIndex(src.getIndex() + 4);
			}
		}
	}
	
	public char unescapeOneSequence(CharacterIterator src) throws EscapeException {
		int rollbackIndex = src.getIndex();
		try {
			char c = src.current();
			if (c == escapeChar) {
				c = src.next();
				
				if (c == CharacterIterator.DONE)
					throw new EscapeException("Incomplete escape sequence at the end");
				
				if (c == escapeChar)
					return c;
				
				if (c == unicodeEscapeChar) {
					return (char) (
							hexValue(src.next()) << (4 * 3) |
							hexValue(src.next()) << (4 * 2) |
							hexValue(src.next()) << (4 * 1) |
							hexValue(src.next()) << (4 * 0)
					);
				}
				
				int index = ArrayUtil.firstIndexOf(safes, c);
				if (index >= 0)
					return unsafes[index];
				
				if (strict)
					throw new EscapeException("Unknown escape sequence \"" + escapeChar + c + "\"");
				else
					return c;
			} else
				return c;
		} catch (EscapeException | RuntimeException e) {
			src.setIndex(rollbackIndex);
			throw e;
		}
	}
	
	private static int hexValue(char c) throws EscapeException {
		if (c <  '0') throw new EscapeException("Invalid hex digit '" + c + "', expected [0-9A-Fa-f]");
		if (c <= '9') return c - '0';
		if (c <  'A') throw new EscapeException("Invalid hex digit '" + c + "', expected [0-9A-Fa-f]");
		if (c <= 'F') return c - 'A';
		if (c <  'a') throw new EscapeException("Invalid hex digit '" + c + "', expected [0-9A-Fa-f]");
		if (c <= 'f') return c - 'a';
		if (c == CharacterIterator.DONE) throw new EscapeException("Incomplete Unicode escape sequence at the end");
		throw new EscapeException("Invalid hex digit '" + c + "', expected [0-9A-Fa-f]");
	}

	protected static boolean isRegular(char c) {
		return c >= ' ' && c <= '~';
	}

	public char getEscapeChar() {
		return escapeChar;
	}

	public char getUnicodeEscapeChar() {
		return unicodeEscapeChar;
	}

	public char[] getSafes() {
		return safes;
	}

	public char[] getUnsafes() {
		return unsafes;
	}

	public boolean isPreferUnicode() {
		return preferUnicode;
	}

	public boolean isStrict() {
		return strict;
	}

}
