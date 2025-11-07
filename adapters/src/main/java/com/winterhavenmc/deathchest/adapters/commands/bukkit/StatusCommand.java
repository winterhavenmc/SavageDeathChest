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
import com.winterhavenmc.deathchest.core.messages.Macro;
import com.winterhavenmc.deathchest.core.messages.MessageId;
import com.winterhavenmc.deathchest.core.permissions.protectionplugins.ProtectionPlugin;

import com.winterhavenmc.library.messagebuilder.core.ports.pipeline.formatters.duration.BoundedDuration;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.winterhavenmc.deathchest.core.messages.Macro.*;


/**
 * Class that implements the status subcommand. Displays plugin configuration settings.
 */
final class StatusCommand extends SubcommandAbstract
{
	private final CommandCtx ctx;


	StatusCommand(final CommandCtx ctx)
	{
		this.ctx = ctx;
		this.name = "status";
		this.usageString = "/deathchest status";
		this.description = MessageId.COMMAND_HELP_STATUS;
	}


	@Override
	public boolean onCommand(final CommandSender sender, final List<String> args)
	{
		if (!sender.hasPermission("deathchest.status"))
		{
			return ctx.messageBuilder().compose(sender, MessageId.COMMAND_FAIL_STATUS_PERMISSION).send();
		}

		displayStatusBanner(sender);
		displayDebugSetting(sender);
		displayPluginVersion(sender);
		displayLanguage(sender);
		displayLocale(sender);
		displayTimezone(sender);
		displayExpirationTime(sender);
		displayProtectionTime(sender);
		displaySearchDistance(sender);
		displayRequireChest(sender);
		displayQuickLoot(sender);
		displayKillerLooting(sender);
		displayProtectionPlugins(sender);
		displayEnabledWorlds(sender);
		displayReplaceableBlocks(sender);
		displayStatusFooter(sender);

		return true;
	}


	private void displayStatusBanner(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_BANNER).send();
	}


	private void displayStatusFooter(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_FOOTER).send();
	}


	private void displayPluginVersion(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_PLUGIN_VERSION).send();
	}


	private void displayDebugSetting(final CommandSender sender)
	{
		if (ctx.plugin().getConfig().getBoolean("debug"))
		{
			sender.sendMessage(ChatColor.DARK_RED + "DEBUG: true");
		}
	}


	private void displayLanguage(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_LANGUAGE)
				.setMacro(Macro.LANGUAGE, ctx.messageBuilder().config().language())
				.send();
	}


	private void displayLocale(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_LOCALE)
				.setMacro(Macro.LOCALE, ctx.messageBuilder().config().locale().toLanguageTag())
				.send();
	}


	private void displayTimezone(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_TIMEZONE)
				.setMacro(TIMEZONE, ctx.messageBuilder().config().zoneId().toString())
				.send();
	}


	private void displayProtectionTime(final CommandSender sender)
	{
		// if config setting is zero, use negative number to represent unlimited time
		int protectionTime = ctx.plugin().getConfig().getInt("protection-time");
		if (protectionTime == 0) { protectionTime = -1; }

		BoundedDuration boundedDuration = new BoundedDuration(Duration.ofMinutes(protectionTime), ChronoUnit.MINUTES);

		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_PROTECTION_TIME)
				.setMacro(PROTECTION_DURATION, boundedDuration)
				.send();
	}


	private void displayExpirationTime(final CommandSender sender)
	{
		// if config setting is zero, use negative number to represent unlimited time
		int expirationTime = ctx.plugin().getConfig().getInt("expire-time");
		if (expirationTime == 0) { expirationTime = -1; }

		BoundedDuration boundedDuration = new BoundedDuration(Duration.ofMinutes(expirationTime), ChronoUnit.MINUTES);

		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_EXPIRATION_TIME)
				.setMacro(EXPIRATION_DURATION, boundedDuration)
				.send();
	}


	private void displaySearchDistance(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_SEARCH_DISTANCE)
				.setMacro(SEARCH_DISTANCE, ctx.plugin().getConfig().getString("search-distance"))
				.send();
	}


	private void displayRequireChest(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_REQUIRE_CHEST)
				.setMacro(REQUIRE_CHEST, ctx.plugin().getConfig().getString("require-chest"))
				.send();
	}


	private void displayQuickLoot(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_QUICK_LOOT)
				.setMacro(Macro.QUICK_LOOT, ctx.plugin().getConfig().getString("quick-loot"))
				.send();
	}


	private void displayKillerLooting(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_KILLER_LOOTING)
				.setMacro(Macro.KILLER_LOOTING, ctx.plugin().getConfig().getString("killer-looting"))
				.send();
	}


	private void displayEnabledWorlds(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_ENABLED_WORLDS)
				.setMacro(Macro.ENABLED_WORLDS, ctx.messageBuilder().worlds().enabledNames().toString())
				.send();
	}


	private void displayReplaceableBlocks(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_REPLACEABLE_BLOCKS)
				.setMacro(Macro.REPLACEABLE_BLOCKS, ctx.chestManager().getReplaceableBlocks())
				.send();
	}


	private void displayProtectionPlugins(final CommandSender sender)
	{
		ctx.messageBuilder().compose(sender, MessageId.COMMAND_STATUS_PROTECTION_PLUGINS).send();

		int count = 0;
		for (ProtectionPlugin protectionPlugin : ctx.protectionPluginRegistry().getAll())
		{

			Collection<String> pluginSettings = new LinkedList<>();

			count++;
			String statusString = ChatColor.AQUA + "  " + protectionPlugin + ": ";

			if (protectionPlugin.isIgnoredOnPlace())
			{
				pluginSettings.add("ignore on placement");
			}
			else
			{
				pluginSettings.add("comply on placement");
			}
			if (protectionPlugin.isIgnoredOnAccess())
			{
				pluginSettings.add("ignore on access");
			}
			else
			{
				pluginSettings.add("comply on access");
			}
			statusString = statusString + ChatColor.RESET + pluginSettings;
			sender.sendMessage(statusString);
		}
		if (count == 0)
		{
			sender.sendMessage(ChatColor.AQUA + "  [ NONE ENABLED ]");
		}
	}

}
