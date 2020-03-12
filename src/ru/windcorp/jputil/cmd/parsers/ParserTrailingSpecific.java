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

import java.lang.reflect.Array;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.PrimitiveUtil;
import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.parsers.Parser.NoBrackets;

public class ParserTrailingSpecific extends Parser implements NoBrackets {
	
	private final Parser parser;
	
	private final Class<?>[] wrappedOutputClasses;
	private final Class<?>[] rawOutputClasses;

	public ParserTrailingSpecific(String id, Parser parser) {
		super(id, createOutputArrayClasses(parser));
		this.parser = parser;
		
		rawOutputClasses = parser.getArgumentClasses();
		wrappedOutputClasses = new Class<?>[rawOutputClasses.length];
		
		for (int i = 0; i < wrappedOutputClasses.length; ++i) {
			wrappedOutputClasses[i] = PrimitiveUtil.getBoxedClass(rawOutputClasses[i]);
		}
	}

	private static Class<?>[] createOutputArrayClasses(Parser parser) {
		Class<?>[] result = new Class<?>[parser.getArgumentClasses().length];
		
		for (int i = 0; i < result.length; ++i) {
			result[i] = Array.newInstance(parser.getArgumentClasses()[i], 0).getClass(); // WTF, JRE? I want my getArrayType()!
		}
		
		return result;
	}

	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		while (data.getIndex() < data.getEndIndex()) {
			int index = data.getIndex();
			if (!parser.matches(data, inv)) {
				data.setIndex(index);
				return parser.getProblem(data, inv);
			}
		}
		
		return null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		while (data.getIndex() < data.getEndIndex()) {
			if (!parser.matches(data, inv)) {
				return false;
			}
		}
		
		return true;
	}
	
	private class OutputCollector implements Consumer<Object> {
		
		final List<List<Object>> collectors;
		int index = 0;
		
		OutputCollector() {
			collectors = new ArrayList<>(wrappedOutputClasses.length);
			for (int i = 0; i < wrappedOutputClasses.length; ++i)
				collectors.add(new ArrayList<>());
		}
		
		@Override
		public void accept(Object t) {
			Class<?> expectedClass = wrappedOutputClasses[index];
			
			if (!expectedClass.isInstance(t)) {
				throw new IllegalArgumentException("Expecting argument of type "
						+ expectedClass + ", received type " + t.getClass()
						+ " (\"" + t + "\") at index " + index);
			}
			
			collectors.get(index).add(t);
			index++;
			if (index == wrappedOutputClasses.length) index = 0;
		}
		
		void insertResults(Consumer<Object> output) {
			for (int i = 0; i < collectors.size(); ++i) {
				// Dark majik ensues
				List<Object> collector = collectors.get(i);
				Object array = Array.newInstance(rawOutputClasses[i], collector.size());
				
				int index = 0;
				for (Object object : collector) {
					Array.set(array, index, object);
					index++;
				}
				
				output.accept(array);
			}
		}
		
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		OutputCollector collector = new OutputCollector();
		
		while (data.getIndex() < data.getEndIndex()) {
			parser.insertParsed(data, inv, collector);
		}
		
		collector.insertResults(output);
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
		sb.append("Trailing {").indent().newLine();
		parser.toDebugString(sb);
		sb.unindent().newLine().append("}");
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#toSyntax(java.lang.StringBuilder, ru.windcorp.jputil.cmd.parsers.SyntaxFormatter)
	 */
	@Override
	protected void toSyntax(StringBuilder sb, SyntaxFormatter formatter) {
		if (parser instanceof NoBrackets) {
			parser.toSyntax(sb, formatter);
		} else {
			formatter.appendStructureChar(sb, '<');
			parser.toSyntax(sb, formatter);
			formatter.appendStructureChar(sb, '>');
		}
		formatter.appendTrailing(sb);
	}

}
