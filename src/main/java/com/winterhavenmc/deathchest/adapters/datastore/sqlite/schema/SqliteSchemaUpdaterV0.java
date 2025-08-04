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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite.schema;

import com.winterhavenmc.deathchest.adapters.datastore.sqlite.SqliteMessage;
import com.winterhavenmc.deathchest.adapters.datastore.sqlite.SqliteQueries;
import com.winterhavenmc.deathchest.models.chestblock.ChestBlock;
import com.winterhavenmc.deathchest.models.chestblock.ValidChestBlock;
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.ports.datastore.ChestRepository;
import com.winterhavenmc.library.messagebuilder.resources.configuration.LocaleProvider;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public final class SqliteSchemaUpdaterV0 implements SqliteSchemaUpdater
{
	private final Plugin plugin;
	private final Connection connection;
	private final ChestRepository chestRepository;
	private final BlockRepository blockRepository;
	private final LocaleProvider localeProvider;


	SqliteSchemaUpdaterV0(final Plugin plugin,
	                      final Connection connection,
	                      final LocaleProvider localeProvider,
	                      final ChestRepository chestRepository,
	                      final BlockRepository blockRepository)
	{
		this.plugin = plugin;
		this.connection = connection;
		this.chestRepository = chestRepository;
		this.blockRepository = blockRepository;
		this.localeProvider = localeProvider;
	}


	public void update()
	{
		if (tableExists(connection, "Chests"))
		{
			try (Statement statement = connection.createStatement())
			{
				Set<ValidDeathChest> existingChests = selectAllChestRecords(plugin, connection);

				statement.executeUpdate(SqliteQueries.getQuery("dropChestTable"));
				statement.executeUpdate(SqliteQueries.getQuery("CreateChestTable"));
				statement.executeUpdate("PRAGMA user_version = 2");

				int chestCount = chestRepository.save(existingChests);
				plugin.getLogger().info(chestCount + " death chest records migrated to schema v2 in the SQLite datastore.");
			}
			catch (SQLException sqlException)
			{
				plugin.getLogger().warning(SqliteMessage.SCHEMA_UPDATE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
				plugin.getLogger().warning(sqlException.getLocalizedMessage());
			}
		}

		if (tableExists(connection, "Blocks"))
		{
			try (Statement statement = connection.createStatement())
			{
				Set<ValidChestBlock> existingBlocks = selectAllBlockRecords(plugin, connection);

				statement.executeUpdate(SqliteQueries.getQuery("dropBlockTable"));
				statement.executeUpdate(SqliteQueries.getQuery("CreateBlockTable"));
				statement.executeUpdate("PRAGMA user_version = 2");

				int blockCount = blockRepository.save(existingBlocks);
				plugin.getLogger().info(blockCount + " death chest records migrated to schema v2 in the SQLite datastore.");
			}
			catch(SQLException sqlException)
			{
				plugin.getLogger().warning(SqliteMessage.SCHEMA_UPDATE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
				plugin.getLogger().warning(sqlException.getLocalizedMessage());
			}
		}
	}


	Set<ValidChestBlock> selectAllBlockRecords(final Plugin plugin, final Connection connection)
	{
		final Set<ValidChestBlock> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectAllBlocks")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				final UUID chestUid = getUid(resultSet.getString("ChestUid"));
				final String worldName = resultSet.getString("WorldName");
				final World world = plugin.getServer().getWorld(worldName);
				final int x = resultSet.getInt("X");
				final int y = resultSet.getInt("Y");
				final int z = resultSet.getInt("Z");

				if (world != null)
				{
					ChestBlock chestBlock = ChestBlock.of(chestUid, worldName, world.getUID(), x, y, z);

					if (chestBlock instanceof ValidChestBlock validChestBlock)
					{
						results.add(validChestBlock);
					}
				}
				else
				{
					plugin.getLogger().warning("World name '" + worldName + "' does not match a loaded world on the server.");
				}
			}
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.SELECT_ALL_CHESTS_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
		}

		return results;
	}


	Set<ValidDeathChest> selectAllChestRecords(final Plugin plugin, final Connection connection)
	{
		final Set<ValidDeathChest> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectAllChestsV0")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				final UUID chestUid = getUid(resultSet.getString("ChestUUID"));
				final UUID ownerUid = getUid(resultSet.getString("OwnerUUID"));
				final UUID killerUid = getUid(resultSet.getString("KillerUUID"));
				final int itemCount = resultSet.getInt("ItemCount");
				final Instant placementTime = Instant.ofEpochMilli(resultSet.getLong("PlacementTime"));
				final Instant expirationTime = Instant.ofEpochMilli(resultSet.getLong("ExpirationTime"));
				final int x = resultSet.getInt("X");
				final int y = resultSet.getInt("Y");
				final int z = resultSet.getInt("Z");

				final OfflinePlayer offlineOwner = plugin.getServer().getOfflinePlayer(ownerUid);
				final String ownerName = (offlineOwner.getName() != null)
						? offlineOwner.getName()
						: "\uD83D\uDC64";

				final OfflinePlayer offlineKiller = plugin.getServer().getOfflinePlayer(killerUid);
				final String killerName = (offlineKiller.getName() != null)
						? offlineKiller.getName()
						: "";

				final World world = plugin.getServer().getWorld(resultSet.getString("WorldName"));

				if (world != null)
				{
					DeathChest deathChest = DeathChest.of(chestUid, ownerUid, ownerName, killerUid, killerName,
							world.getUID(), world.getName(), x, y, z, itemCount, placementTime, expirationTime, Instant.EPOCH);

					// add DeathChestObject to results
					if (deathChest instanceof ValidDeathChest validDeathChest)
					{
						results.add(validDeathChest);
					}
				}
			}
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.SELECT_ALL_CHESTS_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
		}

		return results;
	}


	private UUID getUid(final String uidString)
	{
		UUID uid;
		try
		{
			uid = UUID.fromString(uidString);
		}
		catch (IllegalArgumentException exception)
		{
			uid = new UUID(0, 0);
		}
		return uid;
	}

}
