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
package ru.windcorp.jputil.textui;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ru.windcorp.jputil.chars.StringUtil;

public class TUITable {
	
	private Object[] headers;
	private final List<Object[]> data = Collections.synchronizedList(new LinkedList<Object[]>());
	
	private boolean drawGrid = true;

	public TUITable(Object... headers) {
		this.headers = headers;
	}
	
	public Object[] getHeaders() {
		return headers;
	}
	
	protected void setHeaders(Object[] headers) {
		this.headers = headers;
	}
	
	public synchronized void addHeaders(Object... headers) {
		int oldLength = getHeaders().length;
		resizeTable(oldLength + headers.length);
		System.arraycopy(headers, 0, getHeaders(), oldLength, headers.length);
	}
	
	protected synchronized void resizeTable(int columns) {
		if (columns < 0) {
			throw new IllegalArgumentException("Negative new columns (columns: " + columns + ")");
		}
		
		setHeaders(resize(getHeaders(), columns));
		
		for (int row = 0; row < getRows(); ++row) {
			getData().set(row, resize(getData().get(row), columns));
		}
	}
	
	protected static Object[] resize(Object[] src, int newLength) {
		Object[] result = new Object[newLength];
		System.arraycopy(src, 0, result, 0, Math.min(newLength, src.length));
		
		if (newLength > src.length) {
			Arrays.fill(result, src.length, newLength, "");
		}
		
		return result;
	}
	
	public int getRows() {
		return getData().size();
	}
	
	public int getColumns() {
		return getHeaders().length;
	}
	
	public List<Object[]> getData() {
		return data;
	}
	
	public synchronized void addRow(Object... data) {
		if (data.length != getColumns()) {
			resize(data, getColumns());
		}
		
		getData().add(data);
	}
	
	public synchronized void put(int column, int row, Object obj) {
		if (row < 0) {
			throw new IllegalArgumentException("Row cannot be negative (given " + column + ")");
		}
		
		if (column < 0 || column >= getColumns()) {
			throw new IllegalArgumentException("Column is illegal: given " + column + "; present: " + row);
		}
		
		while (getRows() <= row) {
			getData().add(new Object[getRows()]);
		}
		
		getData().get(row)[column] = obj;
	}
	
	@Override
	public synchronized String toString() {
		synchronized (getData()) {
			if (getColumns() == 0) return "";
			
			String[] headers = new String[getColumns()];
			String[][] data = new String[getRows()][getColumns()];
			int[] widths = new int[getColumns()];
			
			boolean headersExist = processHeaders(headers, widths);
			
			prepareData(data, widths);
			
			StringBuilder sb = new StringBuilder();
			
			if (headersExist) {
				writeHeaders(sb, headers, widths);
			}
			
			writeData(sb, data, widths, headersExist);
			
			return sb.toString();
		}
	}
	
	private boolean processHeaders(String[] headers, int[] widths) {
		boolean headersExist = false;
		
		for (int i = 0; i < getColumns(); ++i) {
			headers[i] = String.valueOf(getHeaders()[i]);
			widths[i] = headers[i].length();
			
			if (widths[i] != 0) {
				headersExist = true;
			}
		}
		
		return headersExist;
	}

	private void prepareData(String[][] data, int[] widths) {
		for (int row = 0; row < getRows(); ++row) {
			Object[] rowObj = getData().get(row);
			
			for (int column = 0; column < getColumns(); ++column) {
				data[row][column] = String.valueOf(rowObj[column]);
				widths[column] = Math.max(widths[column], data[row][column].length());
			}
		}
	}

	private void writeHeaders(StringBuilder sb, String[] headers, int[] widths) {
		sb.append(StringUtil.padToLeft(headers[0], widths[0]));
		
		for (int column = 1; column < getColumns(); ++column) {
			if (getDrawGrid()) {
				sb.append(" | ");
			} else {
				sb.append("  ");
			}
			sb.append(StringUtil.padToLeft(headers[column], widths[column]));
		}

		if (getDrawGrid()) {
			sb.append('\n');
			sb.append(StringUtil.sequence('-', widths[0]));
		
			for (int column = 1; column < getColumns(); ++column) {
				sb.append("-+-");
				sb.append(StringUtil.sequence('-', widths[column]));
			}
		}
	}

	private void writeData(StringBuilder sb, String[][] data, int[] widths, boolean headersExist) {
		for (int row = 0; row < data.length; ++row) {
			if (row != 0 || headersExist) sb.append('\n');
			
			for (int column = 0; column < data[row].length; ++column) {
				sb.append(StringUtil.padToLeft(data[row][column], widths[column]));
				
				if (column != data[0].length - 1) {
					if (getDrawGrid()) {
						sb.append(" | ");
					} else {
						sb.append("  ");
					}
				}
			}
		}
	}

	public boolean getDrawGrid() {
		return drawGrid;
	}

	public TUITable setDrawGrid(boolean drawGrid) {
		this.drawGrid = drawGrid;
		return this;
	}

}
