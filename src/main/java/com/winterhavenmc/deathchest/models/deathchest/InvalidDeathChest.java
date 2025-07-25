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

import java.util.Objects;

public final class InvalidDeathChest implements DeathChest
{
	private final String reason;

	public InvalidDeathChest(String reason)
	{
		this.reason = reason;
	}

	public String reason()
	{
		return reason;
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
		var that = (InvalidDeathChest) obj;
		return Objects.equals(this.reason, that.reason);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(reason);
	}

	@Override
	public String toString()
	{
		return "InvalidDeathChest[" +
				"reason=" + reason + ']';
	}
}
