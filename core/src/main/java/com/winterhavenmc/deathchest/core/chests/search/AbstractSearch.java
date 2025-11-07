/*
 * Copyright (c) 2022-2025 Tim Savage.
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

package com.winterhavenmc.deathchest.core.chests.search;


import com.winterhavenmc.deathchest.core.chests.ChestSize;
import com.winterhavenmc.deathchest.core.chests.LocationUtilities;
import com.winterhavenmc.deathchest.core.context.ListenerCtx;
import com.winterhavenmc.deathchest.core.permissions.protectionplugins.ProtectionCheckResult;
import com.winterhavenmc.deathchest.core.permissions.protectionplugins.ProtectionCheckResultCode;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;


/**
 * An abstract class that provides default implementations of methods required of the Search interface
 */
abstract class AbstractSearch implements Search
{
	private final ListenerCtx ctx;
	protected final Player player;
	protected final ChestSize chestSize;
	protected final int searchDistance;
	protected final boolean placeAboveVoid;
	protected SearchResult searchResult;


	/**
	 * Class constructor
	 * @param player player for whom death chest is being placed
	 * @param chestSize double or single chest
	 */
	protected AbstractSearch(final ListenerCtx ctx,
	                         final Player player,
	                         final ChestSize chestSize)
	{
		this.ctx = ctx;
		this.player = player;
		this.chestSize = chestSize;
		this.searchDistance = ctx.plugin().getConfig().getInt("search-distance");
		this.placeAboveVoid = ctx.plugin().getConfig().getBoolean("place-above-void");

		// initialize default result
		searchResult = new SearchResult();
		searchResult.setLocation(player.getLocation());
	}


	@Override
	public abstract SearchResult execute();


	/**
	 * Validate chest location for chest size
	 *
	 * @param player    the player for whom the chest is being placed
	 * @param location  the location to test
	 * @param chestSize the size of the chest to be placed (single, double)
	 * @return SearchResult - the result object for the tested location
	 */
	SearchResult validateChestLocation(final Player player,
									   final Location location,
									   final ChestSize chestSize)
	{
		// test right chest location
		SearchResult result = validateChestLocation(player, location);

		// if right chest is not successful, return result
		if (!result.getResultCode().equals(SearchResultCode.SUCCESS))
		{
			return result;
		}

		// if chest is to be a double chest, test left chest location
		if (chestSize.equals(ChestSize.DOUBLE))
		{
			// test left chest block location (to player's right)
			result = validateChestLocation(player, LocationUtilities.getLocationToRight(location));
			result.setLocation(location);
		}

		return result;
	}


	/**
	 * Validate chest solitary location
	 *
	 * @param player    the player for whom the chest is being placed
	 * @param location  the location to test
	 * @return SearchResult - the result object for the tested location
	 */
	private SearchResult validateChestLocation(final Player player, final Location location)
	{
		Block block = location.getBlock();

		// if block at location is not replaceable block, return negative result
		if (!ctx.chestManager().isReplaceableBlock(block))
		{
			searchResult.setResultCode(SearchResultCode.NON_REPLACEABLE_BLOCK);
			return searchResult;
		}

		// if block at location is above grass path, return negative result
		if (LocationUtilities.isAbovePath(block))
		{
			searchResult.setResultCode(SearchResultCode.ABOVE_GRASS_PATH);
			return searchResult;
		}

		// if block at location is protected by plugin, return negative result
		ProtectionCheckResult protectionCheckResult = ctx.protectionPluginRegistry().placementAllowed(player, location);
		if (protectionCheckResult.getResultCode().equals(ProtectionCheckResultCode.BLOCKED))
		{
			searchResult.setResultCode(SearchResultCode.PROTECTION_PLUGIN);
			searchResult.setProtectionPlugin(protectionCheckResult.getProtectionPlugin());
			return searchResult;
		}

		// if block at location is within spawn protection radius, return negative result
		if (isSpawnProtected(location))
		{
			searchResult.setResultCode(SearchResultCode.SPAWN_RADIUS);
			return searchResult;
		}

		// return successful result with location
		searchResult.setResultCode(SearchResultCode.SUCCESS);
		searchResult.setLocation(location);
		return searchResult;
	}


	/**
	 * Check if location is within world spawn protection radius
	 *
	 * @param location the location to check
	 * @return {@code true} if passed location is within world spawn protection radius, {@code false} if not
	 */
	private boolean isSpawnProtected(final Location location)
	{
		// if no server ops, spawn protection is disabled
		if (ctx.plugin().getServer().getOperators().isEmpty())
		{
			return false;
		}

		// check for null parameter
		if (location == null || location.getWorld() == null)
		{
			return false;
		}

		// get world spawn location for location
		Optional<Location> worldSpawn = ctx.messageBuilder().worlds().spawnLocation(location.getWorld().getUID());

		// check for null worldSpawn, null world, spawn location is not overworld, same worlds
		if (worldSpawn.isEmpty()
				|| worldSpawn.get().getWorld() == null
				|| !worldSpawn.get().getWorld().getEnvironment().equals(World.Environment.NORMAL)
				|| !location.getWorld().getUID().equals(worldSpawn.get().getWorld().getUID()))
		{
			return false;
		}

		// get spawn protection radius
		double spawnRadius = ctx.plugin().getServer().getSpawnRadius();

		// if location is within spawn radius of world spawn location, return true; else return false
		return location.distanceSquared(worldSpawn.get()) < (Math.pow(spawnRadius, 2.0d));
	}


	/**
	 * An enum that defines upper and lower region and provides a multiplier to achieve
	 * the positive or negative sign of each member
	 */
	enum VerticalAxis
	{
		UPPER(1),
		LOWER(-1);

		final int yFactor;

		VerticalAxis(final int yFactor)
		{
			this.yFactor = yFactor;
		}

		int getFactoredY(int y) {
			return y + this.yFactor;
		}
	}

}
