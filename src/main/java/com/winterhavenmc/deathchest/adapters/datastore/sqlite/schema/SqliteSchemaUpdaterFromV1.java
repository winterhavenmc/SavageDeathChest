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
import com.winterhavenmc.deathchest.plugin.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.plugin.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.plugin.ports.datastore.ChestRepository;
import com.winterhavenmc.library.messagebuilder.resources.configuration.LocaleProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.sql.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


public final class SqliteSchemaUpdaterFromV1 implements SqliteSchemaUpdater
{
	private final Plugin plugin;
	private final Connection connection;
	private final ChestRepository chestRepository;
	private final LocaleProvider localeProvider;


	SqliteSchemaUpdaterFromV1(final Plugin plugin,
	                          final Connection connection,
	                          final LocaleProvider localeProvider,
	                          final ChestRepository chestRepository)
	{
		this.plugin = plugin;
		this.connection = connection;
		this.chestRepository = chestRepository;
		this.localeProvider = localeProvider;
	}


	public void update()
	{
		if (tableExists(connection, "Chests"))
		{
			migrateChestRecords();
		}
	}


	private void migrateChestRecords()
	{
		try (Statement statement = connection.createStatement())
		{
			Set<ValidDeathChest> existingChests = selectAllChestRecords(plugin, connection);

			statement.executeUpdate(SqliteQueries.getQuery("dropChestTable"));
			statement.executeUpdate(SqliteQueries.getQuery("CreateChestTable"));
			statement.executeUpdate("PRAGMA user_version = 2");

			int chestCount = chestRepository.save(existingChests);
			plugin.getLogger().info(SqliteMessage.SCHEMA_CHESTS_MIGRATED_NOTICE.getLocalizeMessage(localeProvider.getLocale(), chestCount));
//			plugin.getLogger().info(chestCount + " death chest records migrated to schema v2 in the SQLite datastore.");
		}
		catch(SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.SCHEMA_UPDATE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
		}
	}


	private Set<ValidDeathChest> selectAllChestRecords(final Plugin plugin, final Connection connection) throws SQLException
	{
		Set<ValidDeathChest> existingChests = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectAllChestsV1")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				if (mapChestRow(resultSet) instanceof ValidDeathChest validDeathChest)
				{
					existingChests.add(validDeathChest);
				}
			}
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.SELECT_ALL_CHESTS_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
		}

		return existingChests;
	}


	private DeathChest mapChestRow(final ResultSet resultSet) throws SQLException
	{
		final UUID chestUid = new UUID(resultSet.getLong("ChestUidMsb"), resultSet.getLong("ChestUidLsb"));
		final UUID ownerUid = new UUID(resultSet.getLong("OwnerUidMsb"), resultSet.getLong("OwnerUidLsb"));
		final UUID killerUid = new UUID(resultSet.getLong("KillerUidMsb"), resultSet.getLong("KillerUidLsb"));

		final OfflinePlayer player = plugin.getServer().getOfflinePlayer(ownerUid);
		final String ownerName = (player.getName() != null)
				? player.getName()
				: "\uD83D\uDC64";

		final OfflinePlayer killer = plugin.getServer().getOfflinePlayer(killerUid);
		final String killerName = (killer.getName() != null)
				? killer.getName()
				: "";

		final int itemCount = resultSet.getInt("ItemCount");
		final long placementTime = resultSet.getLong("PlacementTime");
		final long expirationTime = resultSet.getLong("ExpirationTime");
		final long protectionExpirationTime = resultSet.getLong("ProtectionExpirationTime");

		final UUID worldUid = new UUID(resultSet.getLong("WorldUidMsb"), resultSet.getLong("WorldUidLsb"));
		final String worldName = resultSet.getString("WorldName");
		final int x = resultSet.getInt("X");
		final int y = resultSet.getInt("Y");
		final int z = resultSet.getInt("Z");

		return DeathChest.of(chestUid, ownerUid, ownerName, killerUid, killerName,
				worldUid, worldName, x, y, z, itemCount, Instant.ofEpochMilli(placementTime),
				Instant.ofEpochMilli(expirationTime), Instant.ofEpochMilli(protectionExpirationTime));
	}

}
