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

import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public final class SQLiteChestQueryHelper
{
	static int getChestCount(PreparedStatement preparedStatement) throws SQLException
	{
		int count = 0;

		ResultSet resultSet = preparedStatement.executeQuery();
		if (resultSet.next())
		{
			count = resultSet.getInt("ChestCount");
		}
		return count;
	}

	public int insertChest(final ValidDeathChest validDeathChest, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(   1, validDeathChest.chestUid().getMostSignificantBits());
		preparedStatement.setLong(   2, validDeathChest.chestUid().getLeastSignificantBits());
		preparedStatement.setLong(   3, validDeathChest.ownerUid().getMostSignificantBits());
		preparedStatement.setLong(   4, validDeathChest.ownerUid().getLeastSignificantBits());
		preparedStatement.setString( 5, validDeathChest.ownerName());
		preparedStatement.setLong(   6, validDeathChest.killerUid().getMostSignificantBits());
		preparedStatement.setLong(   7, validDeathChest.killerUid().getLeastSignificantBits());
		preparedStatement.setString( 8, validDeathChest.killerName());
		preparedStatement.setLong(   9, validDeathChest.worldUid().getMostSignificantBits());
		preparedStatement.setLong(  10, validDeathChest.worldUid().getLeastSignificantBits());
		preparedStatement.setString(11, validDeathChest.worldName());
		preparedStatement.setInt(   12, validDeathChest.locationX());
		preparedStatement.setInt(   13, validDeathChest.locationY());
		preparedStatement.setInt(   14, validDeathChest.locationZ());
		preparedStatement.setInt(   15, validDeathChest.itemCount());
		preparedStatement.setLong(  16, validDeathChest.placementTime().toEpochMilli());
		preparedStatement.setLong(  17, validDeathChest.expirationTime().toEpochMilli());
		preparedStatement.setLong(  18, validDeathChest.protectionTime().toEpochMilli());
		return preparedStatement.executeUpdate();
	}


	public int deleteOrphanedChests(final String worldName,
	                                final long pastDueTime,
	                                final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setString(1, worldName);
		preparedStatement.setLong(  2, pastDueTime);
		return preparedStatement.executeUpdate();
	}


	public int deleteChest(final ValidDeathChest validDeathChest, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(1, validDeathChest.chestUid().getMostSignificantBits());
		preparedStatement.setLong(2, validDeathChest.chestUid().getLeastSignificantBits());
		return preparedStatement.executeUpdate();
	}
}
