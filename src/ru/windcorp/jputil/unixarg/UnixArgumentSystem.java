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
package ru.windcorp.jputil.unixarg;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import ru.windcorp.jputil.Version;
import ru.windcorp.jputil.iterators.ArrayIterator;
import ru.windcorp.jputil.textui.TUITable;

// SonarLint: Standard outputs should not be used directly to log anything (java:S106)
//   This is intended for simple console applications that use System.out directly 
@SuppressWarnings("squid:S106")

public class UnixArgumentSystem {
	
	@FunctionalInterface
	public static interface UnknownArgumentPolicy {
		public boolean onUnknownArgument(String name);
	}
	
	public static class UnknownArgumentPolicies {
		private UnknownArgumentPolicies() {}
		
		public static final UnknownArgumentPolicy
		IGNORE = name -> false;
		
		public static final UnknownArgumentPolicy
		WARN = name -> {
				System.out.println("Unknown argument " + name);
				return false;
		};
		
		public static final UnknownArgumentPolicy
		TERMINATE = name -> {
				System.out.println("Unknown argument " + name);
				return true;
		};
	}
	
	@FunctionalInterface
	public static interface InvalidSyntaxPolicy {
		public boolean onInvalidSyntax(UnixArgument<?> arg, String description);
	}
	
	public static class InvalidSyntaxPolicies {
		private InvalidSyntaxPolicies() {}
		
		public static final InvalidSyntaxPolicy
		IGNORE = (arg, description) -> false;

		public static final InvalidSyntaxPolicy
		WARN = (arg, description) -> {
				System.out.println("Invalid use of argument " + arg + ": " + description);
				return false;
		};

		public static final InvalidSyntaxPolicy
		TERMINATE = (arg, description) -> {
				System.out.println("Invalid use of argument " + arg + ": " + description);
				return true;
		};
	}
	
	private final SortedSet<UnixArgument<?>> arguments = Collections.synchronizedSortedSet(new TreeSet<>());
	
	private String name;
	private String description;
	private String usage;
	private Version version;
	
	public UnixArgumentSystem(String name, String description, String usage, Version version) {
		this.name = name;
		this.description = description;
		this.usage = usage;
		this.version = version;
	}

	public SortedSet<UnixArgument<?>> getArguments() {
		return arguments;
	}
	
	public synchronized void addArgument(UnixArgument<?> argument) {
		getArguments().add(argument);
	}

	public synchronized void removeArgument(UnixArgument<?> argument) {
		getArguments().remove(argument);
	}
	
	/**
	 * Parses and executes arguments.
	 * @param args the arguments to parse
	 * @return true if the application should terminate
	 * @throws InvocationTargetException 
	 */
	public boolean run(
			String[] input,
			UnknownArgumentPolicy unknownArgumentPolicy,
			InvalidSyntaxPolicy invalidSyntaxPolicy,
			boolean skipInputWithoutDashes
	)
			throws InvocationTargetException
	{
		return run(new ArrayIterator<>(input), unknownArgumentPolicy, invalidSyntaxPolicy, skipInputWithoutDashes);
	}
	
	/**
	 * Parses and executes arguments.
	 * @param args the arguments to parse
	 * @return true if the application should terminate
	 * @throws InvocationTargetException 
	 */
	public synchronized boolean run(
			Iterator<String> input,
			UnknownArgumentPolicy unknownArgumentPolicy,
			InvalidSyntaxPolicy invalidSyntaxPolicy,
			boolean skipInputWithoutDashes
	)
			throws InvocationTargetException
	{
		
		if (!input.hasNext()) {
			if (checkRequiredArguments()) return true;
		}
		
		do {
			if (tryToParseArgument(input, unknownArgumentPolicy, invalidSyntaxPolicy, skipInputWithoutDashes)) {
				return true;
			}
		} while (input.hasNext());
		
		if (checkRequiredArguments()) return true;
		return false;
	}
	
