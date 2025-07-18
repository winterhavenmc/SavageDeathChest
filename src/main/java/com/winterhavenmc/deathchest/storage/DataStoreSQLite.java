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

package com.winterhavenmc.deathchest.storage;

import com.winterhavenmc.deathchest.PluginMain;
import com.winterhavenmc.deathchest.chests.ChestBlock;
import com.winterhavenmc.deathchest.chests.DeathChestRecord;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * SQLite implementation of Datastore
 * for persistent storage of death chests and chest block objects
 */
final class DataStoreSQLite extends DataStoreAbstract implements DataStore
{
	// reference to main class
	private final PluginMain plugin;

	// database connection object
	private Connection connection;

	// file path for datastore file
	private final String dataFilePath;

	// schema version
	private int schemaVersion;

	private final QueryExecutor queryExecutor = new QueryExecutor();


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 */
	DataStoreSQLite(final PluginMain plugin)
	{
		// set reference to plugin main class
		this.plugin = plugin;

		// set datastore type
		this.type = DataStoreType.SQLITE;

		// set datastore file path
		this.dataFilePath = plugin.getDataFolder() + File.separator + type.getStorageName();
	}


	/**
	 * initialize the database connection and
	 * create table if one doesn't already exist
	 */
	@Override
	public void initialize() throws SQLException, ClassNotFoundException
	{
		// if data store is already initialized, do nothing and return
		if (this.isInitialized())
		{
			plugin.getLogger().info(this + " datastore already initialized.");
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
		statement.executeUpdate(Queries.getQuery("EnableForeignKeys"));

		// update database schema if necessary
		updateSchema();

		// set initialized true
		setInitialized(true);

		// output log message
		plugin.getLogger().info(this + " datastore initialized.");
	}


	private int getStoredSchemaVersion()
	{
		int version = -1;

		try
		{
			final Statement statement = connection.createStatement();

			ResultSet rs = statement.executeQuery(Queries.getQuery("GetUserVersion"));

			while (rs.next())
			{
				version = rs.getInt(1);
			}
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("Could not get schema version for the " + this + " datastore!");
			plugin.getLogger().warning(e.getLocalizedMessage());
			if (plugin.getConfig().getBoolean("debug"))
			{
				e.printStackTrace();
			}
		}
		return version;
	}


	private void updateSchema() throws SQLException
	{
		this.schemaVersion = getStoredSchemaVersion();

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info("Current schema version: " + schemaVersion);
		}

		final Statement statement = connection.createStatement();

//		if (this.schemaVersion == 0)
//		{
//			Collection<DeathChestRecord> existingChestRecords = Collections.emptySet();
//			Collection<ChestBlock> existingBlockRecords = Collections.emptySet();
//
//			ResultSet chestTable = statement.executeQuery(Queries.getQuery("SelectDeathChestTable"));
//			if (chestTable.next())
//			{
//				existingChestRecords = selectAllChestRecords();
//			}
//
//			ResultSet blockTable = statement.executeQuery(Queries.getQuery("SelectDeathBlockTable"));
//			if (blockTable.next())
//			{
//				existingBlockRecords = selectAllBlockRecords();
//			}
//
//			statement.executeUpdate(Queries.getQuery("dropDeathChestTable"));
//			statement.executeUpdate(Queries.getQuery("CreateDeathChestTable"));
//
//			statement.executeUpdate(Queries.getQuery("DropDeathBlockTable"));
//			statement.executeUpdate(Queries.getQuery("CreateDeathBlockTable"));
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
		statement.executeUpdate(Queries.getQuery("CreateDeathChestTable"));

		// execute death block table creation statement
		statement.executeUpdate(Queries.getQuery("CreateDeathBlockTable"));
	}


	/**
	 * Close database connection
	 */
	@Override
	public void close()
	{
		if (isInitialized())
		{
			try
			{
				connection.close();
				plugin.getLogger().info(this + " datastore connection closed.");
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("An error occurred while closing the " +
						this + " datastore connection.");
				plugin.getLogger().warning(e.getMessage());
				if (plugin.getConfig().getBoolean("debug"))
				{
					e.printStackTrace();
				}
			}
			setInitialized(false);
		}
	}


