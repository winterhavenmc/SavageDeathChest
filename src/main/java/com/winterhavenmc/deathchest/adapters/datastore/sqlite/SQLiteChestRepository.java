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

import com.winterhavenmc.deathchest.chests.DeathChestRecord;
import com.winterhavenmc.deathchest.ports.datastore.ChestRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public final class SQLiteChestRepository implements ChestRepository
{
	private final Logger logger;
	private final Connection connection;
	private final SQLiteChestRowMapper chestRowMapper = new SQLiteChestRowMapper();
	private final SQLiteChestQueryHelper chestQueryHelper = new SQLiteChestQueryHelper();


	public SQLiteChestRepository(final Logger logger, final Connection connection)
	{
		this.logger = logger;
		this.connection = connection;
	}


	/**
	 * Retrieve a collection of all chest records from the datastore
	 *
	 * @return List of DeathChest
	 */
	@Override
	public Collection<DeathChestRecord> getAll()
	{
		final Collection<DeathChestRecord> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("SelectAllChests")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				results.add(chestRowMapper.map(resultSet));
			}
		}
		catch (SQLException exception)
		{
			logger.warning("An error occurred while trying to " +
					"select all chest records from the SQLite datastore.");
			logger.warning(exception.getMessage());
		}

		return results;
	}


	/**
	 * Insert a chest record in the datastore
	 *
	 * @param deathChests a collection of DeathChest objects to insert into the datastore
	 */
	@Override
	public int save(Collection<DeathChestRecord> deathChests)
	{
		int count = 0;

		for (DeathChestRecord deathChest : deathChests)
		{
			if (deathChest != null)
			{
				try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("InsertChestRecord")))
				{
					count += chestQueryHelper.insertChest(deathChest, preparedStatement);
				} catch (SQLException sqlException)
				{
					logger.warning("An error occurred while inserting a DeathChest into the SQLite datastore.");
					logger.warning(sqlException.getMessage());
				}
			}
		}

		return count;
	}


	/**
	 * Delete a chest record from the datastore
	 *
	 * @param deathChest the chest to delete
	 */
	@Override
	public void delete(DeathChestRecord deathChest)
	{
		if (deathChest != null)
		{
			try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("DeleteChestByUUID")))
			{
				chestQueryHelper.deleteChest(deathChest, preparedStatement);
			}
			catch (SQLException sqlException)
			{
				logger.warning("An error occurred while attempting to "
						+ "delete a chest record from the SQLite datastore.");
				logger.warning(sqlException.getMessage());
			}
		}
	}


	@Override
	public int getCount()
	{
		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("SelectChestCount")))
		{
			return SQLiteChestQueryHelper.getChestCount(preparedStatement);
		}
		catch (SQLException e)
		{
			logger.warning("An error occurred while attempting to " +
					"retrieve a count of chest records from the SQLite datastore.");
			logger.warning(e.getLocalizedMessage());
			return 0;
		}
	}


	/**
	 * Delete orphaned chests in nonexistent world {@code worldName}
	 *
	 * @param worldName the world name of orphaned chests to delete
	 */
	public int deleteOrphanedChests(final String worldName)
	{
		// pastDueTime = current time in milliseconds - 30 days
		final long pastDueTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("DeleteOrphanedChests")))
		{
			return chestQueryHelper.deleteOrphanedChests(worldName, pastDueTime, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			logger.warning("An error occurred while attempting to delete orphaned chests from the datastore.");
			logger.warning(sqlException.getMessage());
			return 0;
		}
	}

}
