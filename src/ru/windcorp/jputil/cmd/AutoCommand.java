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
/**
 * Copyright (C) 2019 OLEGSHA
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
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.jputil.cmd;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import ru.windcorp.jputil.chars.FancyCharacterIterator;
import ru.windcorp.jputil.chars.StringUtil;
import ru.windcorp.jputil.cmd.parsers.Parser;
import ru.windcorp.jputil.cmd.parsers.Parsers;

public class AutoCommand extends Command {

	private final Parser parser;
	
	private final Object object;
	private final Method method;
	private final Class<?>[] parameterTypes;

	public AutoCommand(
			String[] names,
			String displayedSyntax, String desc,
			Parser parser, Object obj, String method) {
		
		super(names, displayedSyntax, desc);
		Objects.requireNonNull(obj, "obj cannot be null");
		this.parser = parser;
		
		{
			List<Class<?>> parameterTypeList = new ArrayList<>();
			parameterTypeList.add(Invocation.class);
			parser.insertArgumentClasses(parameterTypeList::add);
			parameterTypes = parameterTypeList.toArray(new Class<?>[0]);
		}
		
		Class<?> clazz;
		if (obj instanceof Class<?>) {
			clazz = (Class<?>) obj;
			object = null;
		} else {
			clazz = obj.getClass();
			object = obj;
		}
		
		try {
			this.method = clazz.getMethod(method, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(
					"Method not found. Looking for method with signature \""
							+ constructMethodSignature(clazz, method) + "\"",
					e);
		}
		
		if (this.method.getReturnType() != Void.TYPE) {
			throw new IllegalArgumentException(
					"Method " + this.method + " is not void");
		}
		
		int modifiers = this.method.getModifiers();
		if (object == null && !Modifier.isStatic(modifiers)) {
			throw new IllegalArgumentException(
					"Method " + this.method + " is not static and no object provided");
		}
		
		this.method.setAccessible(true);
	}
	
	public AutoCommand(
			String[] names,
			String displayedSyntax, String desc,
			String syntax, Object obj, String method) {
		this(names, displayedSyntax, desc,
				Parsers.createParser(syntax),
				obj, method);
	}
	
	public AutoCommand(
			String name,
			String displayedSyntax, String desc,
			Parser syntax, Object obj, String method) {
		this(new String[] {name},
				displayedSyntax, desc,
				syntax, obj, method);
	}
	
	public AutoCommand(
			String name,
			String displayedSyntax, String desc,
			String syntax, Object obj, String method) {
		this(new String[] {name},
				displayedSyntax, desc,
				syntax, obj, method);
	}
	
	private String constructMethodSignature(Class<?> clazz, String method) {
		return (object == null ? "static" : "")
				+ " void "
				+ clazz.getCanonicalName()
				+ "."
				+ method
				+ "("
				+ StringUtil.supplierToString(
						i -> parameterTypes[i].getCanonicalName(),
						parameterTypes.length, ", ")
				+ ") throws CommandExceptions";
	}
	
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_BOXED = new HashMap<>();
	private static final Map<Class<?>, Object> PRIMITIVE_TO_NULL = new HashMap<>();
	
	static {
		for (Class<?> boxed : new Class<?>[] {
			Boolean.class, Byte.class, Short.class, Character.class,
			Integer.class, Long.class, Float.class, Double.class
		}) {
			try {
				PRIMITIVE_TO_BOXED.put((Class<?>) boxed.getField("TYPE").get(null), boxed);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		PRIMITIVE_TO_NULL.put(Boolean.TYPE,		Boolean.FALSE);
		PRIMITIVE_TO_NULL.put(Byte.TYPE,		Byte.valueOf((byte) 0));
		PRIMITIVE_TO_NULL.put(Short.TYPE,		Short.valueOf((short) 0));
		PRIMITIVE_TO_NULL.put(Integer.TYPE,		Integer.valueOf(0));
		PRIMITIVE_TO_NULL.put(Long.TYPE,		Long.valueOf(0));
		PRIMITIVE_TO_NULL.put(Float.TYPE,		Float.valueOf(Float.NaN));
		PRIMITIVE_TO_NULL.put(Double.TYPE,		Double.valueOf(Double.NaN));
		PRIMITIVE_TO_NULL.put(Character.TYPE,	Character.valueOf('\uFFFF'));
	}
	
	protected class ParameterFiller implements Consumer<Object> {
		
		private final Object[] params;
		private int index = 1;

		public ParameterFiller(Object[] params) {
			this.params = params;
		}
		
		@Override
		public void accept(Object obj) {
			Class<?> c = parameterTypes[index];
			if (obj != null) {
				if (c.isPrimitive()) {
					c = PRIMITIVE_TO_BOXED.get(c);
				}
				
				if (!c.isInstance(obj)) {
					throw new IllegalArgumentException("Expecting argument of type "
							+ c + ", received type " + obj.getClass()
							+ " (\"" + obj + "\") at index " + index);
				}
				params[index] = obj;
			} else if (!c.isPrimitive()) {
				params[index] = null;
			} else {
				params[index] = PRIMITIVE_TO_NULL.get(c);
			}
			index++;
		}

	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public void run(Invocation inv) throws CommandExceptions {
		CharacterIterator data = new FancyCharacterIterator(inv.getArgs());
		final Object[] params = new Object[parameterTypes.length];
		
		if (!parser.matches(data)) {
			data.first();
			Exception problem = parser.getProblem(data, inv).get();
			
			if (problem instanceof CommandSyntaxException) {
				throw (CommandSyntaxException) problem;
			}
			
			throw new CommandSyntaxException(inv, problem.getLocalizedMessage(), problem);
		}
		data.first();
		
		Consumer<Object> accepter = new ParameterFiller(params);
		parser.parse(data, accepter);
		params[0] = inv;
		
		try {
			this.method.invoke(this.object, params);
		} catch (IllegalAccessException e) {
			throw new CommandErrorException(inv, "Go figure", e);
		} catch (IllegalArgumentException e) {
			throw new CommandErrorException(inv, "Something went wrong in AutoCommand. Check custom parsers then AutoCommand code", e);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof CommandExceptions)
				throw (CommandExceptions) e.getCause();
			throw new CommandErrorException(inv, "Command \"" + this + "\" failed to execute", e);
		}
	}
	
	/**
	 * @return the parser
	 */
	public Parser getParser() {
		return parser;
	}
	
}
