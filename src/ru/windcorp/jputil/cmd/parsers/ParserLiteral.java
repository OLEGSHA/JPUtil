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
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.jputil.cmd.parsers;

import java.text.CharacterIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.cmd.parsers.Parser.NoBrackets;

/**
 * @author Javapony
 *
 */
public class ParserLiteral extends Parser implements NoBrackets {
	
	private final String template;
	private final char[] templateChars;

	public ParserLiteral(String id, String template) {
		super(id);
		
		if (Objects.requireNonNull(template, "template").isEmpty()) {
			throw new IllegalArgumentException("Template is empty");
		}

		this.template = template;
		this.templateChars = template.toCharArray();
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public Supplier<? extends Exception> getProblem(CharacterIterator data, Invocation inv) {
		String arg = String.valueOf(nextWord(data));
		if (arg.length() == 0) return argNotFound(inv);
		return () -> new CommandSyntaxException(inv, inv.getContext().translate("auto.literal.doesNotMatch", "\"%2$s\" expected, \"%1$s\" encountered", arg, template));
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data) {
		skipWhitespace(data);
		
		for (int i = 0; i < templateChars.length; ++i) {
			if (data.next() != templateChars[i]) {
				return false;
			}
		}
		
		return true;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		skipWhitespace(data);
		while (!Character.isWhitespace(data.current()))
			data.next();
		// Output nothing
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		// Output nothing
	}

	public String getTemplate() {
		return template;
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toString()
	 */
	@Override
	public String toString() {
		return toDebugString();
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toDebugString(ru.windcorp.jputil.chars.IndentedStringBuilder)
	 */
	@Override
	protected void toDebugString(IndentedStringBuilder sb) {
		sb.append("\"");
		sb.append(getTemplate());
		sb.append("\"");
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		formatter.appendStructureChar(sb, '"');
		formatter.appendLiteral(sb, getTemplate());
		formatter.appendStructureChar(sb, '"');
	}

}
