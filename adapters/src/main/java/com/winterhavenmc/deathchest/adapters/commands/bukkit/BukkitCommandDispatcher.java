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

import com.winterhavenmc.deathchest.core.context.CommandCtx;
import com.winterhavenmc.deathchest.core.ports.commands.CommandDispatcher;
import com.winterhavenmc.deathchest.core.util.MessageId;
import com.winterhavenmc.deathchest.core.util.SoundId;

import org.bukkit.command.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Predicate;


/**
 * A class that implements player commands for the plugin
 */
public final class BukkitCommandDispatcher implements CommandDispatcher
{
	private final CommandCtx ctx;
	private final SubcommandRegistry subcommandRegistry = new SubcommandRegistry();


	/**
	 * Class constructor
	 */
	public BukkitCommandDispatcher(final CommandCtx ctx)
	{
		this.ctx = ctx;
		Objects.requireNonNull(ctx.plugin().getCommand("deathchest")).setExecutor(this);

		// register subcommands
		for (SubcommandType subcommandType : SubcommandType.values())
		{
			subcommandRegistry.register(subcommandType.create(ctx));
		}

		// register help command
		subcommandRegistry.register(new HelpCommand(ctx.messageBuilder(), subcommandRegistry));
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
				? argsList.removeFirst()
				: "help";

		// get subcommand from map by name
		Subcommand subcommand = subcommandRegistry.getCommand(subcommandName);

		// if subcommand is null, get help command from map
		if (subcommand == null)
		{
			subcommand = subcommandRegistry.getCommand("help");
			ctx.messageBuilder().compose(sender, MessageId.COMMAND_INVALID).send();
			ctx.messageBuilder().sounds().play(sender, SoundId.COMMAND_INVALID);
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
		return subcommandRegistry.getNames().stream()
				.filter(hasPermission(sender))
				.filter(matchesPrefix(matchString))
				.toList();
	}


	private Predicate<String> hasPermission(final CommandSender sender)
	{
		return subcommandName -> Optional.of(subcommandRegistry.getCommand(subcommandName))
				.map(subcommand -> sender.hasPermission("deathchest." + subcommandName))
				.orElse(false);
	}


	private Predicate<String> matchesPrefix(final String prefix)
	{
		return subcommandName -> subcommandName.startsWith(prefix.toLowerCase());
	}


}
