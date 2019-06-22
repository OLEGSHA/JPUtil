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
package ru.windcorp.jputil.quickcfg;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import ru.windcorp.jputil.chars.CharPredicate;
import ru.windcorp.jputil.chars.StringUtil;

public class QCFGParser {
	
	public static final CharPredicate WHITESPACE = Character::isWhitespace;
	public static final CharPredicate NEWLINES = CharPredicate.forArray('\n', '\r');
	public static final CharPredicate WHITESPACE_NO_NEWLINES = c -> WHITESPACE.test(c) && !NEWLINES.test(c);
	
	public static interface Setter {
		void set(String  declar) throws InvalidSettingException;
		
		static int parseInt(String declar, int min, int max) throws InvalidSettingException {
			try {
				int x = Integer.parseInt(declar);
				
				if (x < min) {
					throw new InvalidSettingException("Minimum " + min + " required, " + x + " given");
				} else if (x > max) {
					throw new InvalidSettingException("Maximum " + max + " required, " + x + " given");
				}
				
				return x;
			} catch (NumberFormatException e) {
				throw new InvalidSettingException("\"" + declar + "\" is not an integer", e);
			}
		}
	}
	
	private char intraSep = '=';
	private CharPredicate interSep = WHITESPACE;
	private CharPredicate trimValue = WHITESPACE_NO_NEWLINES;
	private CharPredicate trimName = WHITESPACE_NO_NEWLINES;
	private char commentStart = '#';
	private CharPredicate commentEnd = NEWLINES;

	private class Setting {
		private final String name;
		private final boolean isOptional;
		private boolean isSet = false;
		private final Setter setter;
		
		public Setting(String name, boolean isOptional, Setter setter) {
			this.name = name;
			this.isOptional = isOptional;
			this.setter = setter;
		}

		public void set(String value) throws QCFGException {
			if (!allowRepeats && isSet) {
				throw new QCFGException("Duplicate declaration for \"" + name + "\"");
			}
			
			try {
				InvalidSettingException.startParsing(name);
				setter.set(value);
				InvalidSettingException.endParsing();
				isSet = true;
			} catch (InvalidSettingException e) {
				// Do nothing, rethrow
				throw e;
			} catch (RuntimeException e) {
				throw new InvalidSettingException("Unexpected runtime exception", e);
			} catch (Throwable e) {
				InvalidSettingException.endParsing();
				throw e;
			}
		}
	}
	
	private final Map<String, Setting> settings = new HashMap<>();
	
	private boolean allowRepeats = false;
	private boolean ignoreUnknown = false;
	
	public char getIntraSep() {
		return intraSep;
	}

	public QCFGParser intraSeparator(char intraSep) {
		this.intraSep = intraSep;
		return this;
	}

	public CharPredicate getInterSep() {
		return interSep;
	}

	public QCFGParser interSeparator(CharPredicate interSep) {
		this.interSep = interSep;
		return this;
	}

	public CharPredicate getTrimValue() {
		return trimValue;
	}

	public QCFGParser removeBeforeValue(CharPredicate trimValue) {
		this.trimValue = trimValue;
		return this;
	}

	public CharPredicate getTrimName() {
		return trimName;
	}

	public QCFGParser removeAfterName(CharPredicate trimName) {
		this.trimName = trimName;
		return this;
	}
	
	public QCFGParser doNotTrim() {
		removeAfterName(c -> false);
		removeBeforeValue(c -> false);
		return this;
	}

	public char getCommentStart() {
		return commentStart;
	}

	public QCFGParser commentStart(char commentStart) {
		this.commentStart = commentStart;
		return this;
	}

	public CharPredicate getCommentEnd() {
		return commentEnd;
	}

	public QCFGParser commentEnd(CharPredicate commentEnd) {
		this.commentEnd = commentEnd;
		return this;
	}
	
	public boolean getAllowRepeats() {
		return allowRepeats;
	}

