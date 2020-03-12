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
/**
 * Copyright (C) 2019 OLEGSHA
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
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.jputil.cmd.parsers;

import java.text.CharacterIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.chars.EscapeException;
import ru.windcorp.jputil.chars.Escaper;
import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Invocation;

public abstract class Parser {
	
	public interface NoBrackets {
		// Marker interface
	}
	
	private static final Escaper ESCAPER = Escaper.JAVA;
	
	private final String id;
	private final Class<?>[] argumentClasses;
	
	private String typeAsDeclared = null;
	
	public Parser(String id, Class<?>... argumentClasses) {
		this.id = id;
		this.argumentClasses = argumentClasses;
	}
	
	/**
	 * @return <code>true</true> is <code>data</code>
	 */
	public abstract boolean matches(CharacterIterator data, AutoInvocation inv);
	
	
	public abstract Supplier<Exception> getProblem(CharacterIterator data, AutoInvocation inv);
	public abstract void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output);
	
	public Class<?>[] getArgumentClasses() {
		return argumentClasses;
	}
	
	/**
	 * Selects the next approach if possible.
	 * @param inv - invocation context
	 * @return {@code false} if next approach could not be selected, {@code true} otherwise 
	 */
	public boolean selectNextApproach(AutoInvocation inv) {
		// Do nothing
		return false;
	}
	
	/**
	 * Selects the first approach in this parser and all child parsers.
	 */
	public void resetApproach(AutoInvocation inv) {
		// Do nothing
	}
	
	public final void insertEmpty(Consumer<Object> output) {
		for (int i = 0; i < argumentClasses.length; ++i) output.accept(null);
	}
	
	protected void skipWhitespace(CharacterIterator data) {
		while (Character.isWhitespace(data.current()))
			data.next();
	}
	
	protected char[] readWord(CharacterIterator data) {
		int chars = 0;
		int start = data.getIndex();
		
		char c = data.current();
		if (c == '"') {
			c = data.next();
			try {
				@SuppressWarnings("deprecation")
				char[] unescaped = ESCAPER.unescape(data, '"');
				if (data.current() == '"') {
					data.next();
					return unescaped;
				}
			} catch (EscapeException e) {
				// Do nothing
			}

			data.setIndex(start); // Invalid escape, assume no escape
		}
		
		while (!Character.isWhitespace(c)) {
			if (c == CharacterIterator.DONE) {
				break;
			}
			chars++;
			c = data.next();
		}
		
		data.setIndex(start);
		
		char[] result = new char[chars];
		for (int i = 0; i < chars; ++i) {
			result[i] = data.current();
			data.next();
		}
		
		return result;
	}
	
	protected char[] nextWord(CharacterIterator data) {
		skipWhitespace(data);
		return readWord(data);
	}
	
	protected int readInt(CharacterIterator data) {
		int result = 0;
		
		if (data.current() == '-') {
			while (data.next() >= '0' && data.current() <= '9') result = result * 10 - (data.current() - '0');
		} else {
			if (data.current() == '+') data.next();
			while (data.current() >= '0' && data.current() <= '9') {
				result = result * 10 + (data.current() - '0');
				data.next();
			}
		}
		
		return result;
	}
	
	// SonarLint: Cognitive Complexity of methods should not be too high (java:S3776)
	//   Simple enough
	@SuppressWarnings("squid:S3776")
	
	protected boolean matchInt(CharacterIterator data) {
		long result = 0;
		boolean isPositive = true;
		
		if (data.current() == '-') {
			isPositive = false;
			data.next();
		} else if (data.current() == '+') {
			data.next();
		}
		
		if (data.current() < '0' || data.current() > '9') {
			return false;
		}
		
		do {
			result = result * 10 + (data.current() - '0');
			if (isPositive) {
				if (+result > Integer.MAX_VALUE) return false;
			} else {
				if (-result < Integer.MIN_VALUE) return false;
			}
		} while (data.next() >= '0' && data.current() <= '9');
		
		return data.current() == CharacterIterator.DONE || Character.isWhitespace(data.current());
	}
	
	protected Supplier<Exception> argNotFound(Invocation inv) {
		return () -> new CommandSyntaxException(
				inv,
				inv.getContext().translate(
						"auto.generic.argNotFound", "Argument %1$s not found",
						getId()
				)
		);
	}
	
	public boolean matchOrReset(CharacterIterator data, AutoInvocation inv) {
		int index = data.getIndex();
		if (matches(data, inv)) {
			return true;
		} else {
			data.setIndex(index);
			return false;
		}
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * @return the typeAsDeclared
	 */
	public String getTypeAsDeclared() {
		return typeAsDeclared;
	}
	
	/**
	 * @param typeAsDeclared the typeAsDeclared to set
	 */
	void setTypeAsDeclared(String typeAsDeclared) {
		this.typeAsDeclared = typeAsDeclared;
	}
	
	@Override
	public String toString() {
		return (getTypeAsDeclared() == null ? getClass().getSimpleName() : getTypeAsDeclared()) + " " + getId();
	}
	
	public final String toSyntax(SyntaxFormatter formatter) {
		StringBuilder sb = new StringBuilder();
		toSyntax(sb, formatter == null ? SyntaxFormatters.PLAIN : formatter);
		return sb.toString();
	}
	
	public final String toSyntax() {
		return toSyntax(null);
	}
	
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		formatter.appendType(sb, getTypeAsDeclared());
		sb.append(" ");
		formatter.appendId(sb, getId());
	}
	
	public final String toDebugString() {
		IndentedStringBuilder sb = new IndentedStringBuilder();
		toDebugString(sb);
		return sb.toString();
	}
	
	protected void toDebugString(IndentedStringBuilder sb) {
		sb.append(toString());
	}

}
