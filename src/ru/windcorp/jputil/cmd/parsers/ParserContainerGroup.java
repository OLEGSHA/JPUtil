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

public class ParserContainerGroup extends Parser {
	
	private final Parser[] items; 

	public ParserContainerGroup(String id, Parser[] items) {
		super(id, concatenateArgumentClasses(items));
		this.items = items;
	}
	
	private static Class<?>[] concatenateArgumentClasses(Parser[] parsers) {
		int length = 0;
		
		for (Parser p : parsers)
			length += p.getArgumentClasses().length;
		
		Class<?>[] result = new Class<?>[length];
		int i = 0;
		
		for (Parser p : parsers) {
			System.arraycopy(
					p.getArgumentClasses(), 0,
					result, i,
					p.getArgumentClasses().length);
			i += p.getArgumentClasses().length;
		}
		
		return result;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator)
	 */
	@Override
	public Supplier<? extends Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		Supplier<? extends Exception> problem = null;
		
		for (Parser item : items) {
			problem = item.getProblem(data, inv);
			if (problem != null) {
				return problem;
			}
		}
		
		return null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		for (Parser item : items) {
			if (!item.matches(data, inv))
				return false;
		}
		return true;
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		for (Parser item : items) {
			item.insertParsed(data, inv, output);
		}
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
		sb.append("Group " + getId() + " {").indent();
		for (Parser item : items) {
			sb.newLine();
			item.toDebugString(sb);
		}
		sb.unindent().newLine().append("}");
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		for (int i = 0; i < items.length; ++i) {
			Parser item = items[i];
			
			if (item instanceof NoBrackets) {
				item.toSyntax(sb, formatter);
			} else {
				formatter.appendStructureChar(sb, '<');
				item.toSyntax(sb, formatter);
				formatter.appendStructureChar(sb, '>');
			}
			
			if (i != items.length - 1) {
				sb.append(' ');
			}
		}
	}
	
	@Override
	public boolean selectNextApproach(AutoInvocation inv) {
		boolean couldSelectNext = false;
		
		for (int i = items.length - 1; i >= 0; --i) {
			couldSelectNext = items[i].selectNextApproach(inv);
			
			if (couldSelectNext) {
				break;
			} else {
				items[i].resetApproach(inv);
			}
		}
		
		return couldSelectNext;
	}
	
	@Override
	public void resetApproach(AutoInvocation inv) {
		for (Parser item : items) {
			item.resetApproach(inv);
		}
	}
	
}
