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

import com.winterhavenmc.deathchest.models.chestblock.ChestBlock;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public final class SqliteBlockRowMapperV0
{
	public ChestBlock map(final Plugin plugin, final ResultSet resultSet) throws SQLException
	{
		UUID worldUid = new UUID(0, 0);

		String worldName = resultSet.getString("WorldName");
		final UUID chestUid = getUid(resultSet.getString("ChestUid"));
		final int x = resultSet.getInt("X");
		final int y = resultSet.getInt("Y");
		final int z = resultSet.getInt("Z");

		final World world = plugin.getServer().getWorld(worldName);

		if (world != null)
		{
			worldName = world.getName();
			worldUid = world.getUID();
		}

		return ChestBlock.of(chestUid, worldName, worldUid, x, y, z);
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
