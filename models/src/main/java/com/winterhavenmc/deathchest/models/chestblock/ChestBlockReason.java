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

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public enum ChestBlockReason
{
	CHEST_UID_NULL("The parameter 'chestUid' was null."),
	LOCATION_NULL("The parameter 'location' was null."),
	TYPE_NULL("The parameter 'type' was null."),
	WORLD_NULL("The parameter 'world' was null."),
	NOT_ChEST_BLOCK("The block is not a valid chest block."),
	;

	private final String defaultMessage;


	ChestBlockReason(final String defaultMessage)
	{
		this.defaultMessage = defaultMessage;
	}


	public String getLocalizeMessage(final Locale locale)
	{
		try
		{
			final ResourceBundle bundle = ResourceBundle.getBundle(getClass().getSimpleName(), locale);
			return bundle.getString(name());
		}
		catch (MissingResourceException exception)
		{
			return this.defaultMessage;
		}
	}
}
