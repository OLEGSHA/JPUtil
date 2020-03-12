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

public class UnixArgumentInvalidSyntaxException extends Exception {

	private static final long serialVersionUID = 5689716525982612158L;
	
	// SonarLint: Fields in a "Serializable" class should either be transient or serializable (java:S1948)
	//   Serialization does not make sense in this context. Hope this does not change.
	@SuppressWarnings("squid:S1948")
	
	private final UnixArgument<?> argument;

	public UnixArgumentInvalidSyntaxException(String description, Throwable cause, UnixArgument<?> argument) {
		super(description, cause);
		this.argument = argument;
	}
	
	public UnixArgumentInvalidSyntaxException(String description, UnixArgument<?> argument) {
		this(description, null, argument);
	}
	
	public UnixArgumentInvalidSyntaxException(String description, Throwable cause, UnixArguments.Handle<?> handle) {
		this(description, cause, handle.getArgument());
	}
	
	public UnixArgumentInvalidSyntaxException(String description, UnixArguments.Handle<?> handle) {
		this(description, handle.getArgument());
	}

	public UnixArgument<?> getArgument() {
		return argument;
	}

}
