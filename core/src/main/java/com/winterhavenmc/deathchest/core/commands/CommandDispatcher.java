/*
 * Copyright (c) 2025 Tim Savage.
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

package com.winterhavenmc.deathchest.core.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import javax.annotation.Nonnull;
import java.util.List;


public interface CommandDispatcher extends TabExecutor
{
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
	List<String> onTabComplete(@Nonnull CommandSender sender,
	                           @Nonnull Command command,
	                           @Nonnull String alias,
	                           String[] args);

	/**
	 * Command handler for DeathChest
	 *
	 * @param sender  the command sender
	 * @param command the command typed
	 * @param label   the command label
	 * @param args    Array of String - command arguments
	 * @return boolean - always returns {@code true}, to suppress bukkit builtin help message
	 */
	@Override
	boolean onCommand(@Nonnull CommandSender sender,
	                  @Nonnull Command command,
	                  @Nonnull String label,
	                  String[] args);
}
