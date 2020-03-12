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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public abstract class UnixArgument<T> implements Comparable<UnixArgument<?>> {
	
	public static final int ARGUMENTS_NONE = 1;
	public static final int ARGUMENTS_ONE = 0;
	public static final int ARGUMENTS_ALL = 2;
	
	public static final Collection<Class<?>> ALLOWED_ARGUMENT_TYPES = Collections.unmodifiableCollection(Arrays.asList(
			String.class, String[].class,
			Byte.class, Short.class, Integer.class, Long.class,
			Float.class, Double.class,
			Character.class,
			Boolean.class
	));
	
	private final String name;
	private final Character letter;
	private final String description;
	
	private final Class<T> argumentType;
	private final boolean isRequired;
	private final boolean isArgumentRequired;
	private final boolean isSingleuse;
	
	private boolean hasRun = false;

	public UnixArgument(
			String name, Character letter,
			String description,
			Class<T> argumentType,
			boolean isRequired, boolean isArgumentRequired, boolean isSingleuse) {
		
		this.name = name;
		this.letter = letter;
		
		if (getName() == null && getLetter() == null) {
			throw new IllegalArgumentException("Both name and letter are null");
		}
		
		this.description = description;
		
		this.argumentType = argumentType;
		this.isRequired = isRequired;
		this.isArgumentRequired = isArgumentRequired;
		this.isSingleuse = isSingleuse;
		
		if (argumentType != null && !ALLOWED_ARGUMENT_TYPES.contains(getArgumentType())) {
			throw new IllegalArgumentException("Unknown argument type " + getArgumentType());
		}
		
	}
	
	public String getName() {
		return name;
	}
	
	public Character getLetter() {
		return letter;
	}
	
	private char getCharForComparison() {
		if (getLetter() == null) {
			return getName().charAt(0);
		}
		
		return Character.toLowerCase(getLetter());
	}
	
	public String getDescritpion() {
		return description;
	}
	
	public boolean canRun() {
		return !hasRun() || !isSingleuse();
	}
	
	public synchronized boolean run(T arg) throws InvocationTargetException, UnixArgumentInvalidSyntaxException {
		try {
			setHasRun(true);
			return runImpl(arg);
		} catch (UnixArgumentInvalidSyntaxException | InvocationTargetException e) {
			throw e;
		} catch (Exception e) {
			throw new InvocationTargetException(e, "Unhandled exception in argument " + this);
		}
	}
	
	protected abstract boolean runImpl(T arg) throws UnixArgumentInvalidSyntaxException, InvocationTargetException;
	
	public synchronized void reset() {
		setHasRun(false);
	}
	
	@Override
	public int compareTo(UnixArgument<?> arg0) {
		if (arg0 == this) return 0;
		
		if (getCharForComparison() > arg0.getCharForComparison()) {
			return 1;
		}
		
		return -1;
	}
	
	// Just to clarify: no equal arguments exist
	@Override public boolean equals(Object obj) { return obj == this; }
	@Override public int     hashCode()         { return super.hashCode(); }
	
	@Override
	public String toString() {
		if (getLetter() == null) {
			return "--" + getName();
		} else {
			if (getName() == null) {
				return "-" + getLetter();
			}
			
			return "--" + getName() + " (-" + getLetter() + ")";
		}
	}

	public Class<T> getArgumentType() {
		return argumentType;
	}

	public boolean isRequired() {
		return isRequired;
	}

	public boolean isArgumentRequired() {
		return isArgumentRequired;
	}

	public boolean hasRun() {
		return hasRun;
	}

	protected void setHasRun(boolean hasRun) {
		this.hasRun = hasRun;
	}
	
	public boolean isSingleuse() {
		return isSingleuse;
	}
	
	@SuppressWarnings("unchecked")
	boolean parseInputAndRun(Iterator<String> input)
			throws InvocationTargetException, UnixArgumentInvalidSyntaxException
	{
		if (getArgumentType() == null) {
			return run(null);
		}
		
		if (getArgumentType() == String[].class) {
			List<String> list = new ArrayList<>();
			while (input.hasNext()) {
				list.add(input.next());
			}
			
			// Since Class<T> is String[].class then T is String[],
			// run(T) expects String[] as argument -- this is safe
			return run((T) list.toArray(new String[0]));
		}
		
		if (input.hasNext()) {
			return run(parseSingleInput(input.next()));
		} else {
			if (isArgumentRequired()) {
				throw new UnixArgumentInvalidSyntaxException(this + " is missing required arguments", this);
			} else {
				return run(null);
			}
		}
	}

	// SonarLint: Cognitive Complexity of methods should not be too high (java:S3776) (28 > 15)
	//   This is a very simple method that just happens to contain many if's
	@SuppressWarnings({"unchecked", "squid:S3776"})
	
	private T parseSingleInput(String input) throws UnixArgumentInvalidSyntaxException {
		if (getArgumentType() == String.class) {
			return (T) input;
		} else if (getArgumentType() == Byte.class) {
			try {
				return (T) Byte.decode(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a byte", e, this);
			}
		} else if (getArgumentType() == Short.class) {
			try {
				return (T) Short.decode(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a short", e, this);
			}
		} else if (getArgumentType() == Integer.class) {
			try {
				return (T) Integer.decode(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a integer", e, this);
			}
		} else if (getArgumentType() == Long.class) {
			try {
				return (T) Long.decode(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a long", e, this);
			}
		} else if (getArgumentType() == Float.class) {
			try {
				return (T) Float.valueOf(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a float", e, this);
			}
		} else if (getArgumentType() == Double.class) {
			try {
				return (T) Double.valueOf(input);
			} catch (NumberFormatException e) {
				throw new UnixArgumentInvalidSyntaxException("Could not parse " + input + " as a double", e, this);
			}
		} else if (getArgumentType() == Character.class) {
			if (input.length() != 1) {
				throw new UnixArgumentInvalidSyntaxException(
						"Could not parse " + input + " as a single character", this
				);
			}
			
			return (T) (Character) input.charAt(0);
		} else if (getArgumentType() == Boolean.class) {
			if (input.equalsIgnoreCase("true")) {
				return (T) Boolean.TRUE;
			} else if (input.equalsIgnoreCase("false")) {
				return (T) Boolean.FALSE;
			} else {
				throw new UnixArgumentInvalidSyntaxException(
						"Could not parse " + input + " as a boolean value (\"true\" or \"false\" expected)", this
				);
			}
		} else {
			return null;
		}
	}

}
