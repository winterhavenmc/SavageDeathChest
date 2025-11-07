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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite;

import com.winterhavenmc.deathchest.models.chestblock.ChestBlock;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;


public final class SqliteBlockRowMapper
{
	public ChestBlock map(final Plugin plugin, final ResultSet resultSet) throws SQLException
	{
		String worldName = resultSet.getString("WorldName");
		final UUID chestUid = new UUID(resultSet.getLong("ChestUidMsb"), resultSet.getLong("ChestUidLsb"));
		final UUID worldUid = new UUID(resultSet.getLong("WorldUidMsb"), resultSet.getLong("WorldUidLsb"));
		final World world = plugin.getServer().getWorld(worldUid);
		final int x = resultSet.getInt("X");
		final int y = resultSet.getInt("Y");
		final int z = resultSet.getInt("Z");

		if (world != null)
		{
			worldName = world.getName();
		}

		return ChestBlock.of(chestUid, worldName, worldUid, x, y, z);
	}

}
