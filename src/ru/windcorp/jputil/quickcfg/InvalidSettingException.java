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

import java.util.Deque;
import java.util.LinkedList;

public class InvalidSettingException extends QCFGException {

	private static final long serialVersionUID = 2828365534939572805L;
	
	private static final ThreadLocal<Deque<String>> NOW_PARSING = ThreadLocal.withInitial(LinkedList::new);
	
	public static void startParsing(String setting) {
		NOW_PARSING.get().push(setting);
	}
	
	public static String endParsing() {
		Deque<String> stack = NOW_PARSING.get();
		String result = stack.pop();
		if (stack.isEmpty()) {
			NOW_PARSING.remove();
		}
		return result;
	}
	
	private static String getPrefix() {
		return "Could not parse \"" + endParsing() + "\"";
	}

	public InvalidSettingException() {
		super(getPrefix());
	}

	public InvalidSettingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(getPrefix() + ": " + message, cause, enableSuppression, writableStackTrace);
	}

	public InvalidSettingException(String message, Throwable cause) {
		super(getPrefix() + ": " + message, cause);
	}

	public InvalidSettingException(String message) {
		super(getPrefix() + ": " + message);
	}

	public InvalidSettingException(Throwable cause) {
		super(getPrefix(), cause);
	}

}
