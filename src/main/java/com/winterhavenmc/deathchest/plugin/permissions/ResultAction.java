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

package com.winterhavenmc.deathchest.plugin.permissions;

import com.winterhavenmc.deathchest.plugin.chests.ChestManager;
import com.winterhavenmc.deathchest.plugin.models.deathchest.ValidDeathChest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;


/**
 * Interface for action to be taken on permission result checks
 */
public interface ResultAction
{
	/**
	 * Execute action for permission check
	 *
	 * @param event the event where the permission check occurred
	 * @param player the player involved in the event
	 * @param validDeathChest the deathchest involved in the event
	 */
	void execute(Cancellable event, Player player, ValidDeathChest validDeathChest, ChestManager chestManager);

}
