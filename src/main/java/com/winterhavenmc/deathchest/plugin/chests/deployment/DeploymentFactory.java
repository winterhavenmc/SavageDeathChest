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
 * along with this program. If not, see <http://gnu.org>.
 *
 */

package com.winterhavenmc.deathchest.plugin.chests.deployment;

import com.winterhavenmc.deathchest.plugin.PluginMain;
import com.winterhavenmc.deathchest.plugin.chests.ChestSize;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedList;


public class DeploymentFactory
{
	public Deployment createDeployment(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems)
	{
		return createDeployment(plugin, player, droppedItems, player.getLocation(), player.getKiller());
	}


	public Deployment createDeployment(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation)
	{
		return createDeployment(plugin, player, droppedItems, originLocation, player.getKiller());
	}


	public Deployment createDeployment(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation, final Player killer)
	{
		Collection<ItemStack> consolidatedItems = consolidateItemStacks(droppedItems);
		ChestSize chestSize = ChestSize.selectFor(consolidatedItems.size());

		if (chestSize.equals(ChestSize.SINGLE) || !player.hasPermission("deathchest.doublechest"))
		{
			return new SingleChestDeployment(plugin, player, consolidatedItems, originLocation, killer);
		}
		else
		{
			return new DoubleChestDeployment(plugin, player, consolidatedItems, originLocation, killer);
		}
	}


	private Collection<ItemStack> consolidateItemStacks(final Collection<ItemStack> itemStacks)
	{
		final Collection<ItemStack> returnList = new LinkedList<>();

		for (ItemStack itemStack : itemStacks)
		{
			if (itemStack == null)
			{
				continue;
			}

			for (ItemStack checkStack : returnList)
			{
				if (checkStack == null)
				{
					continue;
				}
				if (checkStack.isSimilar(itemStack))
				{
					int transferAmount = Math.min(itemStack.getAmount(), checkStack.getMaxStackSize() - checkStack.getAmount());
					itemStack.setAmount(itemStack.getAmount() - transferAmount);
					checkStack.setAmount(checkStack.getAmount() + transferAmount);
				}
			}
			if (itemStack.getAmount() > 0)
			{
				returnList.add(itemStack);
			}
		}
		return returnList;
	}

}
