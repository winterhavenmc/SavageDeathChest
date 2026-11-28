package com.winterhavenmc.deathchest.plugin.tasks;

import com.winterhavenmc.deathchest.plugin.PluginMain;
import com.winterhavenmc.deathchest.plugin.chests.search.SearchResult;
import com.winterhavenmc.deathchest.plugin.chests.search.SearchResultCode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;


public final class DeployChestTask extends BukkitRunnable
{
	private final PluginMain plugin;
	private final Player player;
	private final Collection<ItemStack> droppedItems;
	private final Location originLocation;
	private final Location deathLocation;
	private final Player killer;


	public DeployChestTask(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems)
	{
		this(plugin, player, droppedItems, player.getLocation(), player.getLocation(), player.getKiller());
	}


	public DeployChestTask(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation)
	{
		this(plugin, player, droppedItems, originLocation, originLocation, player.getKiller());
	}


	public DeployChestTask(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation, final Player killer)
	{
		this(plugin, player, droppedItems, originLocation, originLocation, killer);
	}


	public DeployChestTask(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation, final Location deathLocation, final Player killer)
	{
		this.plugin = plugin;
		this.player = player;
		this.droppedItems = droppedItems;
		this.originLocation = originLocation;
		this.deathLocation = (deathLocation != null) ? deathLocation : originLocation;
		this.killer = killer;
	}


	@Override
	public void run()
	{
		if (plugin.getConfig().getBoolean("require-chest", true))
		{
			boolean hasChest = false;
			for (ItemStack item : droppedItems)
			{
				if (item != null && item.getType() == org.bukkit.Material.CHEST && item.getAmount() > 0)
				{
					hasChest = true;
					break;
				}
			}

			if (!hasChest)
			{
				final Location dropLoc = (deathLocation != null) ? deathLocation : originLocation;
				if (dropLoc != null && dropLoc.getWorld() != null)
				{
					for (ItemStack item : droppedItems)
					{
						if (item != null)
						{
							dropLoc.getWorld().dropItemNaturally(dropLoc, item);
						}
					}
				}

				com.winterhavenmc.deathchest.plugin.chests.deployment.AbstractDeployment.sendLanguageMessage(
						plugin,
						player,
						com.winterhavenmc.deathchest.plugin.messages.MessageId.NO_CHEST_IN_INVENTORY,
						dropLoc,
						null
				);
				return;
			}
		}
		final SearchResult result = plugin.chestManager.getDeploymentFactory()
				.createDeployment(plugin, player, droppedItems, originLocation, killer)
				.deploy();

		if (result != null
				&& !result.getResultCode().equals(SearchResultCode.SUCCESS)
				&& !result.getResultCode().equals(SearchResultCode.PARTIAL_SUCCESS))
		{
			final Location dropLoc = (deathLocation != null) ? deathLocation : originLocation;
			if (dropLoc != null && dropLoc.getWorld() != null)
			{
				for (ItemStack item : result.getRemainingItems())
				{
					if (item != null)
					{
						dropLoc.getWorld().dropItemNaturally(dropLoc, item);
					}
				}
			}

			if (result.getResultCode().equals(SearchResultCode.NO_CHEST))
			{
				com.winterhavenmc.deathchest.plugin.chests.deployment.AbstractDeployment.sendLanguageMessage(
						plugin,
						player,
						com.winterhavenmc.deathchest.plugin.messages.MessageId.NO_CHEST_IN_INVENTORY,
						dropLoc,
						null
				);
			}
		}
	}
}
