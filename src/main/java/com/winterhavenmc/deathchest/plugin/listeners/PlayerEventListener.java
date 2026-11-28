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

package com.winterhavenmc.deathchest.plugin.listeners;

import com.winterhavenmc.deathchest.plugin.PluginMain;
import com.winterhavenmc.deathchest.plugin.messages.Macro;
import com.winterhavenmc.deathchest.plugin.messages.MessageId;
import com.winterhavenmc.deathchest.plugin.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.plugin.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.plugin.permissions.InventoryOpenAction;
import com.winterhavenmc.deathchest.plugin.permissions.PermissionCheck;
import com.winterhavenmc.deathchest.plugin.permissions.QuickLootAction;
import com.winterhavenmc.deathchest.plugin.permissions.ResultAction;
import com.winterhavenmc.deathchest.plugin.tasks.DeployChestTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.LinkedList;


public final class PlayerEventListener implements Listener
{
	private final PluginMain plugin;
	private final PermissionCheck permissionCheck;
	private final ResultAction inventoryOpenAction = new InventoryOpenAction();
	private final ResultAction quickLootAction = new QuickLootAction();


	public PlayerEventListener(final PluginMain plugin)
	{
		this.plugin = plugin;
		this.permissionCheck = new PermissionCheck(plugin);
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}


	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerDeath(final PlayerDeathEvent event)
	{
		Player player = event.getEntity();

		final Location deathLocSnapshot = player.getLocation().clone();

		if (!plugin.worldManager.isEnabled(player.getWorld()))
		{
			sendDelayedDeathMessage(player, MessageId.CHEST_DENIED_WORLD_DISABLED, deathLocSnapshot);
			return;
		}

		if (!player.hasPermission("deathchest.chest"))
		{
			sendDelayedDeathMessage(player, MessageId.CHEST_DENIED_PERMISSION, deathLocSnapshot);
			return;
		}

		if (permissionCheck.isCreativeDeployDisabled(player))
		{
			sendDelayedDeathMessage(player, MessageId.CREATIVE_MODE, deathLocSnapshot);
			return;
		}

		if (event.getDrops().isEmpty())
		{
			sendDelayedDeathMessage(player, MessageId.INVENTORY_EMPTY, deathLocSnapshot);
			return;
		}


		if (plugin.getConfig().getBoolean("log-inventory-on-death"))
		{
			plugin.getLogger().info(player.getDisplayName() + " death inventory:");
			plugin.getLogger().info(event.getDrops().toString());
		}

		Collection<ItemStack> droppedItems = new LinkedList<>(event.getDrops());
		event.getDrops().clear();

		final Player pvpKiller = resolvePvpKiller(player);
		final Location deathLocation = player.getLocation().clone();
		final Location targetLocation = resolveChestOriginLocation(player, pvpKiller);

		new DeployChestTask(plugin, player, droppedItems, targetLocation, deathLocation, pvpKiller)
				.runTaskLater(plugin, plugin.getConfig().getInt("chest-deployment-delay"));
	}


	private Location resolveChestOriginLocation(final Player player, final Player pvpKiller)
	{
		final Location deathLocation = player.getLocation().clone();
		final boolean killerLooting = plugin.getConfig().getBoolean("killer-looting", false);

		if (killerLooting && pvpKiller != null)
		{
			return deathLocation;
		}

		final String locationMode = plugin.getConfig().getString("chest-location", "DEATH").trim().toUpperCase();
		if ("RESPAWN".equals(locationMode))
		{
			Location respawn = player.getRespawnLocation();
			if (respawn == null)
			{
				final World primaryWorld = Bukkit.getWorlds().isEmpty() ? player.getWorld() : Bukkit.getWorlds().get(0);
				respawn = primaryWorld.getSpawnLocation();
			}
			return respawn.clone();
		}

		return deathLocation;
	}


	private Player resolvePvpKiller(final Player victim)
	{
		final Player directKiller = victim.getKiller();
		if (directKiller != null && !directKiller.getUniqueId().equals(victim.getUniqueId()))
		{
			return directKiller;
		}

		final EntityDamageEvent lastDamage = victim.getLastDamageCause();
		if (lastDamage instanceof EntityDamageByEntityEvent byEntity)
		{
			final Entity damager = byEntity.getDamager();

			if (damager instanceof Player attacker && !attacker.getUniqueId().equals(victim.getUniqueId()))
			{
				return attacker;
			}
			if (damager instanceof Projectile projectile)
			{
				final ProjectileSource shooter = projectile.getShooter();
				if (shooter instanceof Player attacker && !attacker.getUniqueId().equals(victim.getUniqueId()))
				{
					return attacker;
				}
			}
			if (damager instanceof Tameable tameable && tameable.isTamed() && tameable.getOwner() instanceof Player owner)
			{
				if (!owner.getUniqueId().equals(victim.getUniqueId()))
				{
					return owner;
				}
			}
		}

		return null;
	}


	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteract(final PlayerInteractEvent event)
	{
		final DeathChest deathChest = plugin.chestManager.getChest(event.getClickedBlock());

		if (deathChest instanceof ValidDeathChest validDeathChest)
		{
			final Player player = event.getPlayer();

			if (isPlayerQuickLooting(event))
			{
				permissionCheck.performChecks(event, player, validDeathChest, quickLootAction);
				return;
			}

			if (isPlayerOpeningInventory(event))
			{
				permissionCheck.performChecks(event, player, validDeathChest, inventoryOpenAction);
			}
		}
	}


	private boolean isPlayerQuickLooting(final PlayerInteractEvent event)
	{
		return (event.getAction().equals(Action.LEFT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
				&& event.getPlayer().isSneaking()
				&& plugin.getConfig().getBoolean("quick-loot")
				&& event.getPlayer().hasPermission("deathchest.loot");
	}


	private boolean isPlayerOpeningInventory(final PlayerInteractEvent event)
	{
		return event.getAction().equals(Action.RIGHT_CLICK_BLOCK);
	}
	private void sendDelayedDeathMessage(final Player player, final MessageId messageId, final Location location)
	{
		plugin.getServer().getScheduler().runTaskLater(plugin, () ->
		{
			if (!com.winterhavenmc.deathchest.plugin.chests.deployment.AbstractDeployment
					.sendLanguageMessage(plugin, player, messageId, location, null))
			{
				final Player recipient = plugin.getServer().getPlayer(player.getUniqueId());
				if (recipient != null && recipient.isOnline())
				{
					plugin.messageBuilder.compose(recipient, messageId)
							.setMacro(Macro.LOCATION, location)
							.send();
				}
			}
		}, 1L);
	}
}
