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

package com.winterhavenmc.deathchest.chests;

import com.winterhavenmc.deathchest.PluginMain;
import com.winterhavenmc.deathchest.chests.deployment.DeploymentFactory;
import com.winterhavenmc.deathchest.messages.Macro;
import com.winterhavenmc.deathchest.messages.MessageId;
import com.winterhavenmc.deathchest.sounds.SoundId;
import com.winterhavenmc.deathchest.storage.SQLiteDataStore;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.*;


/**
 * A class that tracks the state of death chests and their component blocks
 */
public final class ChestManager
{
	// reference to main class
	private final PluginMain plugin;

	// map of death chests
	private final ChestIndex chestIndex;

	// map of chest blocks
	private final BlockIndex blockIndex;

	// instantiate datastore
	private final SQLiteDataStore dataStore;

	// set of replaceable blocks
	private final ReplaceableBlocks replaceableBlocks;

	// DeathChest material types
	final static Collection<Material> deathChestMaterials = Set.of(
			Material.CHEST,
			Material.OAK_WALL_SIGN,
			Material.OAK_SIGN );

	private final DeploymentFactory deploymentFactory;


	/**
	 * Class constructor
	 *
	 * @param plugin reference to main class
	 */
	public ChestManager(final PluginMain plugin)
	{
		// set reference to main class
		this.plugin = plugin;

		// initialize replaceableBlocks
		replaceableBlocks = new ReplaceableBlocks(plugin);

		// initialize datastore
		dataStore = SQLiteDataStore.connect(plugin);

		// initialize chestIndex
		chestIndex = new ChestIndex();

		// initialize blockIndex
		blockIndex = new BlockIndex();

		deploymentFactory = new DeploymentFactory();
	}

	public DeploymentFactory getDeploymentFactory()
	{
		return this.deploymentFactory;
	}

	/**
	 * Load death chest blocks from datastore.
	 * Expire death chest blocks whose time has passed.
	 * schedule tasks to expire remaining loaded chests.
	 */
	public void loadChests()
	{
		if (plugin.getConfig().getBoolean("debug"))
		{
			plugin.getLogger().info("Loading Death Chests...");
		}

		// populate chestIndex with all death chest records retrieved from datastore
		for (DeathChestRecord deathChest : dataStore.deathChests().getAll())
		{
			this.putChest(deathChest);
		}

		// populate chest block map with all valid chest blocks retrieved from datastore
		for (ChestBlock chestBlock : dataStore.chestBlocks().getAll())
		{
			// if chest block location is null, continue to next chest block
			if (chestBlock.getLocation() == null)
			{
				if (plugin.getConfig().getBoolean("debug"))
				{
					plugin.getLogger().info("chest block " + chestBlock.getChestUid() + " has null location.");
				}
				continue;
			}

			// get chest block type from in game block
			ChestBlockType chestBlockType = ChestBlockType.getType(chestBlock.getLocation().getBlock());

			// if chest block type is null or parent chest not in chest map, delete block record
			if (chestBlockType == null || !chestIndex.containsKey(chestBlock.getChestUid()))
			{
				dataStore.chestBlocks().delete(chestBlock);
			}
			else
			{
				// add chestBlock to block index
				this.blockIndex.put(chestBlockType, chestBlock);
			}
		}

		// expire chests with no blocks or past expiration
		for (DeathChestRecord deathChest : chestIndex.values())
		{
			// if DeathChest has no children, remove from index and datastore
			if (this.getBlocks(deathChest.chestUid()).isEmpty())
			{
				chestIndex.remove(deathChest);
				dataStore.deathChests().delete(deathChest);
			}
			// if DeathChest is past expiration (not infinite, denoted by Instant.EPOCH), expire chest
			else if (deathChest.expirationTime().isAfter(Instant.EPOCH) && deathChest.expirationTime().isBefore(Instant.now()))
			{
				plugin.chestManager.expire(deathChest);
			}
			else
			{
				// set chest metadata
				this.setBlockMetadata(this.getBlocks(deathChest.chestUid()));
				if (plugin.getConfig().getBoolean("debug")) {
					plugin.getLogger().info("[loadDeathChests] Setting metadata for chest " + deathChest.chestUid());
				}
			}
		}
	}


