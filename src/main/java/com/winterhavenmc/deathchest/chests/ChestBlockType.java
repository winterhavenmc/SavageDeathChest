/*
 * Copyright (c) 2022 Tim Savage.
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

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;


/**
 * An enum whose values represent the different types of chest blocks
 */
public enum ChestBlockType
{
	SIGN,
	LEFT_CHEST,
	RIGHT_CHEST;


	/**
	 * Get chest block type from existing in-game block
	 *
	 * @param block block to determine chest type
	 * @return ChestBlockType enum value, or null if block is not a chest or sign
	 */
	public static ChestBlockType getType(final Block block)
	{
		BlockState blockState = block.getState();

		switch (blockState)
		{
			case Sign ignored ->
			{
				return ChestBlockType.SIGN;
			}

			case Chest chest ->
			{
				if (chest.getBlockData() instanceof org.bukkit.block.data.type.Chest chestBlockData
						&& chestBlockData.getType().equals(org.bukkit.block.data.type.Chest.Type.LEFT))
				{
					return ChestBlockType.LEFT_CHEST;
				}
				else
				{
					return ChestBlockType.RIGHT_CHEST;
				}
			}

			default ->
			{
				return null;
			}
		}
	}

}
