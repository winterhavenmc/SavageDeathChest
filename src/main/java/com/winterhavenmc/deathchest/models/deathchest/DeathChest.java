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

package com.winterhavenmc.deathchest.models.deathchest;

import org.bukkit.Location;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;


public sealed interface DeathChest permits ValidDeathChest, InvalidDeathChest
{
	UUID INVALID_UUID = new UUID(0, 0);

	static DeathChest of(final Player owner, final Location location, final Configuration config)
	{
		if (owner == null) return new InvalidDeathChest("Parameter 'owner' was null.");
		else if (location == null) return new InvalidDeathChest("Parameter 'location' was null.");
		else
		{
			UUID chestUid = UUID.randomUUID();
			UUID ownerUid = owner.getUniqueId();
			String ownerName = owner.getName();

			UUID killerUid = (owner.getKiller() != null) ? owner.getKiller().getUniqueId() : INVALID_UUID;
			String killerName = (owner.getKiller() != null) ? owner.getKiller().getName() : "";

			// player.getWorld() cannot be null
			String worldName = owner.getWorld().getName();
			UUID worldUid = owner.getWorld().getUID();
			int locationX = location.getBlockX();
			int locationY = location.getBlockY();
			int locationZ = location.getBlockZ();

			Instant placementTime = Instant.now();
			Instant expirationTime = Instant.now().plus(Duration.ofMinutes(config.getInt("expire-time")));
			Instant protectionExpirationTime = Instant.now().plus(Duration.ofMinutes(config.getInt("chest-protection-time")));

			return new ValidDeathChest(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
					locationX, locationY, locationZ, 0, placementTime, expirationTime, protectionExpirationTime);
		}
	}


	static DeathChest of(final UUID chestUid, final UUID ownerUid, final String ownerName,
	                     final UUID killerUid, final String killerName, final UUID worldUid, final String worldName,
	                     final int locationX, final int locationY, final int locationZ, final int itemCount,
	                     final Instant placementTime, final Instant expirationTime, final Instant protectionExpirationTime)
	{
		if (chestUid == null) return new InvalidDeathChest("The parameter 'chestUid' was null.");
		else if (ownerUid == null) return new InvalidDeathChest("The parameter 'ownerUid' was null.");
		else if (worldUid == null) return new InvalidDeathChest("The parameter 'worldUid' was null.");
		else return new ValidDeathChest(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
				locationX, locationY, locationZ, itemCount, placementTime, expirationTime, protectionExpirationTime);
	}

}