	@Override
	public void sync()
	{
		// no action necessary for this storage type
	}


	@Override
	public boolean delete()
	{
		boolean result = false;
		File dataStoreFile = new File(dataFilePath);
		if (dataStoreFile.exists())
		{
			result = dataStoreFile.delete();
		}
		return result;
	}


	@Override
	public int getChestCount()
	{
		int count = 0;

		try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectChestCount")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();
			if (resultSet.next())
			{
				count = resultSet.getInt("ChestCount");
			}
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("An error occurred while attempting to retrieve a count of chest records from the SQLite datastore.");
			plugin.getLogger().warning(e.getLocalizedMessage());
		}

		return count;
	}


	@Override
	public Collection<ChestBlock> selectAllBlockRecords()
	{
		final Collection<ChestBlock> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllBlocks")))
		{
			// execute sql query
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
				if (world == null)
				{
					// delete all records expired more than 30 days in database that have this invalid world
					deleteOrphanedChests(worldName);
					continue;
				}

				// create chest block object from retrieved record
				ChestBlock chestBlock = new ChestBlock(chestUid, world.getName(), world.getUID(), x, y, z, 0, 0);

				// add DeathChestObject to results set
				results.add(chestBlock);
			}
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("An error occurred while trying to "
					+ "select all block records from the " + this + " datastore.");
			plugin.getLogger().warning(e.getLocalizedMessage());
		}

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(results.size() + " block records selected from the SQLite datastore.");
		}

		return results;
	}


	@Override
	public Collection<DeathChestRecord> selectAllChestRecords()
	{
		final Collection<DeathChestRecord> results = new HashSet<>();

		try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllChests")))
		{
			ResultSet resultSet = preparedStatement.executeQuery();

			while (resultSet.next())
			{
				// convert chest uuid from stored components
				final UUID chestUid = new UUID(resultSet.getLong("ChestUidMsb"), resultSet.getLong("ChestUidLsb"));

				// convert owner uuid from stored components
				final UUID ownerUid = new UUID(resultSet.getLong("OwnerUidMsb"), resultSet.getLong("OwnerUidLsb"));

				// convert killer uuid from stored components
				final UUID killerUid = new UUID(resultSet.getLong("KillerUidMsb"), resultSet.getLong("KillerUidLsb"));

				// get protection expiration time
				final long protectionExpirationTime = resultSet.getLong("ProtectionExpirationTime");

				// get owner name string
				final String ownerName = resultSet.getString("OwnerName");

				// get killer name string
				final String killerName = resultSet.getString("KillerName");

				// set other fields in deathChestBlock from database fields
				int itemCount = resultSet.getInt("ItemCount");
				long placementTime = resultSet.getLong("PlacementTime");
				long expirationTime = resultSet.getLong("ExpirationTime");

				final UUID worldUid = new UUID(resultSet.getLong("WorldUidMsb"), resultSet.getLong("WorldUidLsb"));
				String worldName = resultSet.getString("WorldName");
				int locationX = resultSet.getInt("LocationX");
				int locationY = resultSet.getInt("LocationY");
				int locationZ = resultSet.getInt("LocationZ");

				DeathChestRecord deathChest = new DeathChestRecord(
						chestUid,
						ownerUid,
						ownerName,
						killerUid,
						killerName,
						worldUid,
						worldName,
						locationX,
						locationY,
						locationZ,
						itemCount,
						Instant.ofEpochMilli(placementTime),
						Instant.ofEpochMilli(expirationTime),
						Instant.ofEpochMilli(protectionExpirationTime));

				// add DeathChestObject to results set
				results.add(deathChest);
			}
		}
		catch (SQLException exception)
		{
			plugin.getLogger().warning("An error occurred while trying to " +
					"select all chest records from the SQLite datastore.");
			plugin.getLogger().warning(exception.getMessage());
		}

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(results.size() + " chest records selected from the SQLite datastore.");
		}
		return results;
	}


	@Override
	public synchronized int insertChestRecords(final Collection<DeathChestRecord> deathChests)
	{
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				insertChestRecordsSync(deathChests);
			}
		}.runTaskAsynchronously(plugin);

		return deathChests.size();
	}


	public synchronized int insertChestRecordsSync(final Collection<DeathChestRecord> deathChests)
	{
		int count = 0;

		for (DeathChestRecord deathChest : deathChests)
		{
			if (deathChest == null)
			{
				continue;
			}

			try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("InsertChestRecord")))
			{
				count += queryExecutor.insertChest(deathChest, preparedStatement);
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("An error occurred while inserting a DeathChest into the SQLite datastore.");
				plugin.getLogger().warning(e.getMessage());
			}

			// insert chest blocks into datastore
			insertBlockRecords(plugin.chestManager.getBlocks(deathChest.chestUid()));
		}

		// output debugging information
		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(count + " chest records inserted into the " +
					"SQLite datastore.");
		}

		return deathChests.size();
	}


	@Override
	synchronized public int insertBlockRecords(final Collection<ChestBlock> blockRecords)
	{
		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				insertBlockRecordsSync(blockRecords);
			}
		}.runTaskAsynchronously(plugin);

		return blockRecords.size();
	}


	int insertBlockRecordsSync(final Collection<ChestBlock> blockRecords)
	{
		int count = 0;

		for (ChestBlock blockRecord : blockRecords)
		{

			// if blockRecord is null, skip to next record in collection
			if (blockRecord == null)
			{
				continue;
			}

			try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("InsertBlockRecord")))
			{
				count += queryExecutor.insertBlock(blockRecord, preparedStatement);
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("An error occurred while inserting a death chest block into the SQLite datastore.");
				plugin.getLogger().warning(e.getMessage());
			}
		}

		// log debug info
		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(count + " block records inserted into the SQLite datastore.");
		}

		return count;
	}


	@Override
	synchronized public void deleteChestRecord(final DeathChestRecord deathChest)
	{
		// if passed deathChest is null, do nothing and return
		if (deathChest == null)
		{
			return;
		}

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("DeleteChestByUUID")))
				{
					int rowsAffected = queryExecutor.deleteChest(deathChest, preparedStatement);

					// output debugging information
					if (plugin.getConfig().getBoolean("debug"))
					{
						plugin.getLogger().info(rowsAffected + " chest records deleted from the SQLite datastore.");
					}
				}
				catch (SQLException sqlException)
				{
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a chest record from the SQLite datastore.");
					plugin.getLogger().warning(sqlException.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);

	}


	@Override
	synchronized public void deleteBlockRecord(final ChestBlock chestBlock)
	{
		// if passed chestBlock is null, do nothing and return
		if (chestBlock == null)
		{
			return;
		}

		new BukkitRunnable()
		{
			@Override
			public void run()
			{
				try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("DeleteBlockByLocation")))
				{
					int rowsAffected = queryExecutor.DeleteBlock(chestBlock, preparedStatement);

					// output debugging information
					if (plugin.getConfig().getBoolean("debug"))
					{
						plugin.getLogger().info(rowsAffected + " block records deleted from the SQLite datastore.");
					}
				}
				catch (SQLException sqlException)
				{
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a block record from the SQLite datastore.");
					plugin.getLogger().warning(sqlException.getMessage());
				}
			}
		}.runTaskAsynchronously(plugin);
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

		try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("DeleteOrphanedChests")))
		{
			int rowsAffected = queryExecutor.deleteOrphanedChests(worldName, pastDueTime, preparedStatement);

			if (plugin.getConfig().getBoolean("debug"))
			{
				plugin.getLogger().info(rowsAffected + " rows deleted.");
			}
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning("An error occurred while attempting to delete orphaned chests from the " +
					"SQLite datastore.");
			plugin.getLogger().warning(sqlException.getMessage());
		}
	}

}
