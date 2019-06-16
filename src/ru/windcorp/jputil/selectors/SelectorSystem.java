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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Stack;
import java.util.function.Predicate;

import ru.windcorp.jputil.SyntaxException;
import ru.windcorp.jputil.iterators.PeekingIterator;

public class SelectorSystem<T> {
	
	public static final char EXPRESSION_OPEN = '(';
	public static final char EXPRESSION_CLOSE = ')';
	
	private final Collection<Selector<? super T>> selectors =
			Collections.synchronizedCollection(new ArrayList<Selector<? super T>>());
	
	private final Collection<SelectorOperator> operators =
			Collections.synchronizedCollection(new ArrayList<SelectorOperator>());
	
	private String stackPrefix = null;
	
	public Collection<Selector<? super T>> getSelectors() {
		return this.selectors;
	}
	
	public Collection<SelectorOperator> getSelectorOperators() {
		return this.operators;
	}
	
	public String getStackPrefix() {
		return stackPrefix;
	}

	public SelectorSystem<T> setStackPrefix(String stackPrefix) {
		this.stackPrefix = stackPrefix;
		return this;
	}

	public SelectorSystem<T> add(Selector<? super T> selector) {
		getSelectors().add(selector);
		return this;
	}
	
	public SelectorSystem<T> add(SelectorOperator operator) {
		getSelectorOperators().add(operator);
		return this;
	}

	public Predicate<? super T> parse(Iterator<String> tokens) throws SyntaxException {
		PeekingIterator<String> peeker = new PeekingIterator<String>(tokens);
		
		if (getStackPrefix() != null && peeker.hasNext() && getStackPrefix().equals(peeker.peek())) {
			peeker.next();
			return parseStack(peeker);
		}
		
		Stack<Predicate<? super T>> stack = new Stack<Predicate<? super T>>();

		synchronized (getSelectorOperators()) {
			synchronized (getSelectors()) {
				
				while (peeker.hasNext()) {
					parseToken(stack, peeker);
				}
				
			}
		}
		
		return compress(stack);
	}
	
	private void parseToken(Stack<Predicate<? super T>> stack, Iterator<String> tokens) throws SyntaxException {
		
		if (!tokens.hasNext()) {
			throw new SyntaxException("Not enough tokens");
		}
		String token = tokens.next();
		
		for (SelectorOperator operator : getSelectorOperators()) {
			if (operator.matchesName(token.toLowerCase())) {
				parseToken(stack, tokens);
				operator.process(stack);
				return;
			}
		}
		
		Selector<? super T> tmp;
		for (Selector<? super T> selector : getSelectors()) {
			if ((tmp = selector.derive(token)) != null) {
				stack.push(tmp);
				return;
			}
		}
		
		throw new SyntaxException("Unknown token \"" + token + "\"");
	}
	
	public Predicate<? super T> parseStack(Iterator<String> tokens) throws SyntaxException {
		Stack<Predicate<? super T>> stack = new Stack<Predicate<? super T>>();
		
		Selector<? super T> tmp;
		String token;
		
		synchronized (getSelectorOperators()) {
			synchronized (getSelectors()) {
				
				tokenCycle:
				while (tokens.hasNext()) {
					token = tokens.next();
					
					for (SelectorOperator operator : getSelectorOperators()) {
						if (operator.matchesName(token.toLowerCase())) {
							operator.process(stack);
							continue tokenCycle;
						}
					}
					
					for (Selector<? super T> selector : getSelectors()) {
						if ((tmp = selector.derive(token)) != null) {
							stack.push(tmp);
							continue tokenCycle;
						}
					}
					
					throw new SyntaxException("Unknown token \"" + token + "\"");
					
				}
			}
		}
		
		return compress(stack);
	}
	
	private Predicate<? super T> compress(Stack<Predicate<? super T>> stack) throws SyntaxException {
		if (stack.isEmpty()) {
			throw new SyntaxException("Stack is empty");
		}
		
		if (stack.size() == 1) {
			return stack.pop();
		}
		
		return obj -> {
			for (Predicate<? super T> predicate : stack) {
				if (predicate.test(obj)) {
					return true;
				}
			}
			
			return false;
		};
	}

}
