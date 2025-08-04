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

import com.winterhavenmc.deathchest.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.ports.datastore.ChestRepository;
import com.winterhavenmc.deathchest.ports.datastore.ConnectionProvider;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.concurrent.TimeUnit;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
public final class SqliteConnectionProvider implements ConnectionProvider
{
	private final Plugin plugin;
	private final String dataFilePath;
	private Connection connection;
	private boolean initialized;

	private ChestRepository chestRepository;
	private BlockRepository blockRepository;

	private final SqliteChestQueryExecutor chestQueryHelper = new SqliteChestQueryExecutor();


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 */
	public SqliteConnectionProvider(final Plugin plugin)
	{
		this.plugin = plugin;
		this.dataFilePath = plugin.getDataFolder() + File.separator + "deathchests.db";
	}


	/**
	 * initialize the database connection and
	 * create table if one doesn't already exist
	 */
	@Override
	public void connect() throws SQLException, ClassNotFoundException
	{
		// if data store is already initialized, do nothing and return
		if (initialized)
		{
			logger.info(SqliteMessage.ALREADY_INITIALIZED_NOTICE.getLocalizeMessage(localeProvider.getLocale()));
			return;
		}

		// register the driver
		final String jdbcDriverName = "org.sqlite.JDBC";

		Class.forName(jdbcDriverName);

		// create database url
		String jdbc = "jdbc:sqlite";
		String dbUrl = jdbc + ":" + dataFilePath;

		// create a database connection
		connection = DriverManager.getConnection(dbUrl);

		final Statement statement = connection.createStatement();

		// enable foreign keys
		statement.executeUpdate(SQLiteQueries.getQuery("EnableForeignKeys"));

		// update database schema if necessary
		updateSchema();

		// set initialized true
		initialized = true;

		// instantiate datastore adapters
		chestRepository = new SQLiteChestRepository(plugin.getLogger(), connection);
		blockRepository = new SQLiteBlockRepository(plugin, connection);

		// output log message
		plugin.getLogger().info("Datastore initialized.");
	}


	/**
	 * Close database connection
	 */
	@Override
	public void close()
	{
		if (initialized)
		{
			try
			{
				connection.close();
				plugin.getLogger().info(this + " datastore connection closed.");
			}
			catch (SQLException sqlException)
			{
				logger.warning(SqliteMessage.CLOSE_DATASTORE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
				logger.warning(sqlException.getMessage());
			}
			initialized = true;
		}
	}


	@Override
	public ChestRepository deathChests()
	{
		return this.chestRepository;
	}


	@Override
	public BlockRepository chestBlocks()
	{
		return this.blockRepository;
	}


	private int getStoredSchemaVersion()
	{
		int version = -1;

		try
		{
			final Statement statement = connection.createStatement();

			ResultSet rs = statement.executeQuery(SQLiteQueries.getQuery("GetUserVersion"));

			while (rs.next())
			{
				version = rs.getInt(1);
			}
		}
		catch (SQLException sqlException)
		{
			logger.warning(SqliteMessage.CREATE_CHEST_TABLE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			logger.warning(sqlException.getLocalizedMessage());
		}
		return version;
	}


	private void updateSchema() throws SQLException
	{
		int schemaVersion = getStoredSchemaVersion();

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info("Current schema version: " + schemaVersion);
		}

		final Statement statement = connection.createStatement();

//		if (this.schemaVersion == 0)
//		{
//			Collection<DeathChestRecord> existingChestRecords = Collections.emptySet();
//			Collection<LegacyChestBlock> existingBlockRecords = Collections.emptySet();
//
//			ResultSet chestTable = statement.executeQuery(SQLiteQueries.getQuery("SelectDeathChestTable"));
//			if (chestTable.next())
//			{
//				existingChestRecords = selectAllChestRecords();
//			}
//
//			ResultSet blockTable = statement.executeQuery(SQLiteQueries.getQuery("SelectDeathBlockTable"));
//			if (blockTable.next())
//			{
//				existingBlockRecords = selectAllBlockRecords();
//			}
//
//			statement.executeUpdate(SQLiteQueries.getQuery("dropDeathChestTable"));
//			statement.executeUpdate(SQLiteQueries.getQuery("CreateDeathChestTable"));
//
//			statement.executeUpdate(SQLiteQueries.getQuery("DropDeathBlockTable"));
//			statement.executeUpdate(SQLiteQueries.getQuery("CreateDeathBlockTable"));
//
//
//			int chestCount = insertChestRecordsSync(existingChestRecords);
//			plugin.getLogger().info(chestCount + " death chest records migrated to schema v1 in the " +
//					this + " datastore.");
//
//			int blockCount = insertBlockRecordsSync(existingBlockRecords);
//			plugin.getLogger().info(blockCount + " death block records migrated to schema v1 in the " +
//					this + " datastore.");
//
//			// update schema version in database
//			statement.executeUpdate("PRAGMA user_version = 1");
//
//			// update schema version field
//			schemaVersion = 1;
//		}

		// execute death chest table creation statement
		statement.executeUpdate(SQLiteQueries.getQuery("CreateDeathChestTable"));

		// execute death block table creation statement
		statement.executeUpdate(SQLiteQueries.getQuery("CreateDeathBlockTable"));
	}


	/**
	 * Delete orphaned chests in nonexistent world {@code worldName}
	 *
	 * @param worldName the world name of orphaned chests to delete
	 */
	private void deleteOrphanedChests(final String worldName)
	{
		// pastDueTime = current time in milliseconds - 30 days
		final long pastDueTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

		try (PreparedStatement preparedStatement = connection.prepareStatement(SQLiteQueries.getQuery("DeleteOrphanedChests")))
		{
			int rowsAffected = chestQueryHelper.deleteOrphanedChests(worldName, pastDueTime, preparedStatement);

			if (plugin.getConfig().getBoolean("debug"))
			{
				plugin.getLogger().info(rowsAffected + " rows deleted.");
			}
		}
		catch (SQLException sqlException)
		{
			logger.warning(SqliteMessage.CREATE_BLOCK_TABLE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
			logger.warning(sqlException.getLocalizedMessage());
		}
	}

}
