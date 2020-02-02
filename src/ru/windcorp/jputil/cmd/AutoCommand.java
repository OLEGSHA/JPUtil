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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import ru.windcorp.jputil.PrimitiveUtil;
import ru.windcorp.jputil.chars.FancyCharacterIterator;
import ru.windcorp.jputil.chars.StringUtil;
import ru.windcorp.jputil.cmd.parsers.Parser;
import ru.windcorp.jputil.cmd.parsers.Parsers;
import ru.windcorp.jputil.cmd.parsers.SyntaxFormatter;
import ru.windcorp.jputil.functions.ThrowingBiConsumer;

public class AutoCommand extends Command {
	
	@FunctionalInterface
	public static interface Action extends ThrowingBiConsumer<Invocation, Object[], CommandExceptions> {
		// Alias
	}
	
	public static class AutoInvocation extends Invocation {
		
		private final Map<Parser, Object> approachData = new HashMap<>();
		
		public AutoInvocation(Invocation inv, Parser rootParser) {
			super(
					inv.getContext(),
					inv.getFullCommand(),
					inv.getRunner(),
					inv.getParent(),
					inv.getCurrent(),
					inv.getArgs()
			);
			
			rootParser.resetApproach(this);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.Invocation#getCurrent()
		 */
		@Override
		public AutoCommand getCurrent() {
			return (AutoCommand) super.getCurrent();
		}
		
		public <T> T getApproachData(Parser parser, Class<T> clazz) {
			return clazz.cast(approachData.get(parser));
		}
		
		public void setApproachData(Parser parser, Object data) {
			approachData.put(parser, data);
		}
		
	}

	private final Parser parser;
	
	private final Action action;
	private final Class<?>[] parameterTypes;

