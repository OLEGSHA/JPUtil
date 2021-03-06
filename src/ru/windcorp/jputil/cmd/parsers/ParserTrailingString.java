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

import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.parsers.Parser.NoBrackets;

public class ParserTrailingString extends Parser implements NoBrackets {

	public ParserTrailingString(String id) {
		super(id, String.class);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.tge2.util.ncmd.Invocation)
	 */
	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		return null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		data.setIndex(data.getEndIndex());
		return true;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	
	// SonarLint: Nested code blocks should not be used (java:S1199)
	//   Simple enough. Did not want to pollute locals with c and i
	@SuppressWarnings("squid:S1199")
	
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		skipWhitespace(data);
		StringBuilder sb = new StringBuilder();
		int lastNonWhitespace = 0;
		
		{
			char c = data.current();
			int i = 1;
			while (c != CharacterIterator.DONE) {
				if (!Character.isWhitespace(c)) {
					lastNonWhitespace = i;
				}
				sb.append(c);
				c = data.next();
				i++;
			}
		}
		
		sb.setLength(lastNonWhitespace);
		output.accept(sb.toString());
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toString()
	 */
	@Override
	public String toString() {
		return getId() + "...";
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		formatter.appendId(sb, getId());
		formatter.appendTrailing(sb);
	}

}
