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
package ru.windcorp.jputil.selectors;

import java.util.Deque;
import java.util.function.Predicate;

public class OperatorAnd extends AbstractSelectorOperator {
	
	public OperatorAnd(String... names) {
		super(names);
	}

	@Override
	public <T> void process(Deque<Predicate<T>> stack) {
		Predicate<T> arg2 = stack.pop();
		Predicate<T> arg1 = stack.pop();
		stack.push(obj -> arg1.test(obj) && arg2.test(obj));
	}

}
