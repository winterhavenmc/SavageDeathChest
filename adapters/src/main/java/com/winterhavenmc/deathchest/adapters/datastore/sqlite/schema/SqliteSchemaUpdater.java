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
import com.winterhavenmc.deathchest.core.ports.datastore.BlockRepository;
import com.winterhavenmc.deathchest.core.ports.datastore.ChestRepository;
import com.winterhavenmc.library.messagebuilder.MessageBuilder;
import org.bukkit.plugin.Plugin;

import java.sql.*;


public sealed interface SqliteSchemaUpdater permits SqliteSchemaUpdaterFromV1, SqliteSchemaUpdaterFromV0, SqliteSchemaUpdaterNoOp
{
	void update();


	static SqliteSchemaUpdater create(final Plugin plugin,
	                                  final MessageBuilder messageBuilder,
	                                  final Connection connection,
	                                  final ChestRepository chestRepository,
	                                  final BlockRepository blockRepository)
	{
		return switch (getSchemaVersion(plugin, messageBuilder, connection))
		{
			case 0 -> new SqliteSchemaUpdaterFromV0(plugin, messageBuilder, connection, chestRepository, blockRepository);
			case 1 -> new SqliteSchemaUpdaterFromV1(plugin, messageBuilder, connection, chestRepository);
			default -> new SqliteSchemaUpdaterNoOp(plugin, messageBuilder);
		};
	}


	private static int getSchemaVersion(final Plugin plugin, final MessageBuilder messageBuilder, final Connection connection)
	{
		int version = 0;
		try (PreparedStatement statement = connection.prepareStatement(SqliteQueries.getQuery("GetUserVersion")))
		{
			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next())
			{
				version = resultSet.getInt(1);
			}
		}
		catch (SQLException sqlException)
		{
			plugin.getLogger().warning(SqliteMessage.SCHEMA_VERSION_ERROR.getLocalizeMessage(messageBuilder.config().locale()));
			plugin.getLogger().warning(sqlException.getLocalizedMessage());
		}

		return version;
	}


	default boolean tableExists(final Connection connection, final String tableName)
	{
		try (PreparedStatement preparedStatement = connection.prepareStatement(SqliteQueries.getQuery("SelectTable")))
		{
			preparedStatement.setString(1, tableName);
			try (ResultSet resultSet = preparedStatement.executeQuery())
			{
				return resultSet.next(); // returns true if a row is found
			}
		}
		catch (SQLException sqlException)
		{
			return false;
		}
	}

}
