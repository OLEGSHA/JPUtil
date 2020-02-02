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

public class ParserContainerVariable extends Parser {

	private final Parser[] items;
	
	public ParserContainerVariable(String id, Parser[] groups) {
		super(id, concatenateArgumentClasses(groups));
		this.items = groups;
	}
	
	private static Class<?>[] concatenateArgumentClasses(Parser[] parsers) {
		int length = 0;
		
		for (Parser p : parsers)
			length += p.getArgumentClasses().length + 1;
		
		Class<?>[] result = new Class<?>[length];
		int i = 0;
		
		for (Parser p : parsers) {
			result[i++] = Boolean.TYPE;
			
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
		Supplier<? extends Exception> firstProblem = null;
		
		int index = data.getIndex();
		
		for (Parser item : items) {
			problem = item.getProblem(data, inv);
			if (problem == null) {
				return null;
			} else {
				if (firstProblem == null) {
					firstProblem = problem;
				}
				
				data.setIndex(index);
				continue;
			}
		}
		
		return firstProblem;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		int var = inv.getApproachData(this, Integer.class);
		return items[var].matches(data, inv);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		int var = inv.getApproachData(this, Integer.class);
		
		for (int i = 0; i < items.length; ++i) {
			if (i != var) {
				output.accept(false);
				items[i].insertEmpty(output);
			} else {
				output.accept(true);
				items[i].insertParsed(data, inv, output);
			}
		}
	}
	
	@Override
	public boolean selectNextApproach(AutoInvocation inv) {
		int var = inv.getApproachData(this, Integer.class);
		
		if (!items[var].selectNextApproach(inv)) {
			var++;
			
			if (var == items.length) {
				return false;
			} else {
				inv.setApproachData(this, var);
			}
		}
		
		return true;
	}
	
	@Override
	public void resetApproach(AutoInvocation inv) {
		inv.setApproachData(this, 0);
		for (Parser item : items) {
			item.resetApproach(inv);
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
		sb.append("Variable {");
		for (int i = 0; i < items.length; ++i) {
			sb.newLine().append(i).append(":").indent().newLine();
			items[i].toDebugString(sb);
			sb.unindent();
		}
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		for (int i = 0; i < items.length; ++i) {
			Parser item = items[i];
			
			if (item instanceof NoBrackets || item instanceof ParserContainerGroup) {
				item.toSyntax(sb, formatter);
			} else {
				formatter.appendStructureChar(sb, '<');
				item.toSyntax(sb, formatter);
				formatter.appendStructureChar(sb, '>');
			}
			
			if (i != items.length - 1) {
				sb.append(' ');
				formatter.appendStructureChar(sb, '|');
				sb.append(' ');
			}
		}
	}

}
