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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite;

import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.core.ports.datastore.ChestRepository;
import com.winterhavenmc.library.messagebuilder.MessageBuilder;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;


public final class SQLiteChestRepository implements ChestRepository
{
	private final Plugin plugin;
	private final MessageBuilder messageBuilder;
	private final Connection connection;
	private final SqliteChestRowMapper chestRowMapper = new SqliteChestRowMapper();
	private final SqliteChestQueryExecutor queryExecutor = new SqliteChestQueryExecutor();


	public SQLiteChestRepository(final Plugin plugin, final MessageBuilder messageBuilder, final Connection connection)
	{
		this.plugin = plugin;
		this.messageBuilder = messageBuilder;
		this.connection = connection;
	}


	/**
	 * Retrieve a collection of all chest records from the datastore
	 *
	 * @return List of DeathChest
	 */
	@Override
	public Collection<ValidDeathChest> getAll()
	{
		final Collection<ValidDeathChest> results = new HashSet<>();

		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectAllChests"));
		     final ResultSet resultSet = queryExecutor.SelectAllChests(preparedStatement))
		{
			while (resultSet.next())
			{
				DeathChest deathChest = chestRowMapper.map(resultSet);
				if (deathChest instanceof ValidDeathChest validDeathChest)
				{
					results.add(validDeathChest);
				}
				else
				{
					String worldName = resultSet.getString("worldName");
					int rowsAffected = deleteOrphanedChest(worldName);
					plugin.getLogger().info(SqliteMessage.TABLE_DELETE_ORPHANED_CHESTS_NOTICE
							.getLocalizeMessage(messageBuilder.config().locale(), rowsAffected, worldName));
				}
			}
		}
		catch (SQLException exception)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_SELECT_ALL_CHESTS_ERROR
					.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(exception.getMessage());
		}

		return results;
	}


	/**
	 * Insert a chest record in the datastore
	 *
	 * @param deathChests a collection of DeathChest objects to insert into the datastore
	 */
	@Override
	public int save(Collection<ValidDeathChest> deathChests)
	{
		int count = 0;

		for (ValidDeathChest deathChest : deathChests)
		{
			if (deathChest != null)
			{
				try (PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("InsertChestRecord")))
				{
					count += queryExecutor.insertChest(deathChest, preparedStatement);
				} catch (SQLException sqlException)
				{
					plugin.getLogger().warning(SqliteMessage.TABLE_INSERT_CHEST_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
					plugin.getLogger().warning(sqlException.getMessage());
				}
			}
		}

		return count;
	}


	/**
	 * Delete a chest record from the datastore
	 *
	 * @param validDeathChest the chest to delete
	 */
	@Override
	public int delete(ValidDeathChest validDeathChest)
	{
		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("DeleteChestByUUID")))
		{
			return queryExecutor.deleteChest(validDeathChest, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_DELETE_CHEST_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(sqlException.getMessage());
			return 0;
		}
	}


	@Override
	public int getCount()
	{
		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectChestCount")))
		{
			return SqliteChestQueryExecutor.getChestItemCount(preparedStatement);
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_SELECT_CHEST_COUNT_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(e.getLocalizedMessage());
			return 0;
		}
	}


	/**
	 * Delete orphaned chests in nonexistent worlds 30 days after expiration time has past
	 *
	 * @param worldName the world name of orphaned chests to delete
	 */
	public int deleteOrphanedChest(final String worldName)
	{
		// pastDueTime = current time in milliseconds - 30 days
		final long pastDueTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("DeleteOrphanedChests")))
		{
			return queryExecutor.deleteOrphanedChests(worldName, pastDueTime, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_DELETE_ORPHANED_CHESTS_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(sqlException.getMessage());
			return 0;
		}
	}

}
