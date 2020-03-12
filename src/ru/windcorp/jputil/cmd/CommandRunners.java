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

import java.util.function.Consumer;

public class CommandRunners {
	
	private CommandRunners() {}
	
	private static final CommandRunner CONSOLE_WRAPPER = new CommandRunner() {
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#getName()
		 */
		@Override
		public String getName() {
			return console == null ? "null" : console.getName();
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#respond(java.lang.String)
		 */
		@Override
		public void respond(String msg) {
			if (console != null) console.respond(msg);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#complain(java.lang.String)
		 */
		@Override
		public void complain(String msg) {
			if (console != null) console.complain(msg);
		}
		
		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Wrapper for CommandRunner console [" + console + "]";
		}
	};
	
	// SonarLint: Standard outputs should not be used directly to log anything (java:S106)
	//   This is intended for debugging
	@SuppressWarnings("squid:S106")
	
	private static CommandRunner console = new CommandRunner() {
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#getName()
		 */
		@Override
		public String getName() {
			return "CONSOLE";
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#respond(java.lang.String)
		 */
		@Override
		public void respond(String msg) {
			System.out.println(msg);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#complain(java.lang.String)
		 */
		@Override
		public void complain(String msg) {
			System.out.println("[!] " + msg);
		}
	};
	
	/**
	 * @return the console wrapper (allows changes on the fly)
	 */
	public static CommandRunner getConsole() {
		return CONSOLE_WRAPPER;
	}
	
	/**
	 * @return the console implementation
	 */
	public static CommandRunner getConsoleImplementation() {
		return console;
	}
	
	/**
	 * @param console the console to set
	 */
	public static void setConsole(CommandRunner console) {
		CommandRunners.console = console;
	}
	
	public static CommandRunner of(String name, Consumer<String> respond, Consumer<String> complain) {
		return new CommandRunner() {
			
			@Override
			public String getName() {
				return name;
			}
			
			@Override
			public void respond(String msg) {
				respond.accept(msg);
			}
			
			@Override
			public void complain(String msg) {
				complain.accept(msg);
			}
		};
	}

}