	public AutoCommand(
			String[] names,
			String syntax, String desc,
			Parser parser, Class<?>[] parameterTypes, Action action) {
		super(names, syntax, desc);
		this.parser = parser;
		this.parameterTypes = parameterTypes;
		this.action = action;
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
					c = PrimitiveUtil.getBoxedClass(c);
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
				params[index] = PrimitiveUtil.getPrimitiveNull(c);
			}
			index++;
		}

	}
	
	/**
	 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public void run(Invocation givenInv) throws CommandExceptions {
		AutoInvocation inv = new AutoInvocation(givenInv, parser);
		CharacterIterator data = new FancyCharacterIterator(inv.getArgs());
		final Object[] params = new Object[parameterTypes.length];
		
		// Find an appropriate approach or determine invalid syntax
		
		{
			boolean appropriateApproachFound = false;
			
			do {
				boolean matches = parser.matches(data, inv);
				data.first();
				
				if (matches) {
					appropriateApproachFound = true;
					break;
				}
			} while (parser.selectNextApproach(inv));
			
			if (!appropriateApproachFound) {
				parser.resetApproach(inv);
				Exception problem = parser.getProblem(data, inv).get();
				
				if (problem instanceof CommandSyntaxException) {
					throw (CommandSyntaxException) problem;
				}
				
				throw new CommandSyntaxException(inv, problem.getLocalizedMessage(), problem);
			}
		}
		
		// Parse and collect arguments
		
		Consumer<Object> accepter = new ParameterFiller(params);
		parser.insertParsed(data, inv, accepter);
		
		// Provide the invocation object
		params[0] = inv;
		
		// Run the action
		this.action.accept(inv, params);
	}
	
	/**
	 * @return the parser
	 */
	public Parser getParser() {
		return parser;
	}
	
	public static class AutoCommandBuilder {
		private final Object methodInst;
		private final String methodName;
		private Action action;
		
		private String[] names;
		private String syntax;
		private String description;
		
		private AutoCommandBuilder(Object methodInst, String methodName, Action action) {
			this.methodInst = methodInst;
			this.methodName = methodName;
			this.action = action;
		}
		
		public AutoCommandBuilder name(String... names) {
			this.names = names;
			return this;
		}
		
		public AutoCommandBuilder meta(String description, String syntax) {
			this.description = description;
			this.syntax = syntax;
			return this;
		}
		
		public AutoCommandBuilder desc(String description) {
			return meta(description, null);
		}
		
		public AutoCommand parser(Parser parser, String syntax) {
			Objects.requireNonNull(parser, "parser");
			if (syntax != null) {
				if (this.syntax != null) throw new IllegalArgumentException(
						"Syntax is set manually and overwrite is attempted with parser()");
				this.syntax = syntax;
			}
			
			if (names == null) {
				if (methodName == null) throw new IllegalArgumentException("Names are not set");
				this.names = new String[] { methodName };
			}
			if (this.syntax == null) throw new IllegalArgumentException("Syntax is not set");
			if (this.description == null) throw new IllegalArgumentException("Description is not set");
			
			Class<?>[] parameterTypes = new Class<?>[parser.getArgumentClasses().length + 1];
			parameterTypes[0] = Invocation.class;
			System.arraycopy(
					parser.getArgumentClasses(), 0,
					parameterTypes, 1,
					parser.getArgumentClasses().length);
			
			if (action == null) {
				assert methodInst != null && methodName != null : "Action nor method lookup arguments are set";
				findMethod(parser, parameterTypes);
			}
			
			return new AutoCommand(names, syntax, description, parser, parameterTypes, action);
		}
		
		public AutoCommand parser(Parser parser, SyntaxFormatter formatter) {
			return parser(parser, parser.toSyntax(formatter));
		}
		
		public AutoCommand parser(Parser parser) {
			return parser(parser, (String) null);
		}
		
		public AutoCommand parser(String syntax, SyntaxFormatter formatter) {
			return parser(createParser(syntax), formatter);
		}
		
		public AutoCommand parser(String syntax) {
			return parser(createParser(syntax));
		}
		
		private static Parser createParser(String syntax) {
			return Parsers.appendEndCheck(Parsers.createParser(syntax));
		}
		
		private static Action wrapReflection(Method method, Object inst) {
			return (inv, params) -> {
				try {
					method.invoke(inst, params);
				} catch (IllegalAccessException e) {
					throw new CommandErrorException(inv, "Go figure", e);
				} catch (IllegalArgumentException e) {
					throw new CommandErrorException(inv, "Something went wrong in AutoCommand. Check custom parsers then AutoCommand code", e);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof CommandExceptions)
						throw (CommandExceptions) e.getCause();
					throw new CommandErrorException(inv, "Command \"" + inv.getCurrent() + "\" failed to execute", e);
				}
			};
		}
		
		private void findMethod(Parser parser, Class<?>[] parameterTypes) {
			Class<?> clazz;
			Object inst;
			
			if (methodInst instanceof Class<?>) {
				clazz = (Class<?>) methodInst;
				inst = null;
			} else {
				clazz = methodInst.getClass();
				inst = methodInst;
			}
			
			Method method;
			
			try {
				method = clazz.getMethod(methodName, parameterTypes);
			} catch (NoSuchMethodException e) {
				throw new IllegalArgumentException(
						"Method not found. Looking for method with signature \""
								+ (inst == null ? "static" : "")
								+ " void "
								+ clazz.getCanonicalName()
								+ "."
								+ methodName
								+ "("
								+ StringUtil.supplierToString(
										i -> parameterTypes[i].getCanonicalName(),
										parameterTypes.length, ", ")
								+ ") throws CommandExceptions" + "\"",
						e);
			}
			
			if (method.getReturnType() != Void.TYPE)
				throw new IllegalArgumentException("Method " + method + " is not void");
			int modifiers = method.getModifiers();
			if (inst == null && !Modifier.isStatic(modifiers))
				throw new IllegalArgumentException(
						"Method " + method + " is not static and no object provided");
			method.setAccessible(true);
			action = wrapReflection(method, inst);
		}
	}
	
	public static AutoCommandBuilder forMethod(Object methodInst, String methodName) {
		return new AutoCommandBuilder(methodInst, Objects.requireNonNull(methodName, "methodName"), null);
	}
	
	public static AutoCommandBuilder forAction(Action action) {
		return new AutoCommandBuilder(null, null, Objects.requireNonNull(action, "action"));
	}
	
	public static AutoCommandBuilder forMethod(Method method, Object inst) {
		return new AutoCommandBuilder(null, null, AutoCommandBuilder.wrapReflection(
				Objects.requireNonNull(method, "method"), inst));
	}
	
	public static AutoCommandBuilder forMethod(Method staticMethod) {
		return forMethod(Objects.requireNonNull(staticMethod, "staticMethod"), (Object) null);
	}
	
	public static void regsiterDefaultParsers() {
		Parsers.registerDefaultCreators();
	}
	
}
