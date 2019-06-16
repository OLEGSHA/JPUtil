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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import ru.windcorp.jputil.chars.EscapeException;
import ru.windcorp.jputil.chars.Escaper;
import ru.windcorp.jputil.chars.StringUtil;

public class QLang {
	
	private static QLang current = null;
	private static Function<String, InputStream> langSupplier = null;
	
	private static final Map<String, QLang> LANGS = new HashMap<>();
	
	private final Map<String, String> map = new HashMap<>();
	private final String name;
	private static final Escaper ESCAPER = Escaper.JAVA;

	public synchronized static QLang getInstance(String name) {
		QLang result = LANGS.get(name);
		
		if (result != null) return result;
		if (getLangSupplier() == null) return null;
		
		try (InputStream is = getLangSupplier().apply(name)) {
			result = create(is, name);
			LANGS.put(name, result);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (EscapeException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected static QLang create(InputStream input, String name) throws IOException, EscapeException {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(name, "name");
		
		QLang lang = new QLang(name);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		String line;
		String[] parts;
		
		while ((line = reader.readLine()) != null) {
			if (!line.startsWith("#")) {
				parts = StringUtil.split(line, '=', 2);
				
				if (parts[1] == null) {
					lang.getMap().put(parts[0], "null");
				} else {
					lang.getMap().put(
							parts[0],
							new String(ESCAPER.unescape(parts[1])));
				}
			}
		}
		
		return lang;
	}
	
	public static QLang getCurrent() {
		return current;
	}

	public static void setCurrent(QLang current) {
		QLang.current = current;
	}

	public static Function<String, InputStream> getLangSupplier() {
		return langSupplier;
	}

	public static void setLangSupplier(Function<String, InputStream> langSupplier) {
		QLang.langSupplier = langSupplier;
	}

	public QLang(String name) {
		this.name = name;
	}

	protected Map<String, String> getMap() {
		return map;
	}
	
	public String getName() {
		return name;
	}
	
	public String instGet(String key) {
		return instGet(key, key);
	}
	
	public String instGet(String key, String def) {
		return getMap().getOrDefault(key, def);
	}
	
	public String instGetf(String key, Object... args) {
		return String.format(instGet(key), args);
	}
	
	public String instGetfd(String key, String def, Object... args) {
		return String.format(instGet(key, def), args);
	}
	
	public static String get(String key) {
		return getCurrent() == null ? key : getCurrent().instGet(key);
	}
	
	public static String get(String key, String def) {
		return getCurrent() == null ? def : getCurrent().instGet(key, def);
	}
	
	public static String getf(String key, Object... args) {
		return getCurrent() == null ? String.format(key, args) : getCurrent().instGetf(key, args);
	}
	
	public static String getfd(String key, String def, Object... args) {
		return getCurrent() == null ? String.format(def, args) : getCurrent().instGetfd(key, def, args);
	}

}