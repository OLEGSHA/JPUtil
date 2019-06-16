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
package ru.windcorp.jputil;

public class SafeNumberParser {
	
	public static byte parseByte(String declar, byte def) {
		try {
			return Byte.parseByte(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static short parseShort(String declar, short def) {
		try {
			return Short.parseShort(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static int parseInt(String declar, int def) {
		try {
			return Integer.parseInt(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static long parseLong(String declar, long def) {
		try {
			return Long.parseLong(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static float parseFloat(String declar, float def) {
		try {
			return Float.parseFloat(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	public static double parseDouble(String declar, double def) {
		try {
			return Double.parseDouble(declar);
		} catch (NumberFormatException e) {
			return def;
		}
	}

}