	public QCFGParser setAllowRepeats(boolean allowRepeats) {
		this.allowRepeats = allowRepeats;
		return this;
	}
	
	public QCFGParser allowRepeats() {
		setAllowRepeats(true);
		return this;
	}

	public boolean getIgnoreUnknown() {
		return ignoreUnknown;
	}

	public QCFGParser setIgnoreUnknown(boolean ignoreUnknown) {
		this.ignoreUnknown = ignoreUnknown;
		return this;
	}
	
	public QCFGParser ignoreUnknown() {
		setIgnoreUnknown(true);
		return this;
	}

	public QCFGParser require(String name, Setter output) {
		settings.put(name, new Setting(name, false, output));
		return this;
	}
	
	public QCFGParser optional(String name, Setter output) {
		settings.put(name, new Setting(name, true, output));
		return this;
	}
	
	/*
	 * Modes
	 */
	private static final int
			INTERSTATEMENT = 0,
			READING_NAME = 1,
			TRIMMING_NAME = 2,
			TRIMMING_VALUE = 3,
			READING_VALUE = 4,
			COMMENT = 5;
	
	public void parse(Reader reader) throws IOException, QCFGException, InvalidSettingException {
		LineNumberReader lineReader = new LineNumberReader(reader);
		char current;
		
		int mode = INTERSTATEMENT;
		StringBuilder sb = new StringBuilder();
		
		String name = null;

		int read = lineReader.read();
		if (read != -1) {
			current = (char) read;
			
			processing:
			while (true) {
				switch (mode) {
				case INTERSTATEMENT:
					if (getCommentStart() == current) {
						mode = COMMENT;
						break;
					}
					if (!getInterSep().test(current)) {
						mode = READING_NAME;
						continue processing;
					}
					break;
					
				case READING_NAME:
					if (getTrimName().test(current)) {
						if (sb.length() == 0) {
							throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Empty name");
						}
						name = StringUtil.resetStringBuilder(sb);
						mode = TRIMMING_NAME;
						break;
					}
					if (getIntraSep() == current) {
						if (sb.length() == 0) {
							throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Empty name");
						}
						name = StringUtil.resetStringBuilder(sb);
						mode = TRIMMING_VALUE;
						break;
					}
					sb.append(current);
					break;
					
				case TRIMMING_NAME:
					if (getIntraSep() == current) {
						mode = TRIMMING_VALUE;
						break;
					}
					if (!getTrimName().test(current)) {
						throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Illegal characters after name " + name);
					}
					break;
					
				case TRIMMING_VALUE:
					if (!getTrimValue().test(current)) {
						mode = READING_VALUE;
						continue processing;
					}
					break;
					
				case READING_VALUE:
					if (getInterSep().test(current)) {
						Setting setting = settings.get(name);
						if (!ignoreUnknown && setting == null) {
							throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Unknown setting \"" + name + "\"");
						}
						
						setting.set(StringUtil.resetStringBuilder(sb));
						
						mode = INTERSTATEMENT;
						break;
					}
					sb.append(current);
					break;
					
				case COMMENT:
					if (getCommentEnd().test(current)) {
						mode = INTERSTATEMENT;
						break;
					}
					break;
				}
				
				read = lineReader.read();
				if (read == -1) {
					break;
				}
				current = (char) read;
			}
			
			if (mode == READING_NAME || mode == TRIMMING_NAME) {
				throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Last statement is incomplete: value missing");
			} else if (mode == TRIMMING_VALUE || mode == READING_VALUE) {
				Setting setting = settings.get(name);
				if (!ignoreUnknown && setting == null) {
					throw new QCFGException("[Line " + lineReader.getLineNumber() + "] Unknown setting \"" + name + "\"");
				}
				
				setting.set(sb.toString());
			}
		}
		
		for (Setting setting : settings.values()) {
			if (!setting.isSet && !setting.isOptional) {
				throw new QCFGException("Setting \"" + setting.name + "\" not set");
			}
		}
	}

}
