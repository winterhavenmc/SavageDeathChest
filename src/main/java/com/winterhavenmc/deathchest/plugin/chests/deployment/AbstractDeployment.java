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
import com.winterhavenmc.deathchest.plugin.chests.ChestBlockType;
import com.winterhavenmc.deathchest.plugin.chests.LocationUtilities;
import com.winterhavenmc.deathchest.plugin.chests.search.SearchResult;
import com.winterhavenmc.deathchest.plugin.chests.search.SearchResultCode;
import com.winterhavenmc.deathchest.plugin.messages.Macro;
import com.winterhavenmc.deathchest.plugin.messages.MessageId;
import com.winterhavenmc.deathchest.plugin.models.chestblock.ChestBlock;
import com.winterhavenmc.deathchest.plugin.models.chestblock.ValidChestBlock;
import com.winterhavenmc.deathchest.plugin.models.deathchest.ValidDeathChest;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;


/**
 * Abstract class with common methods required for the deployment of a death chest
 */
public abstract class AbstractDeployment implements Deployment
{
	protected final PluginMain plugin;
	protected final Player player;
	protected final Collection<ItemStack> droppedItems;
	protected final Location originLocation;
	protected final Player killer;


	/**
	 * Class constructor
	 *
	 * @param plugin reference to plugin main class
	 * @param player the player for whom chest is being deployed
	 * @param droppedItems the player's death drops
	 */
	public AbstractDeployment(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems)
	{
		this(plugin, player, droppedItems, player.getLocation(), player.getKiller());
	}


	/**
	 * Class constructor with custom origin location
	 *
	 * @param plugin reference to plugin main class
	 * @param player the player for whom chest is being deployed
	 * @param droppedItems the player's death drops
	 * @param originLocation the target origin location for chest deployment
	 */
	public AbstractDeployment(final PluginMain plugin, final Player player, final Collection<ItemStack> droppedItems, final Location originLocation, final Player killer)
	{
		this.plugin = plugin;
		this.player = player;
		this.droppedItems = droppedItems;
		this.originLocation = (originLocation != null) ? originLocation : player.getLocation();
		this.killer = killer;
	}


	/**
	 * Execute deployment of death chest
	 *
	 * @return the result of the attempted death chest deployment
	 */
	public abstract SearchResult deploy();


	/**
	 * Check if Collection of ItemStack contains at least one chest
	 *
	 * @param itemStacks Collection of ItemStack to check for chest
	 * @return boolean - {@code true} if collection contains at least one chest, {@code false} if not
	 */
	boolean containsChest(final Collection<ItemStack> itemStacks)
	{
		if (itemStacks == null)
		{
			return false;
		}

		boolean result = false;
		for (ItemStack itemStack : itemStacks)
		{
			if (itemStack.getType().equals(Material.CHEST))
			{
				result = true;
				break;
			}
		}
		return result;
	}


	/**
	 * Remove one chest from list of item stacks.
	 */
	Collection<ItemStack> removeOneChest(final Collection<ItemStack> itemStacks)
	{
		Collection<ItemStack> remainingItems = new LinkedList<>(itemStacks);

		Iterator<ItemStack> iterator = remainingItems.iterator();

		while (iterator.hasNext())
		{
			ItemStack itemStack = iterator.next();

			if (itemStack.getType().equals(Material.CHEST) && !itemStack.hasItemMeta())
			{
				if (itemStack.getAmount() == 1)
				{
					iterator.remove();
				}
				else
				{
					itemStack.setAmount(itemStack.getAmount() - 1);
				}
				break;
			}
		}
		return remainingItems;
	}
	/**
	 * Place a chest block and fill with items
	 *
	 * @param chestBlockType the type of chest block (left or right)
	 */
	void placeChest(final ValidDeathChest deathChest, final Location location, final ChestBlockType chestBlockType)
	{
		// get current block at location
		Block block = location.getBlock();

		// set block material to chest
		block.setType(Material.CHEST);

		// set custom inventory name
		setCustomInventoryName(player, block);

		// set chest direction
		setChestDirection(block, originLocation);

		// create new ChestBlock object
		ChestBlock chestBlock = ChestBlock.of(deathChest.chestUid(), block.getLocation(), chestBlockType);

		if (chestBlock instanceof ValidChestBlock validChestBlock)
		{
			// add this ChestBlock to block map
			plugin.chestManager.putBlock(chestBlockType, validChestBlock);

			// set block metadata
			plugin.chestManager.setMetadata(validChestBlock, deathChest);
		}
	}


