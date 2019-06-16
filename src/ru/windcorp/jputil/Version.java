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

import ru.windcorp.jputil.chars.StringUtil;

public class Version implements Comparable<Version> {
	
	private final int[] subVersions;
	
	public Version(int... subVersions) {
		if (subVersions == null ||
				subVersions.length < 1) {
			throw new IllegalArgumentException();
		}
		
		this.subVersions = subVersions;
	}

	public Version(String string) {
		String[] parts = StringUtil.split(string, '.');
		this.subVersions = new int[parts.length];
		
		for (int i = 0; i < this.subVersions.length; ++i) {
			this.subVersions[i] = Integer.parseInt(parts[i]);
		}
	}
	
	public int[] getSubVersions() {
		return subVersions;
	}
	
	public int getSubVersion(int depth) {
		return getSubVersions().length > depth ? getSubVersions()[depth] : 0;
	}
	
	public int getMajor() {
		return getSubVersion(0);
	}
	
	public int getMinor() {
		return getSubVersion(1);
	}
	
	public int getBuild() {
		return getSubVersion(Math.max(getSubVersions().length - 1, 2));
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(Integer.toString(getMajor()));
		for (int i = 1; i < getSubVersions().length; ++i) {
			sb.append('.');
			sb.append(getSubVersions()[i]);
		}
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 0;
		for (int i = 0; i < subVersions.length; ++i) result += subVersions[i] * prime;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return compareTo((Version) obj) == 0;
	}

	@Override
	public int compareTo(Version arg0) {
		int diff;
		for (int i = 0; i < Math.max(getSubVersions().length, arg0.getSubVersions().length); ++i) {
			diff = getSubVersion(i) - arg0.getSubVersion(i); 
			if (diff != 0) {
				return diff;
			}
		}
		
		return 0;
	}

}
