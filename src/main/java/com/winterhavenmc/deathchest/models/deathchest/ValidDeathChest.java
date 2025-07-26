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

import com.winterhavenmc.library.messagebuilder.pipeline.adapters.expiration.Expirable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.location.Locatable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.looter.Lootable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.owner.Ownable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.protection.Protectable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.quantity.Quantifiable;
import com.winterhavenmc.library.messagebuilder.pipeline.adapters.uuid.Identifiable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;


public final class ValidDeathChest implements DeathChest, Ownable, Identifiable, Lootable, Locatable, Quantifiable, Expirable, Protectable
{
	private final UUID chestUid;
	private final UUID ownerUid;
	private final String ownerName;
	private final UUID killerUid;
	private final String killerName;
	private final UUID worldUid;
	private final String worldName;
	private final int locationX;
	private final int locationY;
	private final int locationZ;
	private final int itemCount;
	private final Instant placementTime;
	private final Instant expirationTime;
	private final Instant protectionTime;


	ValidDeathChest(final UUID chestUid, final UUID ownerUid, final String ownerName,
	                final UUID killerUid, final String killerName, final UUID worldUid, final String worldName,
	                final int locationX, final int locationY, final int locationZ, final int itemCount,
	                final Instant placementTime, final Instant expirationTime, final Instant protectionTime)
	{
		this.chestUid = chestUid;
		this.ownerUid = ownerUid;
		this.ownerName = ownerName;
		this.killerUid = killerUid;
		this.killerName = killerName;
		this.worldUid = worldUid;
		this.worldName = worldName;
		this.locationX = locationX;
		this.locationY = locationY;
		this.locationZ = locationZ;
		this.itemCount = itemCount;
		this.placementTime = placementTime;
		this.expirationTime = expirationTime;
		this.protectionTime = protectionTime;
	}

	public UUID chestUid()
	{
		return chestUid;
	}

	public UUID ownerUid()
	{
		return ownerUid;
	}

	public String ownerName()
	{
		return ownerName;
	}

	public UUID killerUid()
	{
		return killerUid;
	}

	public String killerName()
	{
		return killerName;
	}

	public UUID worldUid()
	{
		return worldUid;
	}

	public String worldName()
	{
		return worldName;
	}

	public int locationX()
	{
		return locationX;
	}

	public int locationY()
	{
		return locationY;
	}

	public int locationZ()
	{
		return locationZ;
	}

	public int itemCount()
	{
		return itemCount;
	}

	public Instant placementTime()
	{
		return placementTime;
	}

	public Instant expirationTime()
	{
		return expirationTime;
	}

	public Instant protectionTime()
	{
		return protectionTime;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == this)
		{
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass())
		{
			return false;
		}
		var that = (ValidDeathChest) obj;
		return Objects.equals(this.chestUid, that.chestUid) &&
				Objects.equals(this.ownerUid, that.ownerUid) &&
				Objects.equals(this.ownerName, that.ownerName) &&
				Objects.equals(this.killerUid, that.killerUid) &&
				Objects.equals(this.killerName, that.killerName) &&
				Objects.equals(this.worldUid, that.worldUid) &&
				Objects.equals(this.worldName, that.worldName) &&
				this.locationX == that.locationX &&
				this.locationY == that.locationY &&
				this.locationZ == that.locationZ &&
				this.itemCount == that.itemCount &&
				Objects.equals(this.placementTime, that.placementTime) &&
				Objects.equals(this.expirationTime, that.expirationTime) &&
				Objects.equals(this.protectionTime, that.protectionTime);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(chestUid, ownerUid, ownerName, killerUid, killerName, worldUid, worldName,
				locationX, locationY, locationZ, itemCount, placementTime, expirationTime, protectionTime);
	}

	@Override
	public String toString()
	{
		return "ValidDeathChest[" +
				"chestUid=" + chestUid + ", " +
				"ownerUid=" + ownerUid + ", " +
				"ownerName=" + ownerName + ", " +
				"killerUid=" + killerUid + ", " +
				"killerName=" + killerName + ", " +
				"worldUid=" + worldUid + ", " +
				"worldName=" + worldName + ", " +
				"locationX=" + locationX + ", " +
				"locationY=" + locationY + ", " +
				"locationZ=" + locationZ + ", " +
				"itemCount=" + itemCount + ", " +
				"placementTime=" + placementTime + ", " +
				"expirationTime=" + expirationTime + ", " +
				"protectionTime=" + protectionTime + ']';
	}

	@Override
	public Location getLocation()
	{
		return new Location(Bukkit.getWorld(worldUid), locationX, locationY, locationZ);
	}


	@Override
	public AnimalTamer getOwner()
	{
		return (AnimalTamer) Bukkit.getEntity(ownerUid);
	}

	@Override
	public UUID getUniqueId()
	{
		return chestUid();
	}

	/**
	 * Returns the expiration timestamp for this object.
	 *
	 * @return an {@link Instant} representing when the object expires
	 */
	@Override
	public Instant getExpiration()
	{
		return this.expirationTime;
	}

	/**
	 * Returns the looter or claimant entity who has permission to access the lootable object.
	 *
	 * @return the looter as an {@link Entity}, or {@code null} if unknown
	 */
	@Override
	public Entity getLooter()
	{
		return Bukkit.getEntity(killerUid);
	}

	/**
	 * Returns the protection {@link Instant}, representing the moment at which
	 * protection ends or expires.
	 *
	 * @return the protection expiration timestamp
	 */
	@Override
	public Instant getProtection()
	{
		return this.protectionTime;
	}

	/**
	 * Returns the numeric quantity associated with this object.
	 *
	 * @return the quantity as an integer
	 */
	@Override
	public int getQuantity()
	{
		return this.itemCount;
	}

}
