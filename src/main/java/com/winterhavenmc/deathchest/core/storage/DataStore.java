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

package com.winterhavenmc.deathchest.core.storage;

import com.winterhavenmc.deathchest.plugin.Bootstrap;
import com.winterhavenmc.deathchest.core.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ChestRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ConnectionProvider;

import org.bukkit.plugin.Plugin;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
public final class DataStore implements AutoCloseable
{
	private final ConnectionProvider connectionProvider;


	/**
	 * Private constructor
	 */
	private DataStore(final ConnectionProvider connectionProvider)
	{
		this.connectionProvider = connectionProvider;
	}


	/**
	 * Create new data store of given type and convert old data store.<br>
	 * Two parameter version used when a datastore instance already exists
	 *
	 * @param plugin reference to plugin main class
	 * @return a new datastore instance of the given type
	 */
	public static DataStore connect(final Plugin plugin)
	{
		ConnectionProvider connectionProvider = Bootstrap.getConnectionProvider(plugin);

		// initialize data store
		try
		{
			connectionProvider.connect();
		}
		catch (Exception exception)
		{
			plugin.getLogger().severe("Could not initialize the datastore!");
			plugin.getLogger().severe(exception.getLocalizedMessage());
		}

		// return initialized data store
		return new DataStore(connectionProvider);
	}


	/**
	 * Close datastore connection
	 */
	public void close()
	{
		connectionProvider.close();
	}


	/**
	 * Return DeathChest repository instance
	 */
	public ChestRepository deathChests()
	{
		return connectionProvider.deathChests();
	}


	/**
	 * Return ChestBlock repository instance
	 */
	public BlockRepository chestBlocks()
	{
		return connectionProvider.chestBlocks();
	}

}
