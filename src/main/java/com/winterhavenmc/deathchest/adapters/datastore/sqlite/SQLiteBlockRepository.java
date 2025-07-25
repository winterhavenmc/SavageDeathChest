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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite;

import com.winterhavenmc.deathchest.chests.ChestBlock;
import com.winterhavenmc.deathchest.ports.datastore.BlockRepository;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
public final class SQLiteBlockRepository implements BlockRepository
{
	private final Plugin plugin;
	private final Logger logger;
	private final Connection connection;
	private final SQLiteBlockQueryHelper blockQueryHelper = new SQLiteBlockQueryHelper();


	/**
	 * Class constructor
	 */
	public SQLiteBlockRepository(final Plugin plugin, final Connection connection)
	{
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.connection = connection;
	}


	@Override
	public Collection<ChestBlock> getAll()
	{
		final Collection<ChestBlock> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("SelectAllBlocks")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				final String worldName = resultSet.getString("WorldName");
				final int x = resultSet.getInt("X");
				final int y = resultSet.getInt("Y");
				final int z = resultSet.getInt("Z");

				final UUID chestUid = new UUID(resultSet.getLong("ChestUidMsb"), resultSet.getLong("chestUidLsb"));
				final UUID worldUid = new UUID(resultSet.getLong("WorldUidMsb"), resultSet.getLong("WorldUidLsb"));

				// get server world by uuid
				final World world = plugin.getServer().getWorld(worldUid);

				// if server world is null, skip adding record to return set
				//TODO: refactor orphaned block record deletion
				if (world != null)
				{
					// create chest block object from retrieved record
					ChestBlock chestBlock = new ChestBlock(chestUid, world.getName(), world.getUID(), x, y, z, 0, 0);

					// add DeathChestObject to results set
					results.add(chestBlock);
				}
//				else
//				{
//					// delete all records expired more than 30 days in database that have this invalid world
//					deleteOrphanedChests(worldName);
//				}

			}
		}
		catch (SQLException e)
		{
			logger.warning("An error occurred while trying to "
					+ "select all block records from the SQLite datastore.");
			logger.warning(e.getLocalizedMessage());
		}

		return results;
	}


	@Override
	public int save(final Collection<ChestBlock> blockRecords)
	{
		return blockRecords.stream()
				.filter(Objects::nonNull)
				.mapToInt(this::insertBlock)
				.sum();
	}


	private int insertBlock(final ChestBlock block)
	{
		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("InsertBlockRecord")))
		{
			return blockQueryHelper.insertBlock(block, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			logger.warning("An error occurred while inserting a death chest block into the SQLite datastore.");
			logger.warning(sqlException.getMessage());
			return 0;
		}
	}


	@Override
	synchronized public void delete(final ChestBlock chestBlock)
	{
		// if passed chestBlock is null, do nothing and return
		if (chestBlock != null)
		{
			try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("DeleteBlockByLocation")))
			{
				int rowsAffected = blockQueryHelper.DeleteBlock(chestBlock, preparedStatement);
			}
			catch (SQLException sqlException)
			{
				logger.warning("An error occurred while attempting to "
						+ "delete a block record from the SQLite datastore.");
				logger.warning(sqlException.getMessage());
			}
		}
	}

}
