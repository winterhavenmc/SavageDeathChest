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

package com.winterhavenmc.deathchest.core.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;

public interface InventoryEventListener extends Listener
{
	/**
	 * Inventory open event handler<br>
	 * Uncancels an event that was cancelled by a protection plugin
	 * if configured to override the protection plugin and thereby allow
	 * death chest access where chest access would normally be restricted
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(priority = EventPriority.HIGH)
	void onInventoryOpen(InventoryOpenEvent event);

	/**
	 * Remove empty death chest on inventory close event
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	void onInventoryClose(InventoryCloseEvent event);

	/**
	 * Prevent hoppers from removing or inserting items in death chests
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	void onInventoryMoveItem(InventoryMoveItemEvent event);

	/**
	 * Prevent placing items in death chests if configured
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	void onInventoryClick(InventoryClickEvent event);

	/**
	 * Prevent placing items in death chests if configured
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(ignoreCancelled = true)
	void onInventoryDrag(InventoryDragEvent event);
}
