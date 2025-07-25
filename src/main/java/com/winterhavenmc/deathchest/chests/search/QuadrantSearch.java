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

package com.winterhavenmc.deathchest.chests.search;

import com.winterhavenmc.deathchest.PluginMain;
import com.winterhavenmc.deathchest.chests.ChestSize;
import org.bukkit.Location;
import org.bukkit.entity.Player;


/**
 * A class that implements a search strategy for a valid chest location
 */
public final class QuadrantSearch extends AbstractSearch
{
	/**
	 * An enum that implements a cartesian quadrant system, where each member defines the sign of the x and z coordinates.
	 * The x and z coordinates are used to describe a plane in the horizontal orientation, as if looking down from above.
	 */
	enum Quadrant
	{
		I(1,1),
		II(-1,1),
		III(-1,-1),
		IV(1,-1);

		final int xFactor;
		final int zFactor;

		/**
		 * Constructor for Quadrant enum
		 * @param xFactor the x multiplier to achieve negative or positive sign for the quadrant member
		 * @param zFactor the z multiplier to achieve negative or positive sign for the quadrant member
		 */
		Quadrant(final int xFactor, final int zFactor)
		{
			this.xFactor = xFactor;
			this.zFactor = zFactor;
		}

		/**
		 * Get a value for x that is contained within this quadrant
		 * @param x the value to be negated if necessary to become a value contained within this quadrant
		 * @return the value for x that is contained within this quadrant
		 */
		int getFactoredX(int x)
		{
			return x * xFactor;
		}

		/**
		 * Get a value for z that is contained within this quadrant
		 * @param z the value to be negated if necessary to become a value contained within this quadrant
		 * @return the value for z that is contained within this quadrant
		 */
		int getFactoredZ(int z)
		{
			return z * zFactor;
		}
	}


	/**
	 * Class constructor
	 * @param plugin reference to plugin main class
	 * @param player the player whose death triggered a death chest deployment
	 * @param chestSize the size of chest required to accommodate the players inventory
	 */
	public QuadrantSearch(final PluginMain plugin,
						  final Player player,
						  final ChestSize chestSize)
	{
		// call superclass constructor
		super(plugin, player, chestSize);
	}


	/**
	 * Execute search algorithm
	 */
	@Override
	public SearchResult execute()
	{
		// get player death location
		Location origin = player.getLocation();

		// round y to account for fractional height blocks
		origin.setY(origin.getBlockY() - 1);

		// get min y for origin
		int minY = 0;
		if (origin.getWorld() != null)
		{
			minY = origin.getWorld().getMinHeight();
		}

		// if player died below world min height and place-above-void configured true, start search at world min height
		if (origin.getY() < minY)
		{
			if (placeAboveVoid)
			{
				origin.setY(minY);
			}
			else
			{
				searchResult.setResultCode(SearchResultCode.VOID);
				searchResult.setLocation(origin);
				return searchResult;
			}
		}

		// if player died above world max build height, start search 1 block below max build height
		origin.setY(Math.min(origin.getY(), player.getWorld().getMaxHeight() - 1));

		// set test location to copy of origin
		Location testLocation = origin.clone();

		// search all locations in vertical axis upward, then downward
		for (VerticalAxis verticalAxis : VerticalAxis.values())
		{
			for (int y = 0; y < searchDistance; y++)
			{
				// if world max height reached, break loop
				if (y * verticalAxis.yFactor + testLocation.getY() >= player.getWorld().getMaxHeight())
				{
					break;
				}

				// if world min height reached, break loop
				if (y * verticalAxis.yFactor + testLocation.getY() < minY)
				{
					break;
				}

				// skip test in upper vertical axis when y == 0
				if (verticalAxis.equals(VerticalAxis.LOWER) && y == 0)
				{
					continue;
				}

				// iterate over all locations within search distance until a valid location is found
				for (int x = 0; x < searchDistance; x++)
				{
					for (int z = 0; z < searchDistance; z++)
					{
						// search x,z coordinates in each quadrant
						for (Quadrant quadrant : Quadrant.values())
						{
							// only test x == 0 or z == 0 in first quadrant
							if (!quadrant.equals(Quadrant.I) && (x == 0 || z == 0)) {
								continue;
							}

							// set new test location
							testLocation.add(quadrant.getFactoredX(x),
									verticalAxis.getFactoredY(y),
									quadrant.getFactoredZ(z));

							// get result for test location
							searchResult = validateChestLocation(player, testLocation, chestSize);

							// if test location is valid, return search result object
							if (searchResult.getResultCode().equals(SearchResultCode.SUCCESS))
							{
								return searchResult;
							}

							// rotate test location 90 degrees
							testLocation.setYaw(testLocation.getYaw() - 90);

							// get result for test location
							searchResult = validateChestLocation(player, testLocation, chestSize);

							// if test location is valid, return search result object
							if (searchResult.getResultCode().equals(SearchResultCode.SUCCESS))
							{
								return searchResult;
							}

							// reset test location
							testLocation = origin.clone();
						}
					}
				}
			}
		}
		return searchResult;
	}

}
