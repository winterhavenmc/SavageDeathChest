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
import com.winterhavenmc.deathchest.core.util.MessageId;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;


/**
 * Class that implements the reload subcommand. Reloads the plugin configuration settings.
 */
final class ReloadCommand extends SubcommandAbstract
{
	private final CommandCtx ctx;


	/**
	 * Class constructor
	 */
	ReloadCommand(final CommandCtx ctx)
	{
		this.ctx = ctx;
		this.name = "reload";
		this.usageString = "/deathchest reload";
		this.description = MessageId.COMMAND_HELP_RELOAD;
	}


	public boolean onCommand(final CommandSender sender, final List<String> args)
	{
		// check for null parameter
		Objects.requireNonNull(sender);

		if (!sender.hasPermission("deathchest.reload"))
		{
			return ctx.messageBuilder().compose(sender, MessageId.COMMAND_FAIL_RELOAD_PERMISSION).send();
		}

		// copy default config from jar if it doesn't exist
		ctx.plugin().saveDefaultConfig();

		// reload config file
		ctx.plugin().reloadConfig();

		// update enabledWorlds list
		ctx.messageBuilder().reload();

		// reload ChestManager
		ctx.chestManager().reload();

		// send success message
		return ctx.messageBuilder().compose(sender, MessageId.COMMAND_SUCCESS_RELOAD).send();
	}

}
