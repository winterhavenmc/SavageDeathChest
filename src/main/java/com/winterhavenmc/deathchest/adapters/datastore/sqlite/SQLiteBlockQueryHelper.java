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

import com.winterhavenmc.deathchest.models.chestblock.ValidChestBlock;

import java.sql.PreparedStatement;
import java.sql.SQLException;


public final class SQLiteBlockQueryHelper
{
	public int insertBlock(final ValidChestBlock validChestBlock, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(  1, validChestBlock.getChestUid().getMostSignificantBits());
		preparedStatement.setLong(  2, validChestBlock.getChestUid().getLeastSignificantBits());
		preparedStatement.setString(3, validChestBlock.getWorldName());
		preparedStatement.setLong(  4, validChestBlock.getWorldUid().getMostSignificantBits());
		preparedStatement.setLong(  5, validChestBlock.getWorldUid().getLeastSignificantBits());
		preparedStatement.setInt(   6, validChestBlock.getX());
		preparedStatement.setInt(   7, validChestBlock.getY());
		preparedStatement.setInt(   8, validChestBlock.getZ());
		return preparedStatement.executeUpdate();
	}


	public int DeleteBlock(final ValidChestBlock validChestBlock, final PreparedStatement preparedStatement) throws SQLException
	{
		preparedStatement.setLong(1, validChestBlock.getWorldUid().getMostSignificantBits());
		preparedStatement.setLong(2, validChestBlock.getWorldUid().getLeastSignificantBits());
		preparedStatement.setInt( 3, validChestBlock.getX());
		preparedStatement.setInt( 4, validChestBlock.getY());
		preparedStatement.setInt( 5, validChestBlock.getZ());
		return preparedStatement.executeUpdate();
	}

}
