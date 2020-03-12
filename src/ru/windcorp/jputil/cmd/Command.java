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

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Command {
	
	private final String[] names;
	private final String syntax;
	private final String desc;
	
	private Function<CommandRunner, Supplier<CommandExceptions>> runnerFilter = null;
	
	protected Command(String[] names, String syntax, String desc) {
		Objects.requireNonNull(names, "names cannot be null");
		Objects.requireNonNull(desc, "desc cannot be null");
		
		if (names.length == 0) {
			throw new IllegalArgumentException("names cannot be empty");
		}
		
		this.names = names;
		this.syntax = syntax == null ? "" : syntax;
		this.desc = desc;
	}
	
	public abstract void run(Invocation inv) throws CommandExceptions;
	
	public Supplier<CommandExceptions> canRun(CommandRunner runner) {
		Function<CommandRunner, Supplier<CommandExceptions>> filter = getRunnerFilter();
		return filter == null ? null : filter.apply(runner);
	}
	
	public String getName() {
		return names[0];
	}
	
	/**
	 * @return the names
	 */
	public String[] getNames() {
		return names;
	}
	
	/**
	 * @return the syntax
	 */
	public String getSyntax() {
		return syntax;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return desc;
	}
	
	/**
	 * @return the runnerFilter
	 */
	public Function<CommandRunner, Supplier<CommandExceptions>> getRunnerFilter() {
		return runnerFilter;
	}
	
	/**
	 * @param runnerFilter the runnerFilter to set
	 */
	public Command setRunnerFilter(Function<CommandRunner, Supplier<CommandExceptions>> runnerFilter) {
		this.runnerFilter = runnerFilter;
		return this;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}
	
}
