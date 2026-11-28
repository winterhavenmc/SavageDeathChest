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
 * along with this program. If not, see <http://gnu.org>.
 *
 */

package com.winterhavenmc.deathchest.plugin.models.deathchest;

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
		return of(owner, (owner != null) ? owner.getKiller() : null, location, config);
	}


	static DeathChest of(final Player owner, final Player killer, final Location location, final Configuration config)
	{
		if (owner == null) return new InvalidDeathChest(DeathChestReason.OWNER_NULL);
		else if (location == null) return new InvalidDeathChest(DeathChestReason.LOCATION_NULL);
		else if (config == null) return new InvalidDeathChest(DeathChestReason.CONFIG_NULL);
		else
		{
			final UUID chestUid = UUID.randomUUID();
			final UUID ownerUid = owner.getUniqueId();
			final String ownerName = owner.getName();

			final Player resolvedKiller = (killer != null) ? killer : owner.getKiller();
			final UUID killerUid = (resolvedKiller != null) ? resolvedKiller.getUniqueId() : INVALID_UUID;
			final String killerName = (resolvedKiller != null) ? resolvedKiller.getName() : "";

			// use location world if available so cross-world respawn chests store the actual chest world
			final String worldName = (location.getWorld() != null) ? location.getWorld().getName() : owner.getWorld().getName();
			final UUID worldUid = (location.getWorld() != null) ? location.getWorld().getUID() : owner.getWorld().getUID();
			final int locationX = location.getBlockX();
			final int locationY = location.getBlockY();
			final int locationZ = location.getBlockZ();

			final Instant placementTime = Instant.now();
			final Instant expirationTime = Instant.now().plus(Duration.ofMinutes(config.getInt("expire-time")));
			final Instant protectionExpirationTime = Instant.now().plus(Duration.ofMinutes(config.getInt("chest-protection-time")));

			return new ValidDeathChest(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
					locationX, locationY, locationZ, 0, placementTime, expirationTime, protectionExpirationTime);
		}
	}


	static DeathChest of(final UUID chestUid, final UUID ownerUid, final String ownerName,
						 final UUID killerUid, final String killerName, final UUID worldUid, final String worldName,
						 final int locationX, final int locationY, final int locationZ, final int itemCount,
						 final Instant placementTime, final Instant expirationTime, final Instant protectionExpirationTime)
	{
		if (chestUid == null) return new InvalidDeathChest(DeathChestReason.CHEST_UID_NULL);
		else if (ownerUid == null) return new InvalidDeathChest(DeathChestReason.OWNER_UID_NULL);
		else if (worldUid == null) return new InvalidDeathChest(DeathChestReason.WORLD_UID_NULL);
		else return new ValidDeathChest(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
					locationX, locationY, locationZ, itemCount, placementTime, expirationTime, protectionExpirationTime);
	}

}
