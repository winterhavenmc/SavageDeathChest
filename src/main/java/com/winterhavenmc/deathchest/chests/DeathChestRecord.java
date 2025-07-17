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

package com.winterhavenmc.deathchest.chests;

import com.winterhavenmc.library.messagebuilder.pipeline.adapters.expiration.Expirable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.location.Locatable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.looter.Lootable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.owner.Ownable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.protection.Protectable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.quantity.Quantifiable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.uuid.Identifiable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;


/**
 * A Java Record used as a data object representing a player's DeathChest.
 * <p>
 * A custom constructor is included as well as accessor methods that allow the record to implement a variety
 * of MessageBuilder interfaces, which facilitates the automatic population of macro replacement fields in messages.
 *
 * @param chestUid   a randomly generated UUID used as a key for each unique chest
 * @param ownerUid   the UUID of the player whose inventory was placed in the DeathChest
 * @param ownerName  the name of the player at the time of chest deployment, to be used as a fallback if a name
 *                   cannot be derived from the ownerUid at a later time
 * @param killerUid  the UUID of the entity that killed the player resulting in the deployment of a DeathChest, typically a player
 * @param killerName the name of the player's killer, to be used as a fallback if a name cannot be derived from
 *                   the killerUid at a later time
 * @param worldUid   the UUID of the world in which the player death occurred for use in constructing a new instance
 *                   of the DeathChest location
 * @param worldName  the name of the world, for use in display in the case that a world containing a DeathChest is
 *                   not currently loaded
 * @param locationX  the block X coordinate to be used in the construction of a new instance of the DeathChest location
 * @param locationY  the block Y coordinate to be used in the construction of a new instance of the DeathChest location
 * @param locationZ  the block Z coordinate to be used in the construction of a new instance of the DeathChest location
 * @param itemCount  the number of ItemStacks placed in the chest <em>(currently unimplemented)</em>
 * @param placementTime a timestamp {@code Instant} of when the DeathChest was deployed
 * @param expirationTime a timestamp {@code Instant} of when the DeathChest will expire, dropping its contents
 * @param protectionTime a timestamp {@code Instant} of when the DeathChest will be accessible to other players, if so configured
 */
public record DeathChestRecord(
		UUID chestUid,
		UUID ownerUid,
		String ownerName,
		UUID killerUid,
		String killerName,
		UUID worldUid,
		String worldName,
		int locationX,
		int locationY,
		int locationZ,
		int itemCount,
		Instant placementTime,
		Instant expirationTime,
		Instant protectionTime) implements Ownable, Identifiable, Lootable, Locatable, Quantifiable, Expirable, Protectable
{
	/**
	 * Creates a DeathChest record from a player object at time of death. Parameter validation is performed for values
	 * as appropriate with alternative values substituted via ternary operator.
	 *
	 * @param plugin Instance of the plugin main class, used for retrieving configuration values
	 * @param player Instance of a player, typically at the time of death when a new chest is deployed
	 */
	public DeathChestRecord(final Plugin plugin, final Player player, final Location location)
	{
		this(UUID.randomUUID(),
				(player != null) ? player.getUniqueId() : INVALID_UUID,
				(player != null) ? player.getName() : UNKNOWN_VALUE,
				(player != null) && player.getKiller() != null ? player.getKiller().getUniqueId() : INVALID_UUID,
				(player != null) ? player.getName() : UNKNOWN_VALUE,
				(location != null && location.getWorld() != null) ? location.getWorld().getUID() : INVALID_UUID,
				(location != null && location.getWorld() != null) ? location.getWorld().getName() : UNKNOWN_VALUE,
				(location != null) ? location.getBlockX() : 0,
				(location != null) ? location.getBlockY() : 0,
				(location != null) ? location.getBlockZ() : 0,
				0,
				Instant.now(),
				(plugin.getConfig().getLong("expire-time") > 0)
						? Instant.now().plus(plugin.getConfig().getLong("expire-time"), ChronoUnit.MINUTES)
						: Instant.EPOCH,
				(plugin.getConfig().getLong("chest-protection-time") > 0)
						? Instant.now().plus(plugin.getConfig().getLong("chest-protection-time"), ChronoUnit.MINUTES)
						: Instant.EPOCH);
	}

	private static final UUID INVALID_UUID = new UUID(0,0);
	private static final String UNKNOWN_VALUE = "-";

	@Override
	public AnimalTamer getOwner()
	{
		return (AnimalTamer) Bukkit.getEntity(ownerUid);
	}

	@Override
	public UUID getUniqueId()
	{
		return ownerUid;
	}

	@Override
	public Entity getLooter()
	{
		return Bukkit.getEntity(killerUid);
	}

	@Override
	public Location getLocation()
	{
		// if worldUid is null, return null
		if (this.worldUid == null)
		{
			return null;
		}

		// get world by uid
		World world = Bukkit.getWorld(worldUid);

		// if world is null, return null
		if (world == null)
		{
			return null;
		}

		// return new location
		return new Location(world, locationX, locationY, locationZ);
	}

	@Override
	public int getQuantity()
	{
		return itemCount;
	}

	@Override
	public Instant getExpiration()
	{
		return expirationTime;
	}

	@Override
	public Instant getProtection()
	{
		return protectionTime;
	}


	public Optional<Location> getOptLocation()
	{
		return Optional.ofNullable(this.getLocation());
	}


	public boolean hasValidOwnerUid()
	{
		return this.ownerUid() != null
				&& (this.ownerUid().getMostSignificantBits() != 0
				&& this.ownerUid().getLeastSignificantBits() != 0);
	}


	public boolean hasValidKillerUid()
	{
		return this.killerUid() != null
				&& (this.killerUid().getMostSignificantBits() != 0
				&& this.killerUid().getLeastSignificantBits() != 0);
	}


	/**
	 * Test if a player is the owner of this DeathChest
	 *
	 * @param player The player to test for DeathChest ownership
	 * @return {@code true} if the player is the DeathChest owner, false if not
	 */
	public boolean isOwner(final Player player)
	{
		// if ownerUUID is null, return false
		if (this.ownerUid() == null)
		{
			return false;
		}
		return this.ownerUid().equals(player.getUniqueId());
	}


	/**
	 * Test if a player is the killer of this DeathChest owner
	 *
	 * @param player The player to test for DeathChest killer
	 * @return {@code true} if the player is the killer of the DeathChest owner, false if not
	 */
	public boolean isKiller(final Player player)
	{
		return this.hasValidKillerUid() && this.killerUid().equals(player.getUniqueId());
	}

}
