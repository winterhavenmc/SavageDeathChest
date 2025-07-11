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

package com.winterhavenmc.deathchest.listeners;


import com.winterhavenmc.deathchest.PluginMain;
import com.winterhavenmc.deathchest.chests.DeathChestRecord;
import com.winterhavenmc.deathchest.permissions.PermissionCheck;
import com.winterhavenmc.deathchest.permissions.protectionplugins.ProtectionCheckResult;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;

import java.util.Set;


/**
 * A class that contains {@code EventHandler} methods to process inventory related events
 */

public final class InventoryEventListener implements Listener
{
	// reference to main class
	private final PluginMain plugin;

	// reference to permissionCheck class
	private final PermissionCheck permissionCheck;

	private final Set<InventoryAction> inventoryPlaceActions = Set.of(
			InventoryAction.PLACE_ALL,
			InventoryAction.PLACE_SOME,
			InventoryAction.PLACE_ONE,
			InventoryAction.SWAP_WITH_CURSOR);


	/**
	 * class constructor
	 *
	 * @param plugin reference to main class
	 */
	public InventoryEventListener(final PluginMain plugin)
	{
		// set reference to main class
		this.plugin = plugin;

		// create instance of permissionCheck class
		this.permissionCheck = new PermissionCheck(plugin);

		// register event handlers in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	/**
	 * Inventory open event handler<br>
	 * Uncancels an event that was cancelled by a protection plugin
	 * if configured to override the protection plugin and thereby allow
	 * death chest access where chest access would normally be restricted
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onInventoryOpen(final InventoryOpenEvent event)
	{
		// get death chest for event inventory
		final DeathChestRecord deathChest = plugin.chestManager.getChest(event.getInventory());

		// if death chest is null, do nothing and return
		if (deathChest == null)
		{
			return;
		}

		// if event entity is not a player, do nothing and return
		if (!(event.getPlayer() instanceof final Player player))
		{
			return;
		}

		// if access is blocked by a protection plugin, do nothing and return (allow protection plugin to handle event)
		ProtectionCheckResult protectionCheckResult = plugin.protectionPluginRegistry.accessAllowed(player, deathChest.getLocation());

		if (permissionCheck.isPluginBlockingAccess(protectionCheckResult))
		{
			// do not cancel event - allow protection plugin to handle it
			return;
		}

		// uncancel event
		event.setCancelled(false);
	}


	/**
	 * Remove empty death chest on inventory close event
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	public void onInventoryClose(final InventoryCloseEvent event)
	{
		// get event inventory
		final Inventory inventory = event.getInventory();

		// get death chest from inventory
		final DeathChestRecord deathChest = plugin.chestManager.getChest(inventory);

		// if death chest is null, do nothing and return
		if (deathChest == null)
		{
			return;
		}

		// if inventory is empty, destroy chest(s) and sign
		if (inventory.isEmpty())
		{
			plugin.chestManager.destroy(deathChest);
		}
	}


	/**
	 * Prevent hoppers from removing or inserting items in death chests
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryMoveItem(final InventoryMoveItemEvent event)
	{
		// get inventories involved in event
		final Inventory destination = event.getDestination();
		final Inventory source = event.getSource();

		// prevent extracting items from death chest using hopper
		if (plugin.chestManager.isDeathChestInventory(source))
		{
			event.setCancelled(true);
			return;
		}

		// prevent inserting items into death chest using hopper if prevent-item-placement configured true
		if (plugin.getConfig().getBoolean("prevent-item-placement"))
		{

			// if destination inventory is a death chest, cancel event and return
			if (plugin.chestManager.isDeathChestInventory(destination))
			{
				event.setCancelled(true);
			}
		}
	}


	/**
	 * Prevent placing items in death chests if configured
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(final InventoryClickEvent event)
	{
		// if prevent-item-placement is configured false, do nothing and return
		if (!plugin.getConfig().getBoolean("prevent-item-placement"))
		{
			return;
		}

		final Inventory inventory = event.getInventory();
		final InventoryAction action = event.getAction();

		// if inventory is a death chest inventory
		if (plugin.chestManager.isDeathChestInventory(inventory))
		{

			// if click action is place, test for chest slots
			if (inventoryPlaceActions.contains(action))
			{

				// if slot is in chest inventory area, check for player override permission
				if (event.getRawSlot() < inventory.getSize())
				{

					// if player does not have allow-place permission, cancel event
					if (!event.getWhoClicked().hasPermission("deathchest.allow-place"))
					{
						event.setCancelled(true);
					}
				}
				return;
			}

			// prevent shift-click transfer to death chest
			if (action.equals(InventoryAction.MOVE_TO_OTHER_INVENTORY))
			{

				if (event.getRawSlot() >= inventory.getSize())
				{

					// if player does not have allow-place permission, cancel event
					if (!event.getWhoClicked().hasPermission("deathchest.allow-place"))
					{
						event.setCancelled(true);
					}
				}
			}
		}
	}


	/**
	 * Prevent placing items in death chests if configured
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	public void onInventoryDrag(final InventoryDragEvent event)
	{
		// get inventory from event
		final Inventory inventory = event.getInventory();

		// if inventory is not a death chest inventory, do nothing and return
		if (!plugin.chestManager.isDeathChestInventory(inventory))
		{
			return;
		}

		// if prevent-item-placement is configured false, do nothing and return
		if (!plugin.getConfig().getBoolean("prevent-item-placement"))
		{
			return;
		}

		// if player has allow-place permission, do nothing and return
		if (event.getWhoClicked().hasPermission("deathchest.allow-place"))
		{
			return;
		}

		// iterate over dragged slots and if any are above max slot, cancel event
		for (int slot : event.getRawSlots())
		{
			if (slot < inventory.getSize())
			{
				event.setCancelled(true);
				break;
			}
		}
	}

}
