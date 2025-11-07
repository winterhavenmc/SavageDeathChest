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

package com.winterhavenmc.deathchest.core.controller;

import com.winterhavenmc.deathchest.core.ports.commands.CommandDispatcher;
import com.winterhavenmc.deathchest.core.ports.datastore.ConnectionProvider;
import com.winterhavenmc.deathchest.core.util.MetricsHandler;

import org.bukkit.plugin.java.JavaPlugin;


/**
 * Bukkit plugin to allow creation of graveyard locations where players
 * will respawn on death. The nearest graveyard location that is valid
 * for the player will be chosen at the time of death.
 */
public final class ValidPluginController implements PluginController
{
	private final JavaPlugin plugin;
	private ConnectionProvider datastore;
	public CommandDispatcher commandDispatcher;


	ValidPluginController(final JavaPlugin plugin)
	{
		this.plugin = plugin;
	}


	public void startUp(final ConnectionProvider connectionProvider,
	                    final CommandDispatcher commandDispatcher)
	{
		// install default config.yml if not present
		plugin.saveDefaultConfig();

		// connect to data store
		this.datastore = connectionProvider;

		// initialize command dispatcher (depends on initialized discovery observer)
		this.commandDispatcher = commandDispatcher;

		// instantiate metrics handler
		new MetricsHandler(plugin);
	}


	public void shutDown()
	{
		datastore.close();
	}

}
