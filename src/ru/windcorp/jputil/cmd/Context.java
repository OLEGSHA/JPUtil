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

import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Context {
	
	private final CommandRegistry globals = new CommandRegistry("__GLOBALS", "Global commands") {
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRegistry#contains(java.lang.String)
		 */
		@Override
		public synchronized boolean contains(String name) {
			name = name.toLowerCase();
			
			Command helpCommand = getHelpCommand();
			if (helpCommand != null) {
				for (String helpName : helpCommand.getNames()) {
					if (helpName.toLowerCase().equals(name)) {
						return true;
					}
				}
			}
			
			return super.contains(name);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRegistry#runCommand(ru.windcorp.jputil.cmd.CommandRegistry, ru.windcorp.jputil.cmd.Invocation, java.lang.String, java.lang.String)
		 */
		@Override
		protected synchronized void runCommand(CommandRegistry parent, Invocation inv, String name, String args)
				throws CommandExceptions {
			name = name.toLowerCase();
			
			Command helpCommand = getHelpCommand();
			if (helpCommand != null) {
				for (String helpName : helpCommand.getNames()) {
					if (helpName.toLowerCase().equals(name)) {
						helpCommand.run(inv.nextCall(parent, helpCommand, args));
					}
				}
			}
			
			super.runCommand(parent, inv, name, args);
		}
	};
	
	private Command helpCommand;
	private final Map<Class<? extends CommandExceptions>, Consumer<? extends CommandExceptions>> exceptionHandlers = new WeakHashMap<>();
	private BiConsumer<Invocation, Exception> fallbackExceptionHandler;
	private Function<String, String> translator = null;

	public CommandRegistry getGlobals() {
		return globals;
	}
	
	@SuppressWarnings("unchecked")
	public void handle(CommandExceptions exception) {
		Class<? extends CommandExceptions> mostSpecificClass = null;
		Consumer<? extends CommandExceptions> mostSpecificHandler = null;
		
		synchronized (exceptionHandlers) {
			for (Entry<Class<? extends CommandExceptions>, Consumer<? extends CommandExceptions>> entry
					: exceptionHandlers.entrySet()) {
				
				if (entry.getKey().equals(exception.getClass())) {
					mostSpecificHandler = entry.getValue();
					break;
				}
				
				if (entry.getKey().isAssignableFrom(exception.getClass())) {
					if (mostSpecificClass == null || entry.getKey().isAssignableFrom(mostSpecificClass)) {
						mostSpecificClass = entry.getKey();
						mostSpecificHandler = entry.getValue();
					}
				}
			}
		}
			
		if (mostSpecificHandler == null) {
			fallbackExceptionHandler.accept(exception.getInvocation(), exception);
			return;
		}
		
		((Consumer<CommandExceptions>) mostSpecificHandler).accept(exception);
	}
	
	/**
	 * @return the fallbackExceptionHandler
	 */
	public BiConsumer<Invocation, Exception> getFallbackExceptionHandler() {
		return fallbackExceptionHandler;
	}
	
	/**
	 * @param fallbackExceptionHandler the fallbackExceptionHandler to set
	 */
	public void setFallbackExceptionHandler(BiConsumer<Invocation, Exception> fallbackExceptionHandler) {
		this.fallbackExceptionHandler = fallbackExceptionHandler;
	}
	
	public synchronized <E extends CommandExceptions> void addExceptionHandler(Class<E> clazz, Consumer<E> handler) {
		exceptionHandlers.put(clazz, handler);
	}
	
	public synchronized void addDefaultExceptionHandlers(String fallbackComplaint, String cmdNotFoundComplaint) {
		setFallbackExceptionHandler((Invocation i, Exception e) -> {
			CommandRunner runner = i.getRunner();
			runner.complain(fallbackComplaint, i.getFullCommand());
			runner.reportException(e);
		});
		
		addExceptionHandler(
				CommandNotFoundException.class, e -> 
				e.getRunner().complain(cmdNotFoundComplaint, e.getMessage(), e.getFullCommand())
		);
		
		addExceptionHandler(
				Complaint.class, e ->
				e.getRunner().complain(e.getLocalizedMessage())
		);
	}

	public Function<String, String> getTranslator() {
		return translator;
	}

	/**
	 * Sets the translator function for this context. Function should return <code>null</code> for unknown keys.
	 * <p>
	 * <b>Currently known keys</b>
	 * <table border="1">
	 * <tr>
	 * <th align="center"><code>key</code></th>
	 * <th align="center"><code>def</code></th>
	 * <th align="center">Description</th>
	 * <th align="center"><code>args</code></th>
	 * </tr>
	 * 
	 * <tr><td><code>auto.generic.argNotFound</code></td>
	 * <td><code>Argument %1$s not found</code></td>
	 * <td>A <code>Parser</code> failed to find itself</td>
	 * <td>1: argument name (Parser ID)</td></tr>
	 * 
	 * <tr><td><code>auto.literal.doesNotMatch</code></td>
	 * <td><code>\"%2$s\" expected, \"%1$s\" encountered</code></td>
	 * <td>Argument did not match a <code>ParserLiteral</code></td>
	 * <td>1: argument, 2: template</td></tr>
	 * 
	 * <tr><td><code>auto.int.notInt</code></td>
	 * <td><code>\"%1$s\" is not an int</code></td>
	 * <td><code>ParserInt</code> failed to parse int</td>
	 * <td>1: input</td></tr>
	 * 
	 * <tr><td><code>auto.end.excessive</code></td>
	 * <td><code>Excessive arguments \"%1$s\"</code></td>
	 * <td><code>ParserEnd</code> encountered arguments</td>
	 * <td>1: arguments</td></tr>
	 * 
	 * <tr><td><code>help.list.header</code></td>
	 * <td><code>Command %1$s has %2$d subcmd:</code></td>
	 * <td>Help header when listing all subcmds</td>
	 * <td>1: command name, 2: (int) subcmd count</td></tr>
	 * 
	 * <tr><td><code>help.list.header.empty</code></td>
	 * <td><code>Command %1$s does not have available subcommands</code></td>
	 * <td>Help header when listing all subcmds but none apply</td>
	 * <td>1: command name</td></tr>
	 * 
	 * <tr><td><code>help.search.header</code></td>
	 * <td><code>Search matched %2$d subcommands:</code></td>
	 * <td>Help header when searching for subcmds</td>
	 * <td>1: command name, 2: (int) subcmd count</td></tr>
	 * 
	 * <tr><td><code>help.search.header.empty</code></td>
	 * <td><code>Command %1$s does not have matching subcommands</code></td>
	 * <td>Help header when searching for subcmds but none match</td>
	 * <td>1: command name</td></tr>
	 * 
	 * <tr><td><code>help.entry</code></td>
	 * <td><code>- %1$s %2$s - %3$s</code></td>
	 * <td>Help subcmd entry</td>
	 * <td>1: subcmd name, 2: subcmd syntax, 3: subcmd description, 4: (int) index</td></tr>
	 * </table>
	 * @param translator the translator function or <code>null</code>
	 */
	public void setTranslator(Function<String, String> translator) {
		this.translator = translator;
	}
	
	public String translate(String key, String def) {
		if (getTranslator() == null) {
			return def;
		}
		
		String result = getTranslator().apply(key);
		
		return result == null ? def : result;
	}
	
	public String translate(String key, String def, Object... args) {
		return String.format(translate(key, def), args);
	}

	public Command getHelpCommand() {
		return helpCommand;
	}

	public void setHelpCommand(Command helpCommand) {
		this.helpCommand = helpCommand;
	}

}