	/**
	 * Conditional check for death chest required config and permission
	 *
	 * @return boolean - true if chest is required, false if not
	 */
	boolean chestRequired()
	{
		return plugin.getConfig().getBoolean("require-chest")
				&& !player.hasPermission("deathchest.freechest");
	}


	/**
	 * Set custom inventory name for chest
	 *
	 * @param player the owner of the chest
	 * @param block the chest block
	 */
	@SuppressWarnings("deprecation")
	private void setCustomInventoryName(final Player player, final Block block)
	{
		String rawName = plugin.getConfig().getString("chest-inventory-name", "&8Сундук смерти {OWNER}");
		String formatted = ChatColor.translateAlternateColorCodes('&', rawName.replace("{OWNER}", player.getName()));
		if (block.getState() instanceof org.bukkit.block.Chest chestState)
		{
			chestState.setCustomName(formatted);
			chestState.update();
		}
	}


	/**
	 * Set the block state for a death chest block
	 *
	 * @param block the chest block
	 * @param chestType the chest block type ( SINGLE, LEFT, RIGHT )
	 */
	void setChestBlockState(Block block, Chest.Type chestType)
	{
		BlockState chestBlockState = block.getState();
		Chest chestBlockData = (Chest) chestBlockState.getBlockData();
		chestBlockData.setType(chestType);
		chestBlockState.setBlockData(chestBlockData);
		chestBlockState.update();
	}


	/**
	 * Set chest facing direction from origin location
	 *
	 * @param block the chest block
	 * @param location the origin location
	 */
	private void setChestDirection(final Block block, final Location location)
	{
		Directional blockData = (Directional) block.getBlockData();
		blockData.setFacing(LocationUtilities.getCardinalBlockFace(location));
		block.setBlockData(blockData);
	}


	/**
	 * Finish a death chest deployment by sending messages and storing death chest in datastore
	 *
	 * @param searchResult the search result of the deployment
	 * @param validDeathChest the death chest object
	 */
	void finalize(final SearchResult searchResult, final ValidDeathChest validDeathChest)
	{
		if (plugin.getConfig().getBoolean("debug"))
		{
			logResult(searchResult);
		}

		sendResultMessage(player, validDeathChest, searchResult);

		if (!searchResult.getResultCode().equals(SearchResultCode.SUCCESS)
				&& !searchResult.getResultCode().equals(SearchResultCode.PARTIAL_SUCCESS))
		{
			return;
		}

		final Location dropLocation = (searchResult.getLocation() != null) ? searchResult.getLocation() : originLocation;
		for (ItemStack item : searchResult.getRemainingItems())
		{
			if (dropLocation.getWorld() != null)
			{
				dropLocation.getWorld().dropItemNaturally(dropLocation, item);
			}
		}

		if (!searchResult.getResultCode().equals(SearchResultCode.SUCCESS)
				&& !searchResult.getResultCode().equals(SearchResultCode.PARTIAL_SUCCESS))
		{
			return;
		}

		long chestProtectionTime = plugin.getConfig().getLong("chest-protection-time");

		if (chestProtectionTime == 0)
		{
			chestProtectionTime = -1;
		}

		if (plugin.getConfig().getBoolean("chest-protection") && chestProtectionTime > 0)
		{
			plugin.messageBuilder.compose(player, MessageId.CHEST_DEPLOYED_PROTECTION_TIME)
					.setMacro(Macro.DEATH_CHEST, validDeathChest)
					.send();
		}

		plugin.chestManager.putChest(validDeathChest);

		Set<ValidDeathChest> deathChests = Collections.singleton(validDeathChest);
		plugin.chestManager.saveChestRecords(deathChests);
	}


