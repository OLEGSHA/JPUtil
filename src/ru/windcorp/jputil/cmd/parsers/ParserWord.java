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

import ru.windcorp.jputil.cmd.Invocation;

public class ParserWord extends Parser {

	public ParserWord(String id) {
		super(id);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.tge2.util.ncmd.Invocation)
	 */
	@Override
	public Supplier<? extends Exception> getProblem(CharacterIterator data, Invocation inv) {
		nextWord(data);
		return argNotFound(inv);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data) {
		return nextWord(data).length != 0;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		output.accept(new String(nextWord(data)));
		return;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		output.accept(String.class);
	}

}
