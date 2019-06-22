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
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.cmd.parsers.Parser.NoBrackets;

public class ParserTrailingSpecific extends Parser implements NoBrackets {
	
	private static final CharacterIterator EXHAUSTED_CHARACTER_ITERATOR = new StringCharacterIterator("");
	
	private final Parser parser;
	private final Class<?>[] outputArrayClasses;
	private final Class<?>[] outputClasses;

	public ParserTrailingSpecific(String id, Parser parser) {
		super(id);
		this.parser = parser;
		
		if (parser.matches(EXHAUSTED_CHARACTER_ITERATOR)) {
			throw new IllegalArgumentException("Invalid template parser " + parser
					+ ": matches empty string");
		}
		
		List<Class<?>> collector = new ArrayList<>(1);
		parser.insertArgumentClasses(collector::add);
		if (collector.isEmpty()) {
			throw new IllegalArgumentException("Invalid template parser " + parser
					+ ": must output one argument per appearance, outputs nothing");
		}
		
		outputClasses = collector.toArray(new Class<?>[collector.size()]);
		outputArrayClasses = new Class<?>[outputClasses.length];
		
		for (int i = 0; i < outputClasses.length; ++i) {
			outputArrayClasses[i] = Array.newInstance(outputClasses[i], 0).getClass(); // WTF, JRE? I want my getArrayType()!
		}
	}

	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, Invocation inv) {
		while (data.getIndex() < data.getEndIndex()) {
			int index = data.getIndex();
			if (!parser.matches(data)) {
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
	public boolean matches(CharacterIterator data) {
		while (data.getIndex() < data.getEndIndex()) {
			if (!parser.matches(data)) {
				return false;
			}
		}
		
		return true;
	}
	
	private class OutputCollector implements Consumer<Object> {
		
		final List<List<Object>> collectors;
		int index = 0;
		
		OutputCollector() {
			collectors = new ArrayList<>(outputClasses.length);
			for (int i = 0; i < outputClasses.length; ++i)
				collectors.add(new ArrayList<>());
		}
		
		@Override
		public void accept(Object t) {
			collectors.get(index).add(t);
			index++;
			if (index == outputClasses.length) index = 0;
		}
		
		void insertResults(Consumer<Object> output) {
			for (int i = 0; i < collectors.size(); ++i) {
				// Dark majik ensues
				List<Object> collector = collectors.get(i);
				Object array = Array.newInstance(outputClasses[i], collector.size());
				array = collector.toArray((Object[]) array);
				output.accept(array);
			}
		}
		
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		OutputCollector collector = new OutputCollector();
		
		while (data.getIndex() < data.getEndIndex()) {
			parser.parse(data, collector);
		}
		
		collector.insertResults(output);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		for (Class<?> outputArrayClass : outputArrayClasses) {
			output.accept(outputArrayClass);
		}
	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertEmpty(java.util.function.Consumer)
	 */
	@Override
	public void insertEmpty(Consumer<Object> output) {
		for (int i = 0; i < outputClasses.length; ++i) {
			output.accept(null);
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
