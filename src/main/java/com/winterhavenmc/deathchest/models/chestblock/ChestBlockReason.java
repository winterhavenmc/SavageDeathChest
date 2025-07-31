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

import com.winterhavenmc.deathchest.util.Reason;


public enum ChestBlockReason implements Reason
{
	CHEST_UID_NULL("The parameter 'chestUid' was null."),
	LOCATION_NULL("The parameter 'location' was null."),
	TYPE_NULL("The parameter 'type' was null."),
	WORLD_NULL("The parameter 'world' was null."),
	NOT_ChEST_BLOCK("The block is not a valid chest block."),
	;

	private final String message;


	ChestBlockReason(final String message)
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
