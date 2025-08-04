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

import com.winterhavenmc.deathchest.chests.ChestBlockType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;


public sealed interface ChestBlock permits InvalidChestBlock, ValidChestBlock
{
	static ChestBlock of(final UUID chestUid, final Location location, final ChestBlockType type)
	{
		if (location == null) return new InvalidChestBlock(ChestBlockReason.LOCATION_NULL, "🌐");
		else if (location.getWorld() == null) return new InvalidChestBlock(ChestBlockReason.WORLD_NULL, "🌐");
		else if (chestUid == null) return new InvalidChestBlock(ChestBlockReason.CHEST_UID_NULL, location.getWorld().getName());
		else if (type == null) return new InvalidChestBlock(ChestBlockReason.TYPE_NULL, location.getWorld().getName());
		else return switch (type)
		{
			case ChestBlockType.LEFT_CHEST -> new LeftChestBlock(chestUid, location);
			case ChestBlockType.RIGHT_CHEST -> new RightChestBlock(chestUid, location);
			case ChestBlockType.SIGN -> new SignChestBlock(chestUid, location);
		};
	}


	static ChestBlock of(final UUID chestUid, final String worldName, final UUID worldUid,
	                     final int x, final int y, final int z)
	{
		World world = Bukkit.getWorld(worldUid);
		if (world == null) return new InvalidChestBlock(ChestBlockReason.WORLD_NULL, worldName);
		else if (chestUid == null) return new InvalidChestBlock(ChestBlockReason.CHEST_UID_NULL, worldName);
		else
		{
			Location location = new Location(world, x, y, z);
			return switch (ChestBlockType.getType(world.getBlockAt(x, y, z)))
			{
				case ChestBlockType.LEFT_CHEST -> new LeftChestBlock(chestUid, location);
				case ChestBlockType.RIGHT_CHEST -> new RightChestBlock(chestUid, location);
				case ChestBlockType.SIGN -> new SignChestBlock(chestUid, location);
				case null -> new InvalidChestBlock(ChestBlockReason.NOT_ChEST_BLOCK, worldName);
			};
		}
	}

}
