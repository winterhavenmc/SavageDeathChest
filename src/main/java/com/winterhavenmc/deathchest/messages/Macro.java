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

package com.winterhavenmc.deathchest.messages;

public enum Macro
{
	DEATH_CHEST,
	LOCATION,
	PLUGIN,
	ITEM_NUMBER,
	PAGE_NUMBER,
	PAGE_TOTAL,
	OWNER,
	KILLER,
	VIEWER,

	// Status Command Macros
	VERSION,
	LANGUAGE,
	LOCALE,
	TIMEZONE,
	PROTECTION_DURATION,
	EXPIRATION_DURATION,
	SEARCH_DISTANCE,
	REQUIRE_CHEST,
	QUICK_LOOT,
	KILLER_LOOTING,
	ENABLED_WORLDS,
	URL,
	REPLACEABLE_BLOCKS, CONFIG_SETTING,
}