	/**
	 * Put DeathChest object in map
	 *
	 * @param deathChest the DeathChest object to put in map
	 */
	public void putChest(final DeathChestRecord deathChest)
	{
		this.chestIndex.put(plugin, deathChest);
	}


	/**
	 * Get DeathChest object by chestUUID
	 *
	 * @param chestUUID UUID of DeathChest object to retrieve
	 * @return DeathChest object, or null if no DeathChest exists in map with passed chestUUID
	 */
	public DeathChestRecord getChest(final UUID chestUUID)
	{
		return this.chestIndex.get(chestUUID);
	}


	/**
	 * Get DeathChest object by block
	 *
	 * @param block the block to retrieve DeathChest object
	 * @return DeathChest object, or null if no DeathChest exists in map that contains passed block location
	 */
	public DeathChestRecord getChest(final Block block)
	{
		// if passed block is null, return null
		if (block == null)
		{
			return null;
		}

		// get chest block from index by location
		ChestBlock chestBlock = this.blockIndex.get(block.getLocation());

		// return death chest referenced by uid in chest block
		return (chestBlock != null)
				? getChest(chestBlock.getChestUid())
				: null;
	}


	public DeathChestRecord getChest(final Inventory inventory)
	{
		// if inventory is not a death chest, do nothing and return
		if (!plugin.chestManager.isDeathChestInventory(inventory))
		{
			return null;
		}

		// get inventory holder block (death chest)
		Block block = null;

		// if inventory is a chest, get chest block
		if (inventory.getHolder() instanceof Chest chest)
		{
			block = chest.getBlock();
		}

		// return death chest for block (returns null if block is not valid chest block)
		return getChest(block);
	}


	/**
	 * Remove DeathChest object from chest index
	 *
	 * @param deathChest the DeathChest object to remove from map
	 */
	void removeChest(final DeathChestRecord deathChest)
	{
		this.chestIndex.remove(deathChest);
	}


	/**
	 * Put ChestBlock object in block index
	 *
	 * @param chestBlock the ChestBlock to put in map
	 */
	public void putBlock(final ChestBlockType chestBlockType, final ChestBlock chestBlock)
	{
		this.blockIndex.put(chestBlockType, chestBlock);
	}


	/**
	 * Get ChestBlock object from block index by location
	 *
	 * @param location the location to retrieve ChestBlock object
	 * @return ChestBlock object, or null if no ChestBlock exists in map with passed location
	 */
	@SuppressWarnings("unused")
	public ChestBlock getBlock(final Location location)
	{
		return this.blockIndex.get(location);
	}


	/**
	 * Get chestBlock set from block index by chest uuid
	 *
	 * @param chestUid the UUID of the chest of which to retrieve a set of chest blocks
	 * @return Set of Blocks in uuidBlockMap, or empty set if no blocks exist for chest UUID
	 */
	public Collection<ChestBlock> getBlocks(final UUID chestUid)
	{
		return this.blockIndex.getBlocks(chestUid);
	}


	/**
	 * Get chestBlock map from block index by chest uuid
	 *
	 * @param chestUid the UUID of the chest of which to retrieve a map of chest blocks
	 * @return Map of Blocks in uuidBlockMap, or empty map if no blocks exist for chest UUID
	 */
	Map<ChestBlockType, ChestBlock> getBlockMap(final UUID chestUid)
	{
		return this.blockIndex.getBlockMap(chestUid);
	}


	/**
	 * Remove ChestBlock object from map
	 *
	 * @param chestBlock the ChestBlock object to remove from map
	 */
	void removeBlock(final ChestBlock chestBlock)
	{
		this.blockIndex.remove(chestBlock);
	}


