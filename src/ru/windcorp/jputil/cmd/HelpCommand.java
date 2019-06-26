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
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.jputil.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Javapony
 *
 */
public class HelpCommand extends Command {

	/**
	 * @param names
	 * @param syntax
	 * @param desc
	 */
	public HelpCommand(String[] names, String syntax, String desc) {
		super(names, syntax, desc);
	}
	
	public HelpCommand() {
		this(new String[] {"help", "?"}, "[QUERY]", "Lists appropriate commands or displays information about commands matching QUERY");
	}

	/**
	 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public void run(Invocation inv) throws CommandExceptions {
		String query = inv.getArgs().trim();
		CommandRunner runner = inv.getRunner();
		CommandRegistry parent = Objects.requireNonNull(inv.getParent(), "parent is null");
		
		List<Command> commands;
		
		if (query.isEmpty()) {
			commands = parent.getCommands(inv.getRunner());
			if (commands.isEmpty()) {
				runner.respond(inv.getContext().translate("help.list.header.empty", "Command %1$s does not have available subcommands",
						parent));
				return;
			}
			runner.respond(inv.getContext().translate("help.list.header", "Command %1$s has %2$d subcommands:",
					parent, commands.size()));
		} else {
			query = query.toLowerCase();
			commands = new ArrayList<>();
			
			synchronized (parent) {
				commandLoop:
				for (Command cmd : parent.getCommands()) {
					if (cmd.canRun(runner) != null) {
						continue commandLoop;
					}
					
					for (String name : cmd.getNames()) {
						name = name.toLowerCase();
						if (name.equals(query)) {
							commands.add(0, cmd);
							continue commandLoop;
						} else if (name.contains(query)) {
							commands.add(cmd);
							continue commandLoop;
						}
					}
					
					if (cmd.getDescription().toLowerCase().contains(query)) {
						commands.add(cmd);
					}
				}
			}
			
			if (commands.isEmpty()) {
				runner.respond(inv.getContext().translate("help.search.header.empty", "Command %1$s does not have matching subcommands",
						parent.getName()));
				return;
			}
			runner.respond(inv.getContext().translate("help.list.header", "Search matched %2$d subcommands:",
					parent.getName(), commands.size()));
		}
		
		for (int i = 0; i < commands.size(); ++i) {
			Command command = commands.get(i);
			runner.respond(inv.getContext().translate("help.entry", "- %1$s %2$s - %3$s",
					command.getName(), command.getSyntax(), command.getDescription(), i+1));
		}
	}

}
