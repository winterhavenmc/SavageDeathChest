/*
 * Copyright (c) 2022-2025 Tim Savage.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.winterhavenmc.deathchest.adapters.commands.bukkit;

import com.winterhavenmc.deathchest.core.util.MessageId;
import com.winterhavenmc.deathchest.core.util.SoundId;
import com.winterhavenmc.library.messagebuilder.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 * Class that implements the help subcommand. Displays help and usage for plugin subcommands.
 */
final class HelpCommand extends SubcommandAbstract
{
	private final MessageBuilder messageBuilder;
	private final SubcommandRegistry subcommandRegistry;


	/**
	 * Class constructor
	 *
	 * @param subcommandRegistry reference to the subcommand registry
	 */
	HelpCommand(final MessageBuilder messageBuilder, final SubcommandRegistry subcommandRegistry)
	{
		this.messageBuilder = messageBuilder;
		this.subcommandRegistry = Objects.requireNonNull(subcommandRegistry);
		this.name = "help";
		this.usageString = "/deathchest help [command]";
		this.description = MessageId.COMMAND_HELP_HELP;
		this.maxArgs = 1;
	}


	@Override
	public List<String> onTabComplete(
			final @Nonnull CommandSender sender,
			final @Nonnull Command command,
			final @Nonnull String alias,
			final String[] args)
	{
		List<String> returnList = new LinkedList<>();

		if (args.length == 2)
		{
			for (String subcommand : subcommandRegistry.getNames())
			{
				if (sender.hasPermission("deathchest." + subcommand)
						&& subcommand.startsWith(args[1].toLowerCase())
						&& !subcommand.equalsIgnoreCase("help"))
				{
					returnList.add(subcommand);
				}
			}
		}

		return returnList;
	}


	@Override
	public boolean onCommand(final CommandSender sender, final List<String> args)
	{
		// if command sender does not have permission to display help, output error message and return true
		if (!sender.hasPermission("deathchest.help"))
		{
			return messageBuilder.compose(sender, MessageId.COMMAND_FAIL_HELP_PERMISSION).send();
		}

		// if no arguments, display usage for all commands
		if (args.isEmpty())
		{
			displayUsageAll(sender);
			return true;
		}

		// get subcommand name
		String subcommandName = args.getFirst();
		displayHelp(sender, subcommandName);
		return true;
	}


	/**
	 * Display help message and usage for a command
	 *
	 * @param sender      the command sender
	 * @param commandName the name of the command for which to show help and usage
	 */
	void displayHelp(final CommandSender sender, final String commandName)
	{

		// get subcommand from map by name
		Subcommand subcommand = subcommandRegistry.getCommand(commandName);

		// if subcommand found in map, display help message and usage
		if (subcommand != null)
		{
			messageBuilder.compose(sender, subcommand.getDescription()).send();
			subcommand.displayUsage(sender);
		}
		// else display invalid command help message and usage for all commands
		else
		{
			messageBuilder.compose(sender, MessageId.COMMAND_HELP_INVALID).send();
			messageBuilder.sounds().play(sender, SoundId.COMMAND_INVALID);
			displayUsageAll(sender);
		}
	}


	/**
	 * Display usage message for all commands
	 *
	 * @param sender the command sender
	 */
	void displayUsageAll(final CommandSender sender)
	{
		messageBuilder.compose(sender, MessageId.COMMAND_HELP_USAGE).send();

		for (String subcommandName : subcommandRegistry.getNames())
		{
			if (subcommandRegistry.getCommand(subcommandName) != null)
			{
				subcommandRegistry.getCommand(subcommandName).displayUsage(sender);
			}
		}
	}

}