	/**
	 * Test if ChestBlock exists in map with passed block location
	 *
	 * @param block the block to check for existence in block index
	 * @return {@code true} if a ChestBlock exists in map with passed block location,
	 * {@code false} if no ChestBlock exists in map with passed block location
	 */
	public boolean isChestBlock(final Block block)
	{
		// if passed block is null, return false
		if (block == null)
		{
			return false;
		}

		// confirm block is death chest material
		if (!deathChestMaterials.contains(block.getType()))
		{
			return false;
		}

		// if passed block location is in chest block map return true, else return false
		return this.blockIndex.containsKey(block.getLocation());
	}


	/**
	 * Test if a block is a DeathChest chest block
	 *
	 * @param block The block to test
	 * @return {@code true} if block is Material.CHEST and block location exists in block index, {@code false} if not
	 */
	public boolean isChestBlockChest(final Block block)
	{
		// if passed block is null return false
		if (block == null)
		{
			return false;
		}

		// if passed block is chest and is in block map, return true; else return false
		return (block.getType().equals(Material.CHEST) && blockIndex.containsKey(block.getLocation()));
	}


	/**
	 * Test if a block is a deathchest sign
	 *
	 * @param block The block to test if it is a DeathSign
	 * @return {@code true} if block is Material.SIGN or Material.WALL_SIGN and block location exists in block index,
	 * {@code false} if not
	 */
	public boolean isChestBlockSign(final Block block)
	{
		// if passed block is null return false
		if (block == null)
		{
			return false;
		}

		// get block state
		BlockState blockState = block.getState();

		// if block is sign or wall sign and exists in block index, return true
		return ((blockState instanceof WallSign
				|| blockState instanceof Sign)
				&& blockIndex.containsKey(block.getLocation()));
	}


	/**
	 * Test if an inventory is a death chest inventory
	 *
	 * @param inventory The inventory whose holder will be tested to see if it is a DeathChest
	 * @return {@code true} if the inventory's holder is a DeathChest, {@code false} if not
	 */
	public boolean isDeathChestInventory(final Inventory inventory)
	{
		// if passed inventory is null, return false
		if (inventory == null)
		{
			return false;
		}

		// if inventory type is not a chest inventory, return false
		if (!inventory.getType().equals(InventoryType.CHEST))
		{
			return false;
		}

		// if inventory holder is null, return false
		if (inventory.getHolder() == null)
		{
			return false;
		}

		// try to get inventory holder block
		Block block;

		if (inventory.getHolder() instanceof Chest)
		{
			Chest chest;
			chest = (Chest) inventory.getHolder();
			block = chest.getBlock();
		}
		else if (inventory.getHolder() instanceof DoubleChest)
		{
			DoubleChest doubleChest;
			doubleChest = (DoubleChest) inventory.getHolder();
			block = doubleChest.getLocation().getBlock();
		}
		else
		{
			return false;
		}

		// if inventory holder block is a DeathChest return true, else return false
		return this.isChestBlockChest(block);
	}


	/**
	 * Get all death chests in chest index
	 * @return Collection of DeathChest - all death chests in the chest index
	 */
	public Collection<DeathChestRecord> getAllChests()
	{
		return this.chestIndex.values();
	}


	public void insertChestRecords(final Collection<DeathChestRecord> deathChests)
	{
		// get chestBlocks for all deathChests
		Set<ChestBlock> chestBlocks = new HashSet<>();
		for (DeathChestRecord deathChestRecord : deathChests)
		{
			chestBlocks.addAll(getBlocks(deathChestRecord.chestUid()));
		}

		dataStore.deathChests().save(deathChests);
		dataStore.chestBlocks().save(chestBlocks);
	}


	public void deleteBlockRecord(final ChestBlock chestBlock)
	{
		dataStore.chestBlocks().delete(chestBlock);
	}


	public void deleteChestRecord(final DeathChestRecord deathChest)
	{
		dataStore.deathChests().delete(deathChest);
	}


	public void closeDataStore()
	{
		dataStore.close();
	}