	/**
	 * Send player message with result of death chest deployment
	 *
	 * @param player the player to send message
	 * @param validDeathChest the death chest object for player
	 * @param result the search result of the attempted deployment
	 */
	private void sendResultMessage(final Player player, final ValidDeathChest validDeathChest, final SearchResult result)
	{
		final Player recipient = plugin.getServer().getPlayer(player.getUniqueId());
		if (recipient == null || !recipient.isOnline())
		{
			return;
		}

		final MessageId messageId = switch (result.getResultCode())
		{
			case SUCCESS -> MessageId.CHEST_SUCCESS;
			case PARTIAL_SUCCESS -> MessageId.DOUBLECHEST_PARTIAL_SUCCESS;
			case PROTECTION_PLUGIN -> MessageId.CHEST_DENIED_DEPLOYMENT_BY_PLUGIN;
			case ABOVE_GRASS_PATH, NON_REPLACEABLE_BLOCK -> MessageId.CHEST_DENIED_BLOCK;
			case ADJACENT_CHEST -> MessageId.CHEST_DENIED_ADJACENT_CHEST;
			case NO_CHEST -> MessageId.NO_CHEST_IN_INVENTORY;
			case SPAWN_RADIUS -> MessageId.CHEST_DENIED_SPAWN_PROTECTION;
			case VOID -> MessageId.CHEST_DENIED_VOID;
			case WORLD_HEIGHT -> MessageId.CHEST_DENIED_WORLD_HEIGHT;
			default -> null;
		};

		final org.bukkit.Location loc = (validDeathChest != null) ? validDeathChest.getLocation() : result.getLocation();
		final String pluginName = (result.getProtectionPlugin() != null) ? String.valueOf(result.getProtectionPlugin()) : null;
		if (messageId != null && sendLanguageMessage(plugin, recipient, messageId, loc, pluginName))
		{
			return;
		}

		switch (result.getResultCode())
		{
			case SUCCESS -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_SUCCESS)
					.setMacro(Macro.DEATH_CHEST, validDeathChest)
					.send();
			case PARTIAL_SUCCESS -> plugin.messageBuilder.compose(recipient, MessageId.DOUBLECHEST_PARTIAL_SUCCESS)
					.setMacro(Macro.DEATH_CHEST, validDeathChest)
					.send();
			case PROTECTION_PLUGIN -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_DEPLOYMENT_BY_PLUGIN)
					.setMacro(Macro.LOCATION, result.getLocation())
					.setMacro(Macro.PLUGIN, result.getProtectionPlugin())
					.send();
			case ABOVE_GRASS_PATH, NON_REPLACEABLE_BLOCK ->
					plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_BLOCK)
							.setMacro(Macro.LOCATION, result.getLocation())
							.send();
			case ADJACENT_CHEST -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_ADJACENT_CHEST)
					.setMacro(Macro.LOCATION, result.getLocation())
					.send();
			case NO_CHEST -> plugin.messageBuilder.compose(recipient, MessageId.NO_CHEST_IN_INVENTORY)
					.setMacro(Macro.LOCATION, result.getLocation())
					.send();
			case SPAWN_RADIUS -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_SPAWN_PROTECTION)
					.setMacro(Macro.LOCATION, result.getLocation())
					.send();
			case VOID -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_VOID)
					.setMacro(Macro.LOCATION, result.getLocation())
					.send();
			case WORLD_HEIGHT -> plugin.messageBuilder.compose(recipient, MessageId.CHEST_DENIED_WORLD_HEIGHT)
					.setMacro(Macro.LOCATION, result.getLocation())
					.send();
		}
	}

	public static boolean sendLanguageMessage(final PluginMain plugin, final Player player, final MessageId messageId,
											  final org.bukkit.Location loc, final String protectionPlugin)
	{
		if (player == null)
		{
			return false;
		}
		final Player recipient = plugin.getServer().getPlayer(player.getUniqueId());
		if (recipient == null || !recipient.isOnline())
		{
			return false;
		}

		final String lang = plugin.getConfig().getString("language", "en-US");
		final java.io.File langFile = new java.io.File(plugin.getDataFolder(), "language/" + lang + ".yml");
		if (!langFile.exists())
		{
			return false;
		}

		final org.bukkit.configuration.file.YamlConfiguration yaml =
				org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(langFile);
		final String key = messageId.name();

		final String[] candidateBases = { key, "MESSAGES." + key, "messages." + key };
		String foundBase = null;
		for (final String base : candidateBases)
		{
			if (yaml.contains(base))
			{
				foundBase = base;
				break;
			}
		}

		if (foundBase == null)
		{
			return false;
		}
		if (!yaml.getBoolean(foundBase + ".enabled", true))
		{
			return true;
		}

		String text = yaml.getString(foundBase + ".string");
		if (text == null) text = yaml.getString(foundBase + ".message");
		if (text == null) text = yaml.getString(foundBase + ".text");
		if (text == null && yaml.isString(foundBase)) text = yaml.getString(foundBase);
		if (text == null || text.isEmpty())
		{
			return false;
		}

		if (loc != null)
		{
			final String worldName = (loc.getWorld() != null) ? loc.getWorld().getName() : "world";
			text = text.replace("%WORLD%", worldName).replace("{WORLD}", worldName)
					.replace("%X%", String.valueOf(loc.getBlockX())).replace("{X}", String.valueOf(loc.getBlockX()))
					.replace("%Y%", String.valueOf(loc.getBlockY())).replace("{Y}", String.valueOf(loc.getBlockY()))
					.replace("%Z%", String.valueOf(loc.getBlockZ())).replace("{Z}", String.valueOf(loc.getBlockZ()));
		}

		final long expireMinutes = plugin.getConfig().getLong("expire-time", 60);
		final String expireFormatted;
		if (expireMinutes <= 0)
		{
			expireFormatted = yaml.getString("TIME_STRINGS.UNLIMITED", "unlimited time");
		}
		else if (expireMinutes >= 1440 && expireMinutes % 1440 == 0)
		{
			final long days = expireMinutes / 1440;
			final String unit = yaml.getString(days == 1 ? "TIME_STRINGS.DAY" : "TIME_STRINGS.DAY_PLURAL", days == 1 ? "day" : "days");
			expireFormatted = days + " " + unit;
		}
		else if (expireMinutes >= 60 && expireMinutes % 60 == 0)
		{
			final long hours = expireMinutes / 60;
			final String unit = yaml.getString(hours == 1 ? "TIME_STRINGS.HOUR" : "TIME_STRINGS.HOUR_PLURAL", hours == 1 ? "hour" : "hours");
			expireFormatted = hours + " " + unit;
		}
		else
		{
			final String unit = yaml.getString(expireMinutes == 1 ? "TIME_STRINGS.MINUTE" : "TIME_STRINGS.MINUTE_PLURAL", expireMinutes == 1 ? "minute" : "minutes");
			expireFormatted = expireMinutes + " " + unit;
		}

		text = text.replace("%EXPIRATION_DURATION%", expireFormatted)
				.replace("{EXPIRATION_DURATION}", expireFormatted)
				.replace("%EXPIRE_TIME%", expireFormatted)
				.replace("{EXPIRE_TIME}", expireFormatted);

		if (protectionPlugin != null)
		{
			text = text.replace("%PLUGIN%", protectionPlugin).replace("{PLUGIN}", protectionPlugin);
		}

		recipient.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', text));
		return true;
	}


	/**
	 * Log result of a death chest deployment for debugging purposes
	 *
	 * @param result the search result of a death chest deployment
	 */
	private void logResult(final SearchResult result)
	{
		if (result == null)
		{
			plugin.getLogger().info("SearchResult is null!");
			return;
		}

		if (result.getResultCode() != null)
		{
			plugin.getLogger().info("SearchResult Code: " + result.getResultCode());
		}

		if (result.getLocation() != null)
		{
			plugin.getLogger().info("Location: " + result.getLocation());
		}

		if (result.getProtectionPlugin() != null)
		{
			plugin.getLogger().info("Protection Plugin: " + result.getProtectionPlugin());
		}

		if (result.getRemainingItems() != null)
		{
			plugin.getLogger().info("Remaining Items: " + result.getRemainingItems());
		}
	}

}
