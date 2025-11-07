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

package com.winterhavenmc.deathchest;

import com.winterhavenmc.deathchest.adapters.commands.bukkit.BukkitCommandDispatcher;
import com.winterhavenmc.deathchest.adapters.datastore.sqlite.SqliteConnectionProvider;
import com.winterhavenmc.deathchest.adapters.listeners.bukkit.BukkitBlockEventListener;
import com.winterhavenmc.deathchest.adapters.listeners.bukkit.BukkitInventoryEventListener;
import com.winterhavenmc.deathchest.adapters.listeners.bukkit.BukkitPlayerEventListener;

import com.winterhavenmc.deathchest.core.chests.ChestManager;
import com.winterhavenmc.deathchest.core.commands.CommandDispatcher;
import com.winterhavenmc.deathchest.core.context.CommandCtx;
import com.winterhavenmc.deathchest.core.controller.InvalidPluginController;
import com.winterhavenmc.deathchest.core.controller.PluginController;
import com.winterhavenmc.deathchest.core.controller.ValidPluginController;
import com.winterhavenmc.deathchest.core.listeners.BlockEventListener;
import com.winterhavenmc.deathchest.core.listeners.InventoryEventListener;
import com.winterhavenmc.deathchest.core.listeners.PlayerEventListener;
import com.winterhavenmc.deathchest.core.permissions.protectionplugins.ProtectionPluginRegistry;
import com.winterhavenmc.deathchest.core.ports.datastore.ConnectionProvider;
import com.winterhavenmc.deathchest.core.context.ListenerCtx;

import com.winterhavenmc.library.messagebuilder.MessageBuilder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class Bootstrap extends JavaPlugin
{
	private PluginController pluginController;
	private BlockEventListener blockEventListener;
	private PlayerEventListener playerEventListener;
	private InventoryEventListener inventoryEventListener;


	@Override
	public void onEnable()
	{
		// instantiate plugin controller
		this.pluginController = PluginController.create(this);

		final MessageBuilder messageBuilder = MessageBuilder.create(this);
		ConnectionProvider connectionProvider = new SqliteConnectionProvider(this, messageBuilder);

		final ChestManager chestManager = new ChestManager(this, messageBuilder, connectionProvider);
		final ProtectionPluginRegistry pluginRegistry = new ProtectionPluginRegistry(this);

		// instantiate context containers
		final ListenerCtx listenerCtx = new ListenerCtx(this, messageBuilder, chestManager, pluginRegistry);
		final CommandCtx commandCtx = new CommandCtx(this, messageBuilder, chestManager, pluginRegistry);

		// initialize event listeners
		this.blockEventListener = new BukkitBlockEventListener(listenerCtx); // adapter
		this.playerEventListener = new BukkitPlayerEventListener(listenerCtx); // adapter
		this.inventoryEventListener = new BukkitInventoryEventListener(listenerCtx); // adapter

		// instantiate command dispatcher
		final CommandDispatcher commandDispatcher = new BukkitCommandDispatcher(commandCtx); // adapter

		// startup controller if valid, else close datastore connection and disable plugin
		switch (pluginController)
		{
			case ValidPluginController validPluginController -> validPluginController.startUp(connectionProvider, commandDispatcher);
			case InvalidPluginController invalidPluginController ->
			{
				commandCtx.plugin().getLogger().severe("Could not initialize plugin controller: " + invalidPluginController.reason().getDefaultMessage());
				connectionProvider.close();
				Bukkit.getPluginManager().disablePlugin(this);
			}
		}
	}


	@Override
	public void onDisable()
	{
		if (pluginController instanceof ValidPluginController validPluginController)
		{
			validPluginController.shutDown();
		}
	}

}
