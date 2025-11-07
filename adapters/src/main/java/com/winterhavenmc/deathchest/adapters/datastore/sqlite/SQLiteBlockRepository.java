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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite;

import com.winterhavenmc.deathchest.models.chestblock.ChestBlock;
import com.winterhavenmc.deathchest.models.chestblock.ValidChestBlock;
import com.winterhavenmc.deathchest.core.ports.datastore.BlockRepository;
import com.winterhavenmc.library.messagebuilder.MessageBuilder;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.util.*;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
public final class SQLiteBlockRepository implements BlockRepository
{
	private final Connection connection;
	private final SqliteBlockQueryExecutor queryExecutor = new SqliteBlockQueryExecutor();
	private final SqliteBlockRowMapper rowMapper = new SqliteBlockRowMapper();
	private final Plugin plugin;
	private final MessageBuilder messageBuilder;


	/**
	 * Class constructor
	 */
	public SQLiteBlockRepository(final Plugin plugin, MessageBuilder messageBuilder, final Connection connection)
	{
		this.plugin = plugin;
		this.messageBuilder = messageBuilder;
		this.connection = connection;
	}


	@Override
	public Collection<ValidChestBlock> getAll()
	{
		final Collection<ValidChestBlock> results = new HashSet<>();

		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectAllBlocks"));
		     final ResultSet resultSet = queryExecutor.selectAllBlocks(preparedStatement))
		{
			while (resultSet.next())
			{
				ChestBlock chestBlock = rowMapper.map(plugin, resultSet);
				if (chestBlock instanceof ValidChestBlock validChestBlock)
				{
					results.add(validChestBlock);
				}
			}
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_SELECT_ALL_BLOCKS_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(e.getLocalizedMessage());
		}

		return results;
	}


	@Override
	public int save(final Collection<ValidChestBlock> blockRecords)
	{
		return blockRecords.stream()
				.filter(Objects::nonNull)
				.mapToInt(this::insertBlock)
				.sum();
	}


	private int insertBlock(final ValidChestBlock validChestBlock)
	{
		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("InsertBlockRecord")))
		{
			return queryExecutor.insertBlock(validChestBlock, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_INSERT_CHEST_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
			return 0;
		}
	}


	@Override
	public int delete(final ValidChestBlock validChestBlock)
	{
		try (final PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("DeleteBlockByLocation")))
		{
			return queryExecutor.DeleteBlock(validChestBlock, preparedStatement);
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.TABLE_DELETE_BLOCK_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
			return 0;
		}
	}

}
