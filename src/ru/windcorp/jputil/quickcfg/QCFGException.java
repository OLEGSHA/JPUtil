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
package ru.windcorp.jputil.quickcfg;

public class QCFGException extends Exception {
	
	private static final long serialVersionUID = 9057100484476874366L;

	public QCFGException() {
		super();
	}

	public QCFGException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public QCFGException(String message, Throwable cause) {
		super(message, cause);
	}

	public QCFGException(String message) {
		super(message);
	}
	
	public QCFGException(int lineNumber, String message) {
		super("[Line " + lineNumber + "] " + message);
	}

	public QCFGException(Throwable cause) {
		super(cause);
	}
	
}
