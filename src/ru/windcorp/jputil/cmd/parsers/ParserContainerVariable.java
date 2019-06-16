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
import ru.windcorp.jputil.cmd.Invocation;

public class ParserContainerVariable extends Parser {

	private final Parser[] items;
	
	public ParserContainerVariable(String id, Parser[] groups) {
		super(id);
		this.items = groups;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator)
	 */
	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, Invocation inv) {
		Supplier<Exception> problem = null;
		Supplier<Exception> firstProblem = null;
		
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
	public boolean matches(CharacterIterator data) {
		int index = data.getIndex();
		
		for (Parser item : items) {
			if (item.matches(data)) {
				return true;
			} else {
				data.setIndex(index);
				continue;
			}
		}
		
		return false;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		int index = data.getIndex();
		
		int i;
		for (i = 0; i < items.length; ++i) {
			boolean matches = items[i].matches(data);
			data.setIndex(index);
			
			if (matches) {
				output.accept(true);
				items[i].parse(data, output);
				i++;
				break;
			} else {
				output.accept(false);
				items[i].insertEmpty(output);
				continue;
			}
		}
		
		for (; i < items.length; ++i) {
			output.accept(false);
			items[i].insertEmpty(output);
		}
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertEmpty(java.util.function.Consumer)
	 */
	@Override
	public void insertEmpty(Consumer<Object> output) {
		for (Parser item : items) {
			output.accept(false);
			item.insertEmpty(output);
		}
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		for (Parser item : items) {
			output.accept(Boolean.TYPE);
			item.insertArgumentClasses(output);
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
			
			if (
					item instanceof ParserContainerOptional ||
					item instanceof ParserLiteral ||
					item instanceof ParserTrailingSpecific ||
					item instanceof ParserTrailingString ||
					item instanceof ParserContainerGroup) {
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
