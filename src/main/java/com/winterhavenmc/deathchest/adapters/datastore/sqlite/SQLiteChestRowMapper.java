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

import com.winterhavenmc.deathchest.models.deathchest.DeathChest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;


public final class SQLiteChestRowMapper
{
	public DeathChest map(ResultSet resultSet) throws SQLException
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

		return DeathChest.of(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
				locationX, locationY, locationZ, itemCount, Instant.ofEpochMilli(placementTime),
				Instant.ofEpochMilli(expirationTime), Instant.ofEpochMilli(protectionExpirationTime));
	}

}
