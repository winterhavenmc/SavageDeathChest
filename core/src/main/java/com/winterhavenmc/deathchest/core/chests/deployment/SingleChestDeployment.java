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

package com.winterhavenmc.deathchest.core.chests.deployment;

import com.winterhavenmc.deathchest.core.chests.*;
import com.winterhavenmc.deathchest.core.chests.search.QuadrantSearch;
import com.winterhavenmc.deathchest.core.chests.search.SearchResult;
import com.winterhavenmc.deathchest.core.chests.search.SearchResultCode;

import com.winterhavenmc.deathchest.core.context.ListenerCtx;
import com.winterhavenmc.deathchest.models.chestblock.ChestBlockType;
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;

import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;


public class SingleChestDeployment extends AbstractDeployment implements Deployment
{
	/**
	 * Class constructor
	 *
	 * @param ctx Listener context container
	 * @param player the player for whom a death chest is being deployed
	 * @param droppedItems the player's death drops
	 */
	public SingleChestDeployment(final ListenerCtx ctx, final Player player, final Collection<ItemStack> droppedItems)
	{
		super(ctx, player, droppedItems);
	}


	/**
	 * Execute the deployment of death chest
	 *
	 * @return the result of the attempted death chest deployment
	 */
	@Override
	public SearchResult deploy()
	{
		// make copy of dropped items
		Collection<ItemStack> remainingItems = new ArrayList<>(droppedItems);

		SearchResult searchResult;

		// if require-chest option is enabled and player does not have permission override
		if (chestRequired())
		{
			// check that player has chest in inventory
			if (containsChest(remainingItems))
			{
				// if consume-required-chest configured true: remove one chest from remaining items
				if (ctx.plugin().getConfig().getBoolean("consume-required-chest"))
				{
					remainingItems = removeOneChest(remainingItems);
				}
			}
			// else return NO_CHEST result
			else
			{
				searchResult = new SearchResult(SearchResultCode.NO_REQUIRED_CHEST, remainingItems);
				DeathChest deathChest = DeathChest.of(player, searchResult.getLocation(), ctx.plugin().getConfig());
				if (deathChest instanceof ValidDeathChest validDeathChest)
				{
					this.finalize(searchResult, validDeathChest);
				}
				return searchResult;
			}
		}

		// search for valid chest location
		searchResult = new QuadrantSearch(ctx, player, ChestSize.SINGLE).execute();

		// create new deathChest object for player
		DeathChest deathChest = DeathChest.of(player, searchResult.getLocation(), ctx.plugin().getConfig());
		if (deathChest instanceof ValidDeathChest validDeathChest)
		{
			// if search successful, place chest
			if (searchResult.getResultCode().equals(SearchResultCode.SUCCESS))
			{
				// place chest at result location
				placeChest(validDeathChest, searchResult.getLocation(), ChestBlockType.RIGHT_CHEST);

				// set chest block state
				setChestBlockState(searchResult.getLocation().getBlock(), Chest.Type.SINGLE);

				// fill chest
				remainingItems = ctx.chestManager().fill(remainingItems, validDeathChest);

				// place sign on chest
				new ChestSign(ctx, player, validDeathChest).place();
			}

			// set remaining items in result
			searchResult.setRemainingItems(remainingItems);

			// finish deployment
			this.finalize(searchResult, validDeathChest);
		}

		// return result
		return searchResult;
	}

}
