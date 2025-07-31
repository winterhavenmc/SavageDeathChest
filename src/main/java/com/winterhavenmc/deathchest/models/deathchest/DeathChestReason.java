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

import com.winterhavenmc.deathchest.util.Reason;

public enum DeathChestReason implements Reason
{
	OWNER_NULL("The parameter 'owner' was null."),
	LOCATION_NULL("The parameter 'location' was null."),
	CONFIG_NULL("The parameter 'config' was null."),
	CHEST_UID_NULL("The parameter 'chestUid' was null."),
	OWNER_UID_NULL("The parameter 'ownerUid' was null."),
	WORLD_UID_NULL("The parameter 'worldUid' was null."),
	BLOCK_NULL("The parameter 'block' was null."),
	BLOCK_INVALID("The block was not a DeathChest block."),
	;

	private final String message;


	DeathChestReason(final String message)
	{
		this.message = message;
	}


	public String message()
	{
		return message;
	}


	@Override
	public String toString()
	{
		return message;
	}
}
