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

package com.winterhavenmc.deathchest.adapters.listeners.bukkit;

import com.winterhavenmc.deathchest.core.chests.LocationUtilities;
import com.winterhavenmc.deathchest.core.listeners.BlockEventListener;
import com.winterhavenmc.deathchest.core.context.ListenerCtx;
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.core.permissions.BreakChestAction;
import com.winterhavenmc.deathchest.core.permissions.PermissionCheck;
import com.winterhavenmc.deathchest.core.permissions.ResultAction;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Collection;
import java.util.LinkedList;


/**
 * A class that contains {@code EventHandler} methods to process block related events
 */
public final class BukkitBlockEventListener implements BlockEventListener
{
	private final PermissionCheck permissionCheck;
	final ResultAction breakChestAction = new BreakChestAction();
	private final ListenerCtx ctx;


	/**
	 * Class constructor
	 */
	public BukkitBlockEventListener(final ListenerCtx ctx)
	{
		this.ctx = ctx;
		this.permissionCheck = new PermissionCheck(ctx);
		ctx.plugin().getServer().getPluginManager().registerEvents(this, ctx.plugin());
	}


	/**
	 * Block place event handler<br>
	 * prevent placing chests adjacent to existing death chest
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	@Override
	public void onBlockPlace(final BlockPlaceEvent event)
	{
		final Block block = event.getBlock();
		final Location location = block.getLocation();

		// if placed block is not a chest, do nothing and return
		if (!block.getType().equals(Material.CHEST))
		{
			return;
		}

		// check for adjacent death chests and cancel event if found
		if (ctx.chestManager().isChestBlockChest(LocationUtilities.getBlockToLeft(location))
				|| ctx.chestManager().isChestBlockChest(LocationUtilities.getBlockToRight(location)))
		{
			event.setCancelled(true);
		}
	}


	/**
	 * Block break event handler<br>
	 * Checks for ownership of death chests and prevents breakage by non-owners.<br>
	 * Listens at EventPriority.LOW to handle event before protection plugins
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler(priority = EventPriority.LOW)
	@Override
	public void onBlockBreak(final BlockBreakEvent event)
	{
		// get instance of DeathChest from event block
		final DeathChest deathChest = ctx.chestManager().getChest(event.getBlock());

		// if death chest is valid, do permission check and conditionally break block
		if (deathChest instanceof ValidDeathChest validDeathChest)
		{
			// get player from event
			final Player player = event.getPlayer();

			// do permissions check and take appropriate action
			permissionCheck.performChecks(event, player, validDeathChest, breakChestAction);
		}
	}


	/**
	 * Entity explode event handler<br>
	 * Make death chests explosion proof if chest-protection is enabled
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	@Override
	public void onEntityExplode(final EntityExplodeEvent event)
	{
		// if chest-protection is not enabled in config, do nothing and return
		if (!ctx.plugin().getConfig().getBoolean("chest-protection"))
		{
			return;
		}

		// iterate through all blocks in explosion event and remove those that are DeathChest chests or signs
		Collection<Block> blocks = new LinkedList<>(event.blockList());
		for (Block block : blocks)
		{
			if (ctx.chestManager().isChestBlock(block))
			{
				// remove death chest block from blocks exploded list if protection has not expired
				DeathChest deathChest = ctx.chestManager().getChest(block);

				if (deathChest instanceof ValidDeathChest validDeathChest
						&& !ctx.chestManager().protectionExpired(validDeathChest))
				{
					event.blockList().remove(block);
				}
			}
		}
	}


	/**
	 * Block explode event handler<br>
	 * Make death chests explosion proof if chest-protection is enabled
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	@Override
	public void onBlockExplode(final BlockExplodeEvent event)
	{
		// if chest-protection is not enabled in config, do nothing and return
		if (!ctx.plugin().getConfig().getBoolean("chest-protection"))
		{
			return;
		}

		// iterate through all blocks in explosion event and remove those that are DeathChest chests or signs
		Collection<Block> blocks = new LinkedList<>(event.blockList());
		for (Block block : blocks) {
			if (ctx.chestManager().isChestBlock(block))
			{
				// remove death chest block from blocks exploded list if protection has not expired
				DeathChest deathChest = ctx.chestManager().getChest(block);

				if (deathChest instanceof ValidDeathChest validDeathChest
						&& !ctx.chestManager().protectionExpired(validDeathChest))
				{
					event.blockList().remove(block);
				}
			}
		}
	}


	/**
	 * Block physics event handler<br>
	 * remove detached death chest signs from game to prevent players gaining additional signs
	 *
	 * @param event the event being handled by this method
	 */
	@EventHandler
	@Override
	public void signDetachCheck(final BlockPhysicsEvent event)
	{
		// if event block is a DeathChest component, cancel event
		if (ctx.chestManager().isChestBlockSign(event.getBlock()))
		{
			event.setCancelled(true);
		}
	}

}
