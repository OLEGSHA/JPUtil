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

import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.parsers.Parser.NoBrackets;

public class ParserContainerOptional extends Parser implements NoBrackets {// TODO make several attempts to parse arguments when optional arguments exist (e.g. [arg1] <arg2> fails if only arg2 is present)
	
	private final Parser contents;

	public ParserContainerOptional(String id, Parser item) {
		super(id, appendPresentMarkerToArgumentClasses(item));
		this.contents = item;
	}

	private static Class<?>[] appendPresentMarkerToArgumentClasses(Parser parser) {
		Class<?>[] result = new Class<?>[parser.getArgumentClasses().length + 1];
		result[0] = Boolean.TYPE;
		System.arraycopy(parser.getArgumentClasses(), 0, result, 1, parser.getArgumentClasses().length);
		return result;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator)
	 */
	
	// SonarLint: Boxed "Boolean" should be avoided in boolean expressions (java:S5411)
	//   The Boolean obtained with getApproachData cannot be null by design
	@SuppressWarnings("squid:S5411")
	
	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		if (inv.getApproachData(this, Boolean.class)) {
			return contents.getProblem(data, inv);
		}
		
		return null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	
	// SonarLint: Boxed "Boolean" should be avoided in boolean expressions (java:S5411)
	//   The Boolean obtained with getApproachData cannot be null by design
	@SuppressWarnings("squid:S5411")
	
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		if (inv.getApproachData(this, Boolean.class)) {
			return contents.matches(data, inv);
		}
		
		return true;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	
	// SonarLint: Boxed "Boolean" should be avoided in boolean expressions (java:S5411)
	//   The Boolean obtained with getApproachData cannot be null by design
	@SuppressWarnings("squid:S5411")
	
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		if (inv.getApproachData(this, Boolean.class)) {
			output.accept(true);
			contents.insertParsed(data, inv, output);
		} else {
			output.accept(false);
			contents.insertEmpty(output);
		}
	}
	
	// SonarLint: Boxed "Boolean" should be avoided in boolean expressions (java:S5411)
	//   The Boolean obtained with getApproachData cannot be null by design
	@SuppressWarnings("squid:S5411")
	
	@Override
	public boolean selectNextApproach(AutoInvocation inv) {
		if (inv.getApproachData(this, Boolean.class)) {
			if (!contents.selectNextApproach(inv)) {
				inv.setApproachData(this, false);
			}
			
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public void resetApproach(AutoInvocation inv) {
		inv.setApproachData(this, true);
		contents.resetApproach(inv);
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toString()
	 */
	@Override
	public String toString() {
		return toDebugString();
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toDebugString(ru.windcorp.tge2.util.IndentedStringBuilder)
	 */
	@Override
	protected void toDebugString(IndentedStringBuilder sb) {
		sb.append("Optional ");
		contents.toDebugString(sb);
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		formatter.appendStructureChar(sb, '[');
		contents.toSyntax(sb, formatter);
		formatter.appendStructureChar(sb, ']');
	}

}
