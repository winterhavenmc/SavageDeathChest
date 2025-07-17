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

package com.winterhavenmc.deathchest.storage;

import java.time.Instant;
import java.util.UUID;

public enum Field
{
	CHEST_UID("chestUid", UUID.class),
	OWNER_UID("ownerUid", UUID.class),
	KILLER_UID("killerUid", UUID.class),
	KILLER_NAME("killerName", String.class),
	ITEM_COUNT("itemCount", Integer.class),
	PLACEMENT_TIMESTAMP("placementTime", Instant.class),
	EXPIRATION_TIMESTAMP("expirationTime", Instant.class),
	PROTECTION_TIMESTAMP("protectionExpirationTime", Instant.class),
	;

	private final String fieldName;
	private final Class<?> type;


	Field(final String fieldName, final Class<?> type)
	{
		this.fieldName = fieldName;
		this.type = type;
	}

	public String getFieldName()
	{
		return fieldName;
	}

	public Class<?> getType()
	{
		return type;
	}

}
