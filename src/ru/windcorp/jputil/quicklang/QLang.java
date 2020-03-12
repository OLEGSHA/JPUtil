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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import ru.windcorp.jputil.chars.CharPredicate;
import ru.windcorp.jputil.chars.EscapeException;
import ru.windcorp.jputil.chars.Escaper;
import ru.windcorp.jputil.chars.UncheckedEscapeException;
import ru.windcorp.jputil.chars.reader.CharReader;
import ru.windcorp.jputil.chars.reader.CharReaders;

public class QLang {
	
	private static QLang current = null;
	private static Function<String, InputStream> langSupplier = null;
	
	private static final Map<String, QLang> LANGS = new HashMap<>();
	
	private final Map<String, String> map = new HashMap<>();
	private final String name;
	private static final Escaper ESCAPER = Escaper.JAVA;

	public static synchronized QLang getInstance(String name) {
		QLang result = LANGS.get(name);
		
		if (result != null) return result;
		if (getLangSupplier() == null) return null;
		
		try (InputStream is = getLangSupplier().apply(name)) {
			result = create(is, name);
			LANGS.put(name, result);
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (EscapeException e) {
			throw new UncheckedEscapeException(e);
		}
	}
	
	protected static QLang create(InputStream input, String name) throws IOException, EscapeException {
		Objects.requireNonNull(input, "input");
		Objects.requireNonNull(name, "name");
		
		QLang lang = new QLang(name);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
		CharReader in = CharReaders.wrap(reader);
		
		while (!in.isEnd()) {
			if (in.hasErrored()) throw in.getLastException();
			
			if (in.current() == '#') {
				in.skipLine();
			} else {
				in.skipWhitespace();
				String key = new String(in.readUntil(CharPredicate.forChar('=')));
				
				if (in.current() != '=') {
					lang.getMap().put(key, "null");
				} else {
					in.mark();
					int length = in.skipWhitespace();
					in.reset();
					lang.getMap().put(key, new String(ESCAPER.unescape(in, length)));
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
