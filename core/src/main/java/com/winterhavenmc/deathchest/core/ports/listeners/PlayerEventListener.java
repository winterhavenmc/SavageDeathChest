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

package com.winterhavenmc.deathchest.core.ports.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public interface PlayerEventListener extends Listener
{
	/**
	 * Event listener for PlayerDeathEvent<p>
	 * Attempt to deploy a death chest on player death.<br>
	 * Listens at EventPriority.HIGH to allow other plugins to process event first,
	 * in order to manipulate player's dropped items on death before placement in chest
	 *
	 * @param event PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGH)
	void onPlayerDeath(PlayerDeathEvent event);

	/**
	 * Event listener for PlayerInteractEvent<p>
	 * Performs permission checks when a player attempts to interact with a death chest.
	 * Listens at EventPriority.LOW to handle event before protection plugins
	 *
	 * @param event PlayerInteractEvent
	 */
	@EventHandler(priority = EventPriority.LOW)
	void onPlayerInteract(PlayerInteractEvent event);

}