	public void reload()
	{
		replaceableBlocks.reload();
	}

	@SuppressWarnings("unused")

	public boolean isReplaceableBlock(final Material material)
	{
		return replaceableBlocks.contains(material);
	}


	public boolean isReplaceableBlock(final Block block)
	{
		return replaceableBlocks.contains(block.getType());
	}


	public ReplaceableBlocks getReplaceableBlocks()
	{
		return replaceableBlocks;
	}


	public int getChestCount()
	{
		return this.dataStore.deathChests().getCount();
	}


	/**
	 * Set chest metadata on all component blocks
	 */
	void setBlockMetadata(Collection<ChestBlock> chestBlocks)
	{
		// set metadata on blocks in set
		for (ChestBlock chestBlock : chestBlocks)
		{
			chestBlock.setMetadata(getChest(chestBlock.getChestUid()));
		}
	}


	/**
	 * Check if protection is enabled and has expired
	 * @return boolean - true if protection has expired, false if not
	 */
	public boolean protectionExpired(DeathChestRecord chestRecord)
	{
		return plugin.getConfig().getBoolean("chest-protection")
				&& plugin.getConfig().getInt("chest-protection-time") > 0
				&& chestRecord.protectionTime().isBefore(Instant.now());
	}


	/**
	 * Getter method for DeathChest expireTaskId
	 *
	 * @return the value of the expireTaskId field in the DeathChest object
	 */
	int getExpireTaskId(DeathChestRecord deathChest)
	{
		return chestIndex.getExpireTaskId(deathChest);
	}


	/**
	 * Cancel expire task for this death chest
	 */
	public void cancelExpireTask(DeathChestRecord deathChest)
	{
		// if task id is positive integer, cancel task
		if (getExpireTaskId(deathChest) > 0)
		{
			plugin.getServer().getScheduler().cancelTask(getExpireTaskId(deathChest));
		}
	}

	public Inventory getInventory(final DeathChestRecord deathChest)
	{
		Map<ChestBlockType, ChestBlock> blockMap = getBlockMap(deathChest.chestUid());
		return getInventory(blockMap);
	}


	/**
	 * Get inventory associated with this death chest
	 *
	 * @return Inventory - the inventory associated with this death chest;
	 * returns null if both right and left chest block inventories are invalid
	 */
	public Inventory getInventory(Map<ChestBlockType, ChestBlock> blockMap)
	{
		// get right chest inventory
		Inventory inventory = blockMap.get(ChestBlockType.RIGHT_CHEST).getInventory();

		// if right chest inventory is null, try left chest
		if (inventory == null)
		{
			inventory = blockMap.get(ChestBlockType.LEFT_CHEST).getInventory();
		}

		// return the inventory, or null if right and left chest inventories were both invalid
		return inventory;
	}


	/**
	 * Place collection of ItemStacks in chest, returning collection of ItemStacks that did not fit in chest
	 *
	 * @param itemStacks Collection of ItemStacks to place in chest
	 * @return Collection of ItemStacks that did not fit in chest
	 */
	public Collection<ItemStack> fill(final Collection<ItemStack> itemStacks, final DeathChestRecord deathChest)
	{
		// create empty list for return
		Collection<ItemStack> remainingItems = new LinkedList<>();

		// get inventory for this death chest
		Inventory inventory = getInventory(deathChest);

		// if inventory is not null, add itemStacks to inventory and put leftovers in remainingItems
		if (inventory != null)
		{
			remainingItems = new LinkedList<>(inventory.addItem(itemStacks.toArray(new ItemStack[0])).values());
		}

		// return collection of items that did not fit in inventory
		return remainingItems;
	}


	/**
	 * Expire this death chest, destroying in game chest and dropping contents,
	 * and sending message to chest owner if online.
	 */
	public void expire(final DeathChestRecord deathChest)
	{
		// get player from ownerUUID
		final Player player = plugin.getServer().getPlayer(deathChest.ownerUid());

		// destroy DeathChest
		this.destroy(deathChest);

		// if player is not null, send player message
		if (player != null)
		{
			plugin.messageBuilder.compose(player, MessageId.CHEST_EXPIRED)
					.setMacro(Macro.DEATH_CHEST, deathChest)
					.send();
		}
	}


