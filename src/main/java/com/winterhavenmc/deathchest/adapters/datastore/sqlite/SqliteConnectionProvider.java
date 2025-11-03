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

import com.winterhavenmc.deathchest.adapters.datastore.sqlite.schema.SqliteSchemaUpdater;
import com.winterhavenmc.deathchest.core.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ChestRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ConnectionProvider;
import com.winterhavenmc.library.messagebuilder.resources.configuration.LocaleProvider;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.Locale;
import java.util.logging.Logger;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
public final class SqliteConnectionProvider implements ConnectionProvider
{
	private final Plugin plugin;
	private final Logger logger;
	private final LocaleProvider localeProvider;
	private final String dataFilePath;
	private Connection connection;
	private boolean initialized;

	private ChestRepository chestRepository;
	private BlockRepository blockRepository;


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 */
	public SqliteConnectionProvider(final Plugin plugin)
	{
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.localeProvider = LocaleProvider.create(plugin);
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

		// instantiate datastore adapters
		chestRepository = new SQLiteChestRepository(plugin, connection);
		blockRepository = new SQLiteBlockRepository(plugin, connection);

		// enable foreign keys
		enableForeignKeys(connection, localeProvider.getLocale());

		// Update schema
		SqliteSchemaUpdater schemaUpdater = SqliteSchemaUpdater.create(plugin, connection, localeProvider, chestRepository, blockRepository);
		schemaUpdater.update();

		// create tables if necessary
		createChestTable(connection, localeProvider.getLocale());
		createBlockTable(connection, localeProvider.getLocale());

		// set initialized true
		initialized = true;

		// output log message
		logger.info(SqliteMessage.INITIALIZE_DATASTORE_NOTICE.getLocalizeMessage(localeProvider.getLocale()));
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
				logger.info(SqliteMessage.CONNECTION_CLOSED_NOTICE.getLocalizeMessage(localeProvider.getLocale()));
			}
			catch (SQLException sqlException)
			{
				logger.warning(SqliteMessage.CLOSE_DATASTORE_ERROR.getLocalizeMessage(localeProvider.getLocale()));
				logger.warning(sqlException.getMessage());
			}
			initialized = false;
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


	private void enableForeignKeys(final Connection connection, final Locale locale)
	{
		try (final Statement statement = connection.createStatement())
		{
			statement.executeUpdate(SqliteQueries.getQuery("EnableForeignKeys"));
		}
		catch (SQLException sqlException)
		{
			logger.warning(SqliteMessage.ENABLE_FOREIGN_KEYS_ERROR.getLocalizeMessage(locale));
		}
	}


	private void createChestTable(final Connection connection, final Locale locale)
	{
		try (final Statement statement = connection.createStatement())
		{
			statement.executeUpdate(SqliteQueries.getQuery("CreateChestTable"));
		}
		catch (SQLException sqlException)
		{
			logger.warning(SqliteMessage.CREATE_CHEST_TABLE_ERROR.getLocalizeMessage(locale));
			logger.warning(sqlException.getLocalizedMessage());
		}
	}


	private void createBlockTable(final Connection connection, final Locale locale)
	{
		try (final Statement statement = connection.createStatement())
		{
			statement.executeUpdate(SqliteQueries.getQuery("CreateBlockTable"));
		}
		catch (SQLException sqlException)
		{
			logger.warning(SqliteMessage.CREATE_BLOCK_TABLE_ERROR.getLocalizeMessage(locale));
			logger.warning(sqlException.getLocalizedMessage());
		}
	}

}
