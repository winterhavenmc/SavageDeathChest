/*
 * Copyright (c) 2022 Tim Savage.
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

package com.winterhavenmc.deathchest.commands;

import com.winterhavenmc.deathchest.PluginMain;

import org.bukkit.command.*;
import com.winterhavenmc.deathchest.messages.MessageId;
import com.winterhavenmc.deathchest.sounds.SoundId;

import javax.annotation.Nonnull;
import java.util.*;


/**
 * A class that implements player commands for the plugin
 */
public final class CommandManager implements TabExecutor
{
	private final PluginMain plugin;
	private final SubcommandRegistry subcommandRegistry = new SubcommandRegistry();


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 */
	public CommandManager(final PluginMain plugin)
	{
		this.plugin = Objects.requireNonNull(plugin);
		Objects.requireNonNull(plugin.getCommand("deathchest")).setExecutor(this);

		// register subcommands
		for (SubcommandType subcommandType : SubcommandType.values())
		{
			subcommandRegistry.register(subcommandType.create(plugin));
		}

		// register help command
		subcommandRegistry.register(new HelpCommand(plugin, subcommandRegistry));
	}


	/**
	 * Tab completer for DeathChest
	 *
	 * @param sender  the command sender
	 * @param command the command typed
	 * @param alias   alias for the command
	 * @param args    additional command arguments
	 * @return List of String - the possible matching values for tab completion
	 */
	@Override
	public List<String> onTabComplete(final @Nonnull CommandSender sender,
	                                  final @Nonnull Command command,
	                                  final @Nonnull String alias,
	                                  final String[] args)
	{
		// if more than one argument, use tab completer of subcommand
		if (args.length > 1)
		{
			// get subcommand from map
			Subcommand subcommand = subcommandRegistry.getCommand(args[0]);

			// if no subcommand returned from map, return empty list
			if (subcommand == null)
			{
				return Collections.emptyList();
			}
			// return subcommand tab completer output
			return subcommand.onTabComplete(sender, command, alias, args);
		}

		// return list of subcommands for which sender has permission
		return matchingCommands(sender, args[0]);
	}


	/**
	 * Command handler for DeathChest
	 *
	 * @param sender   the command sender
	 * @param command  the command typed
	 * @param label    the command label
	 * @param args     Array of String - command arguments
	 * @return boolean - always returns {@code true}, to suppress bukkit builtin help message
	 */
	@Override
	public boolean onCommand(final @Nonnull CommandSender sender,
	                         final @Nonnull Command command,
	                         final @Nonnull String label,
	                         final String[] args)
	{
		// convert args array to list
		List<String> argsList = new LinkedList<>(Arrays.asList(args));

		// get subcommand name, or "help" if list is empty
		String subcommandName = (!argsList.isEmpty())
				? argsList.remove(0)
				: "help";

		// get subcommand from map by name
		Subcommand subcommand = subcommandRegistry.getCommand(subcommandName);

		// if subcommand is null, get help command from map
		if (subcommand == null)
		{
			subcommand = subcommandRegistry.getCommand("help");
			plugin.messageBuilder.compose(sender, MessageId.COMMAND_FAIL_INVALID_COMMAND).send();
			plugin.soundConfig.playSound(sender, SoundId.COMMAND_INVALID);
		}

		// execute subcommand
		return subcommand.onCommand(sender, argsList);
	}


	/**
	 * Get matching list of subcommands for which sender has permission
	 *
	 * @param sender the command sender
	 * @param matchString the string prefix to match against command names
	 * @return List of String - command names that match prefix and sender has permission
	 */
	private List<String> matchingCommands(final CommandSender sender, final String matchString)
	{
		List<String> returnList = new LinkedList<>();

		for (String subcommand : subcommandRegistry.getNames())
		{
			if (sender.hasPermission("deathchest." + subcommand)
					&& subcommand.startsWith(matchString.toLowerCase()))
			{
				returnList.add(subcommand);
			}
		}
		return returnList;
	}

}
