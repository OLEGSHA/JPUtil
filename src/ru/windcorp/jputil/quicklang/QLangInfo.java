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
package ru.windcorp.jputil.quicklang;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class QLangInfo {
	
	private static final List<QLangInfo> REGISTERED = Collections.synchronizedList(new LinkedList<QLangInfo>());
	
	public static QLangInfo getLangInfo(String internalName, String fullName) {
		QLangInfo result = getLangInfoOrNull(internalName);
		if (result != null) {
			return result;
		}
		
		result = new QLangInfo(internalName, fullName);
		REGISTERED.add(result);
		return result;
	}
	
	public static QLangInfo getLangInfo(String name) {
		return getLangInfo(name, name);
	}
	
	public static QLangInfo getLangInfoOrNull(String name) {
		for (QLangInfo l : REGISTERED) {
			if (l.getInternalName().equals(name)) {
				return l;
			}
		}
		
		return null;
	}
	
	public static List<QLangInfo> getRegistered() {
		return REGISTERED;
	}
	
	private final String internalName;
	private final String fullName;
	
	public QLangInfo(String internalName, String fullName) {
		this.internalName = internalName;
		this.fullName = fullName;
	}
	
	public QLangInfo(String name) {
		this(name, name);
	}

	public String getInternalName() {
		return internalName;
	}

	public String getFullName() {
		return fullName;
	}
	
	public QLang getLang() {
		return QLang.getInstance(getInternalName());
	}
	
	public QLang setCurrent() {
		QLang lang = getLang();
		QLang.setCurrent(lang);
		return lang;
	}
	
	@Override
	public String toString() {
		return getFullName();
	}
	
}
