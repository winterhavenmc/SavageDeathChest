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

package com.winterhavenmc.deathchest.models.chestblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;


public class AbstractChestBlock
{
	protected final UUID chestUid;
	protected final String worldName;
	protected final UUID worldUid;
	protected final int x;
	protected final int y;
	protected final int z;
	protected final float yaw;
	protected final float pitch;

	AbstractChestBlock(final UUID chestUid, final Location location)
	{
		this.chestUid = chestUid;
		this.worldName = location.getWorld().getName();
		this.worldUid = location.getWorld().getUID();
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
		this.yaw = location.getYaw();
		this.pitch = location.getPitch();
	}

	/**
	 * Getter method for chest block location
	 *
	 * @return Location - the in game location of this chest block
	 */
	public Location getLocation()
	{
		World world = Bukkit.getServer().getWorld(this.worldUid);

		if (world == null)
		{
			return null;
		}

		// return new location object
		return new Location(world,
				this.x,
				this.y,
				this.z,
				this.yaw,
				this.pitch);
	}

	public UUID getChestUid()
	{
		return chestUid;
	}

	public String getWorldName()
	{
		return this.worldName;
	}

	public UUID getWorldUid()
	{
		return this.worldUid;
	}

	public int getX()
	{
		return this.x;
	}

	public int getY()
	{
		return this.y;
	}

	public int getZ()
	{
		return this.z;
	}

}
