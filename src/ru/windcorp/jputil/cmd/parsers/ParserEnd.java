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

import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Invocation;

public class ParserEnd extends Parser {

	public ParserEnd() {
		super("End");
	}

	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, Invocation inv) {
		skipWhitespace(data);
		if (data.getIndex() < data.getEndIndex()) {
			char[] chars = new char[data.getEndIndex() - data.getIndex()];
			int length = 0;
			
			int i = 0;
			while (data.getIndex() < data.getEndIndex()) {
				chars[i++] = data.current();
				if (!Character.isWhitespace(data.current())) {
					length = i; // Incremented
				}
				data.next();
			}
			
			String excessiveArgs = String.valueOf(chars, 0, length);
			return () -> new CommandSyntaxException(inv,
					inv.getContext().translate("auto.end.excessive", "Excessive arguments \"%1$s\"", excessiveArgs));
		}
		return null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data) {
		skipWhitespace(data);
		return data.getIndex() >= data.getEndIndex();
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		// Do nothing
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		// Do nothing
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toString()
	 */
	@Override
	public String toString() {
		return "End";
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		formatter.appendStructureChar(sb, '.');
	}

}