	private boolean tryToParseArgument(
			Iterator<String> input,
			UnknownArgumentPolicy unknownArgumentPolicy,
			InvalidSyntaxPolicy invalidSyntaxPolicy,
			boolean skipInputWithoutDashes
	)
			throws InvocationTargetException
	{
		try {
			if (parseArgument(input.next(), input, skipInputWithoutDashes)) {
				return true;
			}
		} catch (UnixArgumentInvalidSyntaxException e) {
			if (invalidSyntaxPolicy.onInvalidSyntax(e.getArgument(), e.getMessage())) {
				return true;
			}
		} catch (UnixArgumentUnknownException e) {
			if (unknownArgumentPolicy.onUnknownArgument(e.getMessage())) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean parseArgument(
			String arg, Iterator<String> input,
			boolean skipInputWithoutDashes
	)
			throws InvocationTargetException,
			       UnixArgumentInvalidSyntaxException,
			       UnixArgumentUnknownException
	{
		if (arg.equals("--help") || arg.equals("-h")) {
			showHelp();
			return true;
		}
		
		if (arg.equals("--version") || arg.equals("-v")) {
			showVersion();
			return true;
		}
			
		if (arg.startsWith("--")) {
			if (parseLongArgument(arg.substring("--".length()), input)) {
				return true;
			}
		} else if (arg.startsWith("-")) {
			for (char c : arg.substring("-".length()).toCharArray()) {
				if (parseShortArgument(c, input)) {
					return true;
				}
			}
		} else if (skipInputWithoutDashes) {
			// continue
		} else {
			throw new UnixArgumentUnknownException(arg);
		}
		
		return false;
	}

	private boolean parseShortArgument(char c, Iterator<String> input)
			throws InvocationTargetException, UnixArgumentInvalidSyntaxException, UnixArgumentUnknownException {
		synchronized (getArguments()) {
			for (UnixArgument<?> a : getArguments()) {
				if (a.getLetter() != null && a.getLetter().charValue() == c) {
					
					if (a.canRun()) {
						return a.parseInputAndRun(input);
					} else {
						throw new UnixArgumentInvalidSyntaxException("Invalid use of argument " + a, a);
					}
					
				}
			}
		}
		
		throw new UnixArgumentUnknownException("-" + c);
	}

	private boolean parseLongArgument(String name, Iterator<String> input)
			throws InvocationTargetException, UnixArgumentInvalidSyntaxException, UnixArgumentUnknownException {
		synchronized (getArguments()) {
			for (UnixArgument<?> a : getArguments()) {
				if (name.equals(a.getName())) { // a.getName() can be null, then evaluates to false
					
					if (a.canRun()) {
						return a.parseInputAndRun(input);
					} else {
						throw new UnixArgumentInvalidSyntaxException("Invalid use of argument " + a, a);
					}
					
				}
			}
		}
		
		throw new UnixArgumentUnknownException("--" + name);
	}

	private boolean checkRequiredArguments() {
		boolean argumentsMissing = false;
		
		synchronized (getArguments()) {
			for (UnixArgument<?> a : getArguments()) {
				if (a.isRequired() && !a.hasRun()) {
					argumentsMissing = true;
					break;
				}
			}
		}
		
		if (argumentsMissing) {
			System.out.println("One or more required arguments are missing, the program will terminate");
		}
		
		return argumentsMissing;
	}

	private void showHelp() {
		System.out.println(getName());
		System.out.println(getDescription());
		if (getUsage() != null) {
			System.out.println("Usage: " + getUsage());
		}
		
		System.out.println("Options:");
		
		TUITable table = new TUITable("", "", "").setDrawGrid(false);
		
		for (UnixArgument<?> a : getArguments()) {
			table.addRow(a.getLetter() == null ? "" : "-" + a.getLetter(),
					a.getName() == null ? "" : "--" + a.getName(),
					a.getDescritpion());
		}
		
		System.out.println(table);
	}
	
	private void showVersion() {
		System.out.println(getName() + " version " + getVersion());
	}
	
	public void reset() {
		synchronized (getArguments()) {
			for (UnixArgument<?> a : getArguments()) {
				a.reset();
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}
	
}
