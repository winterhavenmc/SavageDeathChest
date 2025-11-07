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

import com.winterhavenmc.deathchest.core.context.ListenerCtx;
import com.winterhavenmc.deathchest.core.listeners.PlayerEventListener;
import com.winterhavenmc.deathchest.core.messages.Macro;
import com.winterhavenmc.deathchest.core.messages.MessageId;
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.core.permissions.InventoryOpenAction;
import com.winterhavenmc.deathchest.core.permissions.PermissionCheck;
import com.winterhavenmc.deathchest.core.permissions.QuickLootAction;
import com.winterhavenmc.deathchest.core.permissions.ResultAction;
import com.winterhavenmc.deathchest.core.tasks.DeployChestTask;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.LinkedList;


/**
 * A class that contains {@code EventHandler} methods to process player related events
 */

public final class BukkitPlayerEventListener implements PlayerEventListener
{
	private final ListenerCtx ctx;
	private final PermissionCheck permissionCheck;
	private final ResultAction inventoryOpenAction = new InventoryOpenAction();
	private final ResultAction quickLootAction = new QuickLootAction();


	/**
	 * class constructor
	 */
	public BukkitPlayerEventListener(final ListenerCtx ctx)
	{
		this.ctx = ctx;
		this.permissionCheck = new PermissionCheck(ctx);
		ctx.plugin().getServer().getPluginManager().registerEvents(this, ctx.plugin());
	}


	/**
	 * Event listener for PlayerDeathEvent<p>
	 * Attempt to deploy a death chest on player death.<br>
	 * Listens at EventPriority.HIGH to allow other plugins to process event first,
	 * in order to manipulate player's dropped items on death before placement in chest
	 *
	 * @param event PlayerDeathEvent
	 */
	@EventHandler(priority = EventPriority.HIGH)
	@Override
	public void onPlayerDeath(final PlayerDeathEvent event)
	{
		// get event player
		Player player = event.getEntity();

		// if player's current world is not enabled in config,
		// do nothing and allow inventory items to drop on ground
		if (!ctx.messageBuilder().worlds().isEnabled(player.getWorld().getUID()))
		{
			ctx.messageBuilder().compose(player, MessageId.CHEST_DENIED_WORLD_DISABLED)
					.setMacro(Macro.LOCATION, player.getLocation())
					.send();
			return;
		}

		// if player does not have permission for death chest creation,
		// do nothing and allow inventory items to drop on ground
		if (!player.hasPermission("deathchest.chest"))
		{
			ctx.messageBuilder().compose(player, MessageId.CHEST_DENIED_PERMISSION)
					.setMacro(Macro.LOCATION, player.getLocation())
					.send();
			return;
		}

		// if player is in creative mode,
		// and creative-deploy is configured false,
		// and player does not have creative-deploy permission override:
		// output message and return
		if (permissionCheck.isCreativeDeployDisabled(player))
		{
			ctx.messageBuilder().compose(player, MessageId.CREATIVE_MODE)
					.setMacro(Macro.LOCATION, player.getLocation())
					.send();
			return;
		}

		// if player inventory is empty, output message and return
		if (event.getDrops().isEmpty())
		{
			ctx.messageBuilder().compose(player, MessageId.INVENTORY_EMPTY)
					.setMacro(Macro.LOCATION, player.getLocation())
					.send();
			return;
		}

		// if configured true, output player inventory to log
		if (ctx.plugin().getConfig().getBoolean("log-inventory-on-death"))
		{
			ctx.plugin().getLogger().info(player.getDisplayName() + " death inventory:");
			ctx.plugin().getLogger().info(event.getDrops().toString());
		}

		// copy event drops to new collection
		Collection<ItemStack> droppedItems = new LinkedList<>(event.getDrops());

		// remove all items from event drops
		event.getDrops().clear();

		// deploy DeathChest after configured delay
		new DeployChestTask(ctx, player, droppedItems)
				.runTaskLater(ctx.plugin(), ctx.plugin().getConfig().getInt("chest-deployment-delay"));
	}


	/**
	 * Event listener for PlayerInteractEvent<p>
	 * Performs permission checks when a player attempts to interact with a death chest.
	 * Listens at EventPriority.LOW to handle event before protection plugins
	 *
	 * @param event PlayerInteractEvent
	 */
	@EventHandler(priority = EventPriority.LOW)
	@Override
	public void onPlayerInteract(final PlayerInteractEvent event)
	{
		final DeathChest deathChest = ctx.chestManager().getChest(event.getClickedBlock());

		if (deathChest instanceof ValidDeathChest validDeathChest)
		{
			final Player player = event.getPlayer();

			// if player sneak-clicked chest, try auto-loot
			if (isPlayerQuickLooting(event))
			{
				permissionCheck.performChecks(event, player, validDeathChest, quickLootAction);
				return;
			}

			// if right-click chest, try to open chest inventory
			if (isPlayerOpeningInventory(event))
			{
				permissionCheck.performChecks(event, player, validDeathChest, inventoryOpenAction);
			}
		}
	}


	/**
	 * Test if player is attempting to quick loot chest if allowed
	 *
	 * @param event the PlayerInteractEvent being checked
	 * @return true if player is sneak-punching a chest and configuration and permissions allows
	 */
	public boolean isPlayerQuickLooting(PlayerInteractEvent event)
	{
		return (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
				&& event.getPlayer().isSneaking()
				&& ctx.plugin().getConfig().getBoolean("quick-loot")
				&& event.getPlayer().hasPermission("deathchest.loot");
	}

	/**
	 * Test if player is attempting to open a chest by right-clicking
	 *
	 * @param event the PlayerInteractEvent being checked
	 * @return true if player is opening a chest by right-clinking
	 */
	public boolean isPlayerOpeningInventory(PlayerInteractEvent event)
	{
		return event.getAction().equals(Action.RIGHT_CLICK_BLOCK);
	}
}
