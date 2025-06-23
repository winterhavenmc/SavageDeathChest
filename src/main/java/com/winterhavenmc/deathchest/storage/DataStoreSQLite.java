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

		try
		{
			PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectChestCount"));
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next())
			{
				count = rs.getInt("ChestCount");
			}
		} catch (SQLException e)
		{
			plugin.getLogger().warning("An error occurred while attempting to retrieve a count of chest records from the " + this + " datastore.");
			plugin.getLogger().warning(e.getLocalizedMessage());
		}

		return count;
	}


	@Override
	public Collection<ChestBlock> selectAllBlockRecords()
	{
		final Collection<ChestBlock> results = new HashSet<>();

		try
		{
			PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllBlocks"));

			// execute sql query
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next())
			{
				final String worldName = rs.getString("WorldName");
				final int x = rs.getInt("X");
				final int y = rs.getInt("Y");
				final int z = rs.getInt("Z");

				final UUID chestUid = new UUID(rs.getLong("ChestUidMsb"), rs.getLong("chestUidLsb"));
				final UUID worldUid = new UUID(rs.getLong("WorldUidMsb"), rs.getLong("WorldUidLsb"));

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
			if (plugin.getConfig().getBoolean("debug"))
			{
				e.printStackTrace();
			}
		}

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(results.size() + " block records selected from the " + this + " datastore.");
		}

		return results;
	}


	@Override
	public Collection<DeathChestRecord> selectAllChestRecords()
	{
		final Collection<DeathChestRecord> results = new HashSet<>();

		try
		{
			PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("SelectAllChests"));
			ResultSet rs = preparedStatement.executeQuery();

			while (rs.next())
			{

				// convert chest uuid from stored components
				final UUID chestUid = new UUID(rs.getLong("ChestUidMsb"), rs.getLong("ChestUidLsb"));

				// convert owner uuid from stored components
				final UUID ownerUid = new UUID(rs.getLong("OwnerUidMsb"), rs.getLong("OwnerUidLsb"));

				// convert killer uuid from stored components
				final UUID killerUid = new UUID(rs.getLong("KillerUidMsb"), rs.getLong("KillerUidLsb"));

				// get protection expiration time
				final long protectionExpirationTime = rs.getLong("ProtectionExpirationTime");

				// get owner name string
				final String ownerName = rs.getString("OwnerName");

				// get killer name string
				final String killerName = rs.getString("KillerName");

				// set other fields in deathChestBlock from database fields
				int itemCount = rs.getInt("ItemCount");
				long placementTime = rs.getLong("PlacementTime");
				long expirationTime = rs.getLong("ExpirationTime");

				final UUID worldUid = new UUID(rs.getLong("WorldUidMsb"), rs.getLong("WorldUidLsb"));
				String worldName = rs.getString("WorldName");
				int locationX = rs.getInt("LocationX");
				int locationY = rs.getInt("LocationY");
				int locationZ = rs.getInt("LocationZ");

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
		catch (SQLException e)
		{
			plugin.getLogger().warning("An error occurred while trying to " +
					"select all chest records from the " + this + " datastore.");
			plugin.getLogger().warning(e.getMessage());

			if (plugin.getConfig().getBoolean("debug"))
			{
				e.printStackTrace();
			}
		}

		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(results.size() + " chest records selected from the " + this + " datastore.");
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
			// if deathChest is null, skip to next
			if (deathChest == null)
			{
				continue;
			}

			try
			{
				// create prepared statement
				PreparedStatement preparedStatement = connection.prepareStatement(Queries.getQuery("InsertChestRecord"));

				preparedStatement.setLong(1, deathChest.chestUid().getMostSignificantBits());
				preparedStatement.setLong(2, deathChest.chestUid().getLeastSignificantBits());
				preparedStatement.setLong(3, deathChest.ownerUid().getMostSignificantBits());
				preparedStatement.setLong(4, deathChest.ownerUid().getLeastSignificantBits());
				preparedStatement.setString(5, deathChest.ownerName());
				preparedStatement.setLong(6, deathChest.killerUid().getMostSignificantBits());
				preparedStatement.setLong(7, deathChest.killerUid().getLeastSignificantBits());
				preparedStatement.setString(8, deathChest.killerName());
				preparedStatement.setLong(9, deathChest.worldUid().getMostSignificantBits());
				preparedStatement.setLong(10, deathChest.worldUid().getLeastSignificantBits());
				preparedStatement.setString(11, deathChest.worldName());
				preparedStatement.setInt(12, deathChest.locationX());
				preparedStatement.setInt(13, deathChest.locationY());
				preparedStatement.setInt(14, deathChest.locationZ());
				preparedStatement.setInt(15, deathChest.itemCount());
				preparedStatement.setLong(16, deathChest.placementTime().toEpochMilli());
				preparedStatement.setLong(17, deathChest.expirationTime().toEpochMilli());
				preparedStatement.setLong(18, deathChest.protectionTime().toEpochMilli());

				// execute prepared statement
				int rowsAffected = preparedStatement.executeUpdate();

				count += rowsAffected;
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("An error occurred while inserting a DeathChest into the " +
						"SQLite datastore.");
				plugin.getLogger().warning(e.getMessage());
				if (plugin.getConfig().getBoolean("debug"))
				{
					e.printStackTrace();
				}
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

			try
			{
				// create prepared statement
				PreparedStatement preparedStatement =
						connection.prepareStatement(Queries.getQuery("InsertBlockRecord"));

				preparedStatement.setLong(1, blockRecord.getChestUid().getMostSignificantBits());
				preparedStatement.setLong(2, blockRecord.getChestUid().getLeastSignificantBits());
				preparedStatement.setString(3, blockRecord.getWorldName());
				preparedStatement.setLong(4, blockRecord.getWorldUid().getMostSignificantBits());
				preparedStatement.setLong(5, blockRecord.getWorldUid().getLeastSignificantBits());
				preparedStatement.setInt(6, blockRecord.getX());
				preparedStatement.setInt(7, blockRecord.getY());
				preparedStatement.setInt(8, blockRecord.getZ());

				// execute prepared statement
				int rowsAffected = preparedStatement.executeUpdate();

				count += rowsAffected;
			}
			catch (SQLException e)
			{
				plugin.getLogger().warning("An error occurred while "
						+ "inserting a death chest block into the SQLite datastore.");
				plugin.getLogger().warning(e.getMessage());
				if (plugin.getConfig().getBoolean("debug"))
				{
					e.printStackTrace();
				}
			}
		}

		// output debugging information
		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info(count + " block records inserted into the " +
					"SQLite datastore.");
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
				try
				{
					// create prepared statement
					PreparedStatement preparedStatement =
							connection.prepareStatement(Queries.getQuery("DeleteChestByUUID"));

					preparedStatement.setLong(1, deathChest.chestUid().getMostSignificantBits());
					preparedStatement.setLong(2, deathChest.chestUid().getLeastSignificantBits());

					// execute prepared statement
					int rowsAffected = preparedStatement.executeUpdate();

					// output debugging information
					if (plugin.getConfig().getBoolean("debug"))
					{
						plugin.getLogger().info(rowsAffected + " chest records deleted from the SQLite datastore.");
					}
				} catch (SQLException e)
				{
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a chest record from the SQLite datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.getConfig().getBoolean("debug"))
					{
						e.printStackTrace();
					}
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
				try
				{
					// create prepared statement
					PreparedStatement preparedStatement =
							connection.prepareStatement(Queries.getQuery("DeleteBlockByLocation"));

					preparedStatement.setLong(1, chestBlock.getWorldUid().getMostSignificantBits());
					preparedStatement.setLong(2, chestBlock.getWorldUid().getLeastSignificantBits());
					preparedStatement.setInt(3, chestBlock.getX());
					preparedStatement.setInt(4, chestBlock.getY());
					preparedStatement.setInt(5, chestBlock.getZ());

					// execute prepared statement
					int rowsAffected = preparedStatement.executeUpdate();

					// output debugging information
					if (plugin.getConfig().getBoolean("debug"))
					{
						plugin.getLogger().info(rowsAffected + " block records deleted from the SQLite datastore.");
					}
				} catch (SQLException e)
				{
					plugin.getLogger().warning("An error occurred while attempting to "
							+ "delete a record from the SQLite datastore.");
					plugin.getLogger().warning(e.getMessage());
					if (plugin.getConfig().getBoolean("debug"))
					{
						e.printStackTrace();
					}
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

		try
		{
			// create prepared statement
			PreparedStatement preparedStatement =
					connection.prepareStatement(Queries.getQuery("DeleteOrphanedChests"));

			preparedStatement.setString(1, worldName);
			preparedStatement.setLong(2, pastDueTime);

			// execute prepared statement
			int rowsAffected = preparedStatement.executeUpdate();

			// output debugging information
			if (plugin.getConfig().getBoolean("debug"))
			{
				plugin.getLogger().info(rowsAffected + " rows deleted.");
			}
		}
		catch (SQLException e)
		{
			plugin.getLogger().warning("An error occurred while attempting to delete orphaned chests from the " +
					this + " datastore.");
			plugin.getLogger().warning(e.getMessage());
			if (plugin.getConfig().getBoolean("debug"))
			{
				e.printStackTrace();
			}
		}
	}

}
