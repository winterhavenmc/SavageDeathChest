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

import com.winterhavenmc.deathchest.adapters.datastore.sqlite.schema.SqliteSchemaUpdater;
import com.winterhavenmc.deathchest.core.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ChestRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ConnectionProvider;
import com.winterhavenmc.library.messagebuilder.MessageBuilder;
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
	private final MessageBuilder messageBuilder;
	private final String dataFilePath;
	private Connection connection;
	private boolean initialized;

	private ChestRepository chestRepository;
	private BlockRepository blockRepository;


	/**
	 * Class constructor
	 */
	public SqliteConnectionProvider(final Plugin plugin, final MessageBuilder messageBuilder)
	{
		this.plugin = plugin;
		this.logger = plugin.getLogger();
		this.messageBuilder = messageBuilder;
		this.dataFilePath = plugin.getDataFolder() + File.separator + "deathchests.db";
	}


	/**
	 * initialize the database connection and
	 * create table if one doesn't already exist
	 */
	@Override
	public void connect()
	{
		// if data store is already initialized, do nothing and return
		if (initialized)
		{
			logger.info(SqliteMessage.DATASTORE_INITIALIZED_PREVIOUSLY_NOTICE.getLocalizeMessage(messageBuilder.config().locale()));
			return;
		}

		// register the driver
		final String jdbcDriverName = "org.sqlite.JDBC";

		try
		{
			Class.forName(jdbcDriverName);
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}

		// create database url
		String jdbc = "jdbc:sqlite";
		String dbUrl = jdbc + ":" + dataFilePath;

		// create a database connection
		try
		{
			connection = DriverManager.getConnection(dbUrl);
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}

		// instantiate datastore adapters
		chestRepository = new SQLiteChestRepository(plugin, messageBuilder, connection);
		blockRepository = new SQLiteBlockRepository(plugin, messageBuilder, connection);

		// enable foreign keys
		enableForeignKeys(connection, messageBuilder.config().locale());

		// Update schema
		SqliteSchemaUpdater schemaUpdater = SqliteSchemaUpdater.create(plugin, messageBuilder, connection, chestRepository, blockRepository);
		schemaUpdater.update();

		// create tables if necessary
		createChestTable(connection, messageBuilder.config().locale());
		createBlockTable(connection, messageBuilder.config().locale());

		// set initialized true
		initialized = true;

		// output log message
		logger.info(SqliteMessage.DATASTORE_INITIALIZE_NOTICE.getLocalizeMessage(messageBuilder.config().locale()));
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
				logger.info(SqliteMessage.DATASTORE_CLOSE_CONNECTION_NOTICE.getLocalizeMessage(messageBuilder.config().locale()));
			}
			catch (SQLException sqlException)
			{
				logger.warning(SqliteMessage.DATASTORE_CLOSE_CONNECTION_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
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
			logger.warning(SqliteMessage.PRAGMA_ENABLE_FOREIGN_KEYS_ERROR.getLocalizeMessage(locale));
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
			logger.warning(SqliteMessage.DATASTORE_CREATE_CHEST_TABLE_ERROR.getLocalizeMessage(locale));
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
			logger.warning(SqliteMessage.DATASTORE_CREATE_BLOCK_TABLE_ERROR.getLocalizeMessage(locale));
			logger.warning(sqlException.getLocalizedMessage());
		}
	}

}
