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

import com.winterhavenmc.deathchest.chests.ChestBlock;

import java.sql.PreparedStatement;
import java.sql.SQLException;


public final class SQLiteBlockQueryHelper
{
	public int insertBlock(final ChestBlock blockRecord, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(  1, blockRecord.getChestUid().getMostSignificantBits());
		preparedStatement.setLong(  2, blockRecord.getChestUid().getLeastSignificantBits());
		preparedStatement.setString(3, blockRecord.getWorldName());
		preparedStatement.setLong(  4, blockRecord.getWorldUid().getMostSignificantBits());
		preparedStatement.setLong(  5, blockRecord.getWorldUid().getLeastSignificantBits());
		preparedStatement.setInt(   6, blockRecord.getX());
		preparedStatement.setInt(   7, blockRecord.getY());
		preparedStatement.setInt(   8, blockRecord.getZ());
		return preparedStatement.executeUpdate();
	}


	public int DeleteBlock(final ChestBlock chestBlock, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(1, chestBlock.getWorldUid().getMostSignificantBits());
		preparedStatement.setLong(2, chestBlock.getWorldUid().getLeastSignificantBits());
		preparedStatement.setInt( 3, chestBlock.getX());
		preparedStatement.setInt( 4, chestBlock.getY());
		preparedStatement.setInt( 5, chestBlock.getZ());
		return preparedStatement.executeUpdate();
	}

}
