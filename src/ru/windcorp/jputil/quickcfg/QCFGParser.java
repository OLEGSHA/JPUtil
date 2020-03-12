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
import java.util.function.IntSupplier;

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
				isSet = true;
			} catch (InvalidSettingException e) {
				// Do nothing, rethrow
				throw e;
			} catch (RuntimeException e) {
				throw new InvalidSettingException("Unexpected runtime exception", e);
			} finally {
				InvalidSettingException.endParsing();
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
	
	private enum Mode {
		INTERSTATEMENT,
		READING_NAME,
		TRIMMING_NAME,
		TRIMMING_VALUE,
		READING_VALUE,
		COMMENT;
		
		private ModeParser parser = null;
		
		ModeParser getParser() {
			return parser;
		}
	}
	
	@FunctionalInterface
	private static interface ModeParser {
		Mode process(QCFGParser parser, Context context) throws QCFGException;
	}
	
	private static class Context {
		char current;

		private String name = null;
		private final StringBuilder sb = new StringBuilder();
		private boolean processAgain = false;
		
		private final IntSupplier lineNumberGetter;
		
		Context(IntSupplier lineNumberGetter) {
			this.lineNumberGetter = lineNumberGetter;
		}
		
		void processAgain() {
			processAgain = true;
		}

		boolean getAndResetProcessAgain() {
			boolean result = processAgain;
			processAgain = false;
			return result;
		}
		
		int getLineNumber() {
			return lineNumberGetter.getAsInt();
		}
		
		void appendToBuffer() {
			sb.append(current);
		}
		
		boolean isBufferEmpty() {
			return sb.length() == 0;
		}
		
		String getAndResetBuffer() {
			return StringUtil.resetStringBuilder(sb);
		}
		
		String getName() {
			return name;
		}
		
		void setNameToBuffer() {
			this.name = getAndResetBuffer();
		}
	}
	
	static {
		Mode.INTERSTATEMENT.parser = (parser, context) -> {
			
			if (parser.getCommentStart() == context.current) {
				return Mode.COMMENT;
			}
			if (!parser.getInterSep().test(context.current)) {
				context.processAgain();
				return Mode.READING_NAME;
			}
			return Mode.INTERSTATEMENT;
			
		};
		
		Mode.READING_NAME.parser = (parser, context) -> {
			
			if (
					parser.getTrimName().test(context.current)
					||
					parser.getIntraSep() == context.current
			) {
				if (context.isBufferEmpty()) {
					throw new QCFGException(context.getLineNumber(), "Empty name");
				}
				context.setNameToBuffer();
				
				return (parser.getIntraSep() == context.current)
					? Mode.TRIMMING_VALUE
					: Mode.TRIMMING_NAME;
			}
			
			context.appendToBuffer();
			return Mode.READING_NAME;
			
		};
		
		Mode.TRIMMING_NAME.parser = (parser, context) -> {

			if (parser.getIntraSep() == context.current) {
				return Mode.TRIMMING_VALUE;
			}
			if (!parser.getTrimName().test(context.current)) {
				throw new QCFGException(context.getLineNumber(), "Illegal characters after name " + context.getName());
			}
			
			return Mode.TRIMMING_NAME;
			
		};
		
		Mode.TRIMMING_VALUE.parser = (parser, context) -> {

			if (!parser.getTrimValue().test(context.current)) {
				context.processAgain();
				return Mode.READING_VALUE;
			}
			return Mode.TRIMMING_VALUE;
			
		};
		
		Mode.READING_VALUE.parser = (parser, context) -> {
			
			if (parser.getInterSep().test(context.current)) {
				Setting setting = parser.settings.get(context.getName());
				if (!parser.ignoreUnknown && setting == null) {
					throw new QCFGException(context.getLineNumber(), "Unknown setting \"" + context.getName() + "\"");
				}
				
				setting.set(context.getAndResetBuffer());
				
				return Mode.INTERSTATEMENT;
			}
			context.appendToBuffer();
			return Mode.READING_VALUE;
			
		};
		
		Mode.COMMENT.parser = (parser, context) -> {
			
			if (parser.getCommentEnd().test(context.current)) {
				return Mode.INTERSTATEMENT;
			}
			return Mode.COMMENT;
			
		};
	}
	
	public void parse(Reader reader) throws IOException, QCFGException {
		LineNumberReader lineReader = new LineNumberReader(reader);
		
		Mode mode = Mode.INTERSTATEMENT;
		Context context = new Context(lineReader::getLineNumber);
		
		while (true) {
			if (!context.getAndResetProcessAgain()) {
				int read = lineReader.read();
				if (read == -1) break;
				context.current = (char) read;
			}
			
			mode = mode.getParser().process(this, context);
		}
			
		handleEOF(mode, context);
		checkRequiredSettings();
	}

	private void handleEOF(Mode mode, Context context) throws QCFGException {
		if (mode == Mode.READING_NAME || mode == Mode.TRIMMING_NAME) {
			throw new QCFGException(context.getLineNumber(), "Last statement is incomplete: value missing");
		} else if (mode == Mode.TRIMMING_VALUE || mode == Mode.READING_VALUE) {
			Setting setting = settings.get(context.getName());
			if (!ignoreUnknown && setting == null) {
				throw new QCFGException(context.getLineNumber(), "Unknown setting \"" + context.getName() + "\"");
			}
			
			setting.set(context.getAndResetBuffer());
		}
	}
	
	private void checkRequiredSettings() throws QCFGException {
		for (Setting setting : settings.values()) {
			if (!setting.isSet && !setting.isOptional) {
				throw new QCFGException("Setting \"" + setting.name + "\" not set");
			}
		}
	}

}