	/**
	 * Destroy this death chest, dropping chest contents
	 */
	public void destroy(final DeathChestRecord deathChest)
	{
		this.dropContents(deathChest);

		// play chest break sound at chest location
		plugin.soundConfig.playSound(deathChest.getLocation(), SoundId.CHEST_BREAK);

		// get block map for this chest
		Map<ChestBlockType, ChestBlock> chestBlockMap = plugin.chestManager.getBlockMap(deathChest.chestUid());

		// destroy DeathChest blocks (sign gets destroyed first due to enum order, preventing detach before being destroyed)
		for (ChestBlock chestBlock : chestBlockMap.values())
		{
			chestBlock.destroy();
		}

		// delete DeathChest record from datastore
		this.deleteChestRecord(deathChest);

		// cancel expire block task
		if (this.getExpireTaskId(deathChest) > 0)
		{
			plugin.getServer().getScheduler().cancelTask(this.getExpireTaskId(deathChest));
		}

		// remove DeathChest from ChestManager DeathChest map
		plugin.chestManager.removeChest(deathChest);
	}


	public void dropContents(final DeathChestRecord deathChest)
	{
		if (deathChest == null) {
			return;
		}

		Location location = deathChest.getLocation();
		if (location == null)
		{
			return;
		}

		if (location.getWorld() != null)
		{
			ItemStack[] contents = this.getInventory(deathChest).getStorageContents();

			this.getInventory(deathChest).clear();

			for (ItemStack stack : contents)
			{
				if (stack !=null)
				{
					location.getWorld().dropItemNaturally(location, stack);
				}
			}
		}
	}


	/**
	 * Get the number of players currently viewing a DeathChest inventory
	 *
	 * @return The number of inventory viewers
	 */
	public int getViewerCount(final DeathChestRecord deathChest)
	{
		// get chest inventory
		Inventory inventory = this.getInventory(deathChest);

		// if inventory is not null, return viewer count
		if (inventory != null) {
			return inventory.getViewers().size();
		}
		else
		{
			// inventory is null, so return 0 for viewer count
			return 0;
		}
	}


	/**
	 * Transfer all chest contents to player inventory and remove in-game chest if empty.
	 * Items that do not fit in player inventory will be retained in chest.
	 *
	 * @param player the player whose inventory the chest contents will be transferred
	 */
	public void autoLoot(final Player player, final DeathChestRecord deathChest)
	{
		// if passed player or deathchest is null, do nothing and return
		if (player == null || deathChest == null)
		{
			return;
		}

		// create collection to hold items that did not fit in player inventory
		Collection<ItemStack> remainingItems = new ArrayList<>();

		// transfer contents of any chest blocks to player, putting any items that did not fit in remainingItems
		for (ChestBlock chestBlock : this.getBlocks(deathChest.chestUid()))
		{
			remainingItems.addAll(chestBlock.transferContents(player));
		}

		// if remainingItems is empty, all chest items fit in player inventory so destroy chest and return
		if (remainingItems.isEmpty())
		{
			plugin.chestManager.destroy(deathChest);
			return;
		}

		// send player message
		plugin.messageBuilder.compose(player, MessageId.INVENTORY_FULL)
				.setMacro(Macro.LOCATION, player.getLocation())
				.send();

		// try to put remaining items back in chest
		remainingItems = plugin.chestManager.fill(remainingItems, deathChest);

		// if remainingItems is still not empty, items could not be placed back in chest, so drop items at player location
		// this should never actually occur, but let's play it safe just in case
		if (!remainingItems.isEmpty())
		{
			for (ItemStack itemStack : remainingItems)
			{
				player.getWorld().dropItem(player.getLocation(), itemStack);
			}
		}
	}

}
