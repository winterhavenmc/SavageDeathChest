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


public final class SqliteChestRowMapper
{
	public DeathChest map(ResultSet resultSet) throws SQLException
	{
		final UUID chestUid = new UUID(resultSet.getLong("ChestUidMsb"), resultSet.getLong("ChestUidLsb"));
		final UUID ownerUid = new UUID(resultSet.getLong("OwnerUidMsb"), resultSet.getLong("OwnerUidLsb"));
		final UUID killerUid = new UUID(resultSet.getLong("KillerUidMsb"), resultSet.getLong("KillerUidLsb"));

		final String ownerName = resultSet.getString("OwnerName");
		final String killerName = resultSet.getString("KillerName");

		final int itemCount = resultSet.getInt("ItemCount");
		final long placementTime = resultSet.getLong("PlacementTime");
		final long expirationTime = resultSet.getLong("ExpirationTime");
		final long protectionExpirationTime = resultSet.getLong("ProtectionExpirationTime");

		final UUID worldUid = new UUID(resultSet.getLong("WorldUidMsb"), resultSet.getLong("WorldUidLsb"));
		final String worldName = resultSet.getString("WorldName");
		final int locationX = resultSet.getInt("LocationX");
		final int locationY = resultSet.getInt("LocationY");
		final int locationZ = resultSet.getInt("LocationZ");

		return DeathChest.of(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
				locationX, locationY, locationZ, itemCount, Instant.ofEpochMilli(placementTime),
				Instant.ofEpochMilli(expirationTime), Instant.ofEpochMilli(protectionExpirationTime));
	}

}
