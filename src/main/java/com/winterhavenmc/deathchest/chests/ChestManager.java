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
import com.winterhavenmc.deathchest.models.chestblock.ValidChestBlock;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Instant;
import java.util.*;

import static com.winterhavenmc.deathchest.models.deathchest.DeathChest.INVALID_UUID;


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
		for (ValidDeathChest validDeathChest : dataStore.deathChests().getAll())
		{
			this.putChest(validDeathChest);
		}

		// populate chest block map with all valid chest blocks retrieved from datastore
		for (ValidChestBlock validChestBlock : dataStore.chestBlocks().getAll())
		{
			// if chest block location is null, continue to next chest block
			//TODO: confirm ValidChestBlock location is always valid, then remove this conditional block
			if (validChestBlock.getLocation() == null)
			{
				if (plugin.getConfig().getBoolean("debug"))
				{
					plugin.getLogger().info("chest block " + validChestBlock.getChestUid() + " has null location.");
				}
				continue;
			}

			// get chest block type from in game block
			ChestBlockType chestBlockType = ChestBlockType.getType(validChestBlock.getLocation().getBlock());

			// if chest block type is null or parent chest not in chest map, delete block record
			if (chestBlockType == null || !chestIndex.containsKey(validChestBlock.getChestUid()))
			{
				dataStore.chestBlocks().delete(validChestBlock);
			}
			else
			{
				// add validChestBlock to block index
				this.blockIndex.put(chestBlockType, validChestBlock);
			}
		}

		// expire chests with no blocks or past expiration
		for (ValidDeathChest validDeathChest : chestIndex.values())
		{
			// if DeathChest has no children, remove from index and datastore
			if (this.getBlocks(validDeathChest.chestUid()).isEmpty())
			{
				chestIndex.remove(validDeathChest);
				dataStore.deathChests().delete(validDeathChest);
			}
			// if DeathChest is past expiration (not infinite, denoted by Instant.EPOCH), expire chest
			else if (validDeathChest.expirationTime().isAfter(Instant.EPOCH) && validDeathChest.expirationTime().isBefore(Instant.now()))
			{
				plugin.chestManager.expire(validDeathChest);
			}
			else
			{
				// set chest metadata
				this.setBlockMetadata(this.getBlocks(validDeathChest.chestUid()));
				if (plugin.getConfig().getBoolean("debug")) {
					plugin.getLogger().info("[loadDeathChests] Setting metadata for chest " + validDeathChest.chestUid());
				}
			}
		}
	}


	/**
	 * Put DeathChest object in map
	 *
	 * @param validDeathChest the DeathChest object to put in map
	 */
	public void putChest(final ValidDeathChest validDeathChest)
	{
		this.chestIndex.put(plugin, validDeathChest);
	}


	/**
	 * Get DeathChest object by chestUUID
	 *
	 * @param chestUUID UUID of DeathChest object to retrieve
	 * @return DeathChest object, or null if no DeathChest exists in map with passed chestUUID
	 */
	public ValidDeathChest getChest(final UUID chestUUID)
	{
		return this.chestIndex.get(chestUUID);
	}


	/**
	 * Get DeathChest object by block
	 *
	 * @param block the block to retrieve DeathChest object
	 * @return DeathChest object, or null if no DeathChest exists in map that contains passed block location
	 */
	public ValidDeathChest getChest(final Block block)
	{
		// if passed block is null, return null
		if (block == null)
		{
			return null;
		}

		// get chest block from index by location
		ValidChestBlock validChestBlock = this.blockIndex.get(block.getLocation());

		// return death chest referenced by uid in chest block
		return (validChestBlock != null)
				? getChest(validChestBlock.getChestUid())
				: null;
	}


	public ValidDeathChest getChest(final Inventory inventory)
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
	void removeChest(final ValidDeathChest deathChest)
	{
		this.chestIndex.remove(deathChest);
	}


	/**
	 * Put validChestBlock object in block index
	 *
	 * @param validChestBlock the validChestBlock to put in map
	 */
	public void putBlock(final ChestBlockType chestBlockType, final ValidChestBlock validChestBlock)
	{
		this.blockIndex.put(chestBlockType, validChestBlock);
	}


	/**
	 * Get validChestBlock object from block index by location
	 *
	 * @param location the location to retrieve validChestBlock object
	 * @return validChestBlock object, or null if no validChestBlock exists in map with passed location
	 */
	@SuppressWarnings("unused")
	public ValidChestBlock getBlock(final Location location)
	{
		return this.blockIndex.get(location);
	}


	/**
	 * Get chestBlock set from block index by chest uuid
	 *
	 * @param chestUid the UUID of the chest of which to retrieve a set of chest blocks
	 * @return Set of Blocks in uuidBlockMap, or empty set if no blocks exist for chest UUID
	 */
	public Collection<ValidChestBlock> getBlocks(final UUID chestUid)
	{
		return this.blockIndex.getBlocks(chestUid);
	}


	/**
	 * Get chestBlock map from block index by chest uuid
	 *
	 * @param chestUid the UUID of the chest of which to retrieve a map of chest blocks
	 * @return Map of Blocks in uuidBlockMap, or empty map if no blocks exist for chest UUID
	 */
	Map<ChestBlockType, ValidChestBlock> getBlockMap(final UUID chestUid)
	{
		return this.blockIndex.getBlockMap(chestUid);
	}


	/**
	 * Remove validChestBlock object from map
	 *
	 * @param validChestBlock the validChestBlock object to remove from map
	 */
	void removeBlock(final ValidChestBlock validChestBlock)
	{
		this.blockIndex.remove(validChestBlock);
	}


	/**
	 * Test if validChestBlock exists in map with passed block location
	 *
	 * @param block the block to check for existence in block index
	 * @return {@code true} if a validChestBlock exists in map with passed block location,
	 * {@code false} if no validChestBlock exists in map with passed block location
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
	public Collection<ValidDeathChest> getAllChests()
	{
		return this.chestIndex.values();
	}


	public void insertChestRecords(final Collection<ValidDeathChest> deathChests)
	{
		// get validChestBlocks for all deathChests
		Set<ValidChestBlock> validChestBlocks = new HashSet<>();
		for (ValidDeathChest validDeathChest : deathChests)
		{
			validChestBlocks.addAll(getBlocks(validDeathChest.chestUid()));
		}

		dataStore.deathChests().save(deathChests);
		dataStore.chestBlocks().save(validChestBlocks);
	}


	public void deleteBlockRecord(final ValidChestBlock validChestBlock)
	{
		dataStore.chestBlocks().delete(validChestBlock);
	}


	public void deleteChestRecord(final ValidDeathChest validDeathChest)
	{
		dataStore.deathChests().delete(validDeathChest);
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
	void setBlockMetadata(Collection<ValidChestBlock> validChestBlocks)
	{
		// set metadata on blocks in set
		for (ValidChestBlock validChestBlock : validChestBlocks)
		{
			setMetadata(validChestBlock, getChest(validChestBlock.getChestUid()));
		}
	}


	/**
	 * Check if protection is enabled and has expired
	 * @return boolean - true if protection has expired, false if not
	 */
	public boolean protectionExpired(ValidDeathChest validDeathChest)
	{
		return plugin.getConfig().getBoolean("chest-protection")
				&& plugin.getConfig().getInt("chest-protection-time") > 0
				&& validDeathChest.protectionTime().isBefore(Instant.now());
	}


	/**
	 * Getter method for DeathChest expireTaskId
	 *
	 * @return the value of the expireTaskId field in the DeathChest object
	 */
	int getExpireTaskId(ValidDeathChest validDeathChest)
	{
		return chestIndex.getExpireTaskId(validDeathChest);
	}


	/**
	 * Cancel expire task for this death chest
	 */
	public void cancelExpireTask(ValidDeathChest validDeathChest)
	{
		// if task id is positive integer, cancel task
		if (getExpireTaskId(validDeathChest) > 0)
		{
			plugin.getServer().getScheduler().cancelTask(getExpireTaskId(validDeathChest));
		}
	}


	public Inventory getInventory(final ValidDeathChest deathChest)
	{
		Map<ChestBlockType, ValidChestBlock> blockMap = getBlockMap(deathChest.chestUid());
		return getInventory(blockMap);
	}


	/**
	 * Get inventory associated with this death chest
	 *
	 * @return Inventory - the inventory associated with this death chest;
	 * returns null if both right and left chest block inventories are invalid
	 */
	public Inventory getInventory(Map<ChestBlockType, ValidChestBlock> blockMap)
	{
		// get right chest inventory
		Inventory inventory = plugin.chestManager.getInventory(blockMap.get(ChestBlockType.RIGHT_CHEST));

		// if right chest inventory is null, try left chest
		if (inventory == null)
		{
			inventory = plugin.chestManager.getInventory(blockMap.get(ChestBlockType.LEFT_CHEST));
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
	public Collection<ItemStack> fill(final Collection<ItemStack> itemStacks, final ValidDeathChest validDeathChest)
	{
		// create empty list for return
		Collection<ItemStack> remainingItems = new LinkedList<>();

		// get inventory for this death chest
		Inventory inventory = getInventory(validDeathChest);

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
	public void expire(final ValidDeathChest validDeathChest)
	{
		// get player from ownerUUID
		final Player player = plugin.getServer().getPlayer(validDeathChest.ownerUid());

		// destroy DeathChest
		this.destroy(validDeathChest);

		// if player is not null, send player message
		if (player != null)
		{
			plugin.messageBuilder.compose(player, MessageId.CHEST_EXPIRED)
					.setMacro(Macro.DEATH_CHEST, validDeathChest)
					.send();
		}
	}


	/**
	 * Destroy this death chest, dropping chest contents
	 */
	public void destroy(final ValidDeathChest validDeathChest)
	{
		this.dropContents(validDeathChest);

		// play chest break sound at chest location
		plugin.soundConfig.playSound(validDeathChest.getLocation(), SoundId.CHEST_BREAK);

		// get block map for this chest
		Map<ChestBlockType, ValidChestBlock> chestBlockMap = plugin.chestManager.getBlockMap(validDeathChest.chestUid());

		// destroy DeathChest blocks (sign gets destroyed first due to enum order, preventing detach before being destroyed)
		for (ValidChestBlock validChestBlock : chestBlockMap.values())
		{
			plugin.chestManager.destroy(validChestBlock);
		}

		// delete DeathChest record from datastore
		this.deleteChestRecord(validDeathChest);

		// cancel expire block task
		if (this.getExpireTaskId(validDeathChest) > 0)
		{
			plugin.getServer().getScheduler().cancelTask(this.getExpireTaskId(validDeathChest));
		}

		// remove DeathChest from ChestManager DeathChest map
		plugin.chestManager.removeChest(validDeathChest);
	}


	public void dropContents(final ValidDeathChest validDeathChest)
	{
		Location location = validDeathChest.getLocation();

		if (location.getWorld() != null)
		{
			ItemStack[] contents = this.getInventory(validDeathChest).getStorageContents();

			this.getInventory(validDeathChest).clear();

			for (ItemStack stack : contents)
			{
				if (stack != null)
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
	public int getViewerCount(final ValidDeathChest validDeathChest)
	{
		// get chest inventory
		Inventory inventory = this.getInventory(validDeathChest);

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
	public void autoLoot(final Player player, final ValidDeathChest validDeathChest)
	{
		// if passed player or deathchest is null, do nothing and return
		if (player == null || validDeathChest == null)
		{
			return;
		}

		// create collection to hold items that did not fit in player inventory
		Collection<ItemStack> remainingItems = new ArrayList<>();

		// transfer contents of any chest blocks to player, putting any items that did not fit in remainingItems
		for (ValidChestBlock validChestBlock : this.getBlocks(validDeathChest.chestUid()))
		{
			remainingItems.addAll(plugin.chestManager.transferContents(validChestBlock, player));
		}

		// if remainingItems is empty, all chest items fit in player inventory so destroy chest and return
		if (remainingItems.isEmpty())
		{
			plugin.chestManager.destroy(validDeathChest);
			return;
		}

		// send player message
		plugin.messageBuilder.compose(player, MessageId.INVENTORY_FULL)
				.setMacro(Macro.LOCATION, player.getLocation())
				.send();

		// try to put remaining items back in chest
		remainingItems = plugin.chestManager.fill(remainingItems, validDeathChest);

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


	/**
	 * Get DeathChest chest block that DeathChest sign is attached
	 *
	 * @return Block - DeathChest chest block;
	 * returns null if sign is not a DeathChest sign or attached block is not a DeathChest chest block
	 */
	public Block getAttachedBlock(final Block block)
	{
		// if block is not a DeathSign, return null
		if (!plugin.chestManager.isChestBlockSign(block))
		{
			return null;
		}

		Block returnBlock = null;

		// if block is a wall sign, get block behind
		if (block.getBlockData() instanceof WallSign wallSign)
		{
			returnBlock = block.getRelative(wallSign.getFacing().getOppositeFace());
		}

		// else if block is a sign post, get block below
		else if (block.getBlockData() instanceof Sign)
		{
			returnBlock = block.getRelative(0, 1, 0);
		}

		// if attached block is not a DeathChest, return null
		if (!plugin.chestManager.isChestBlockChest(returnBlock))
		{
			return null;
		}

		return returnBlock;
	}


	/**
	 * Get the inventory of this validChestBlock
	 *
	 * @return Inventory - the inventory of this validChestBlock;
	 * if validChestBlock is a sign, return inventory of attached validChestBlock;
	 * returns null if this validChestBlock (or attached block) is not a chest
	 */
	public Inventory getInventory(final Block block)
	{
		// get the block state of block represented by this validChestBlock
		BlockState blockState = block.getState();

		// if block is a sign or wall sign, get attached block
		if (blockState.getType().equals(Material.OAK_SIGN) || blockState.getType().equals((Material.OAK_WALL_SIGN)))
		{
			// get attached block
			Block attachedBlock = this.getAttachedBlock(block);

			// if attached block returned null, do nothing and return
			if (attachedBlock != null)
			{
				blockState = attachedBlock.getState();
			}
			else
			{
				return null;
			}
		}

		// if blockState is a chest object, open inventory for player
		if (blockState instanceof Chest)
		{
			return ((Chest) blockState).getInventory();
		}

		return null;
	}


	/**
	 * Get the inventory of this validChestBlock
	 *
	 * @return Inventory - the inventory of this validChestBlock;
	 * if validChestBlock is a sign, return inventory of attached validChestBlock;
	 * returns null if this validChestBlock (or attached block) is not a chest
	 */
	Inventory getInventory(final ValidChestBlock validChestBlock)
	{
		// if this validChestBlock location is null, return null
		if (validChestBlock.getLocation() == null)
		{
			return null;
		}

		// get the block state of block represented by this validChestBlock
		BlockState blockState = validChestBlock.getLocation().getBlock().getState();

		// if block is a sign or wall sign, get attached block
		if (blockState.getType().equals(Material.OAK_SIGN) || blockState.getType().equals((Material.OAK_WALL_SIGN)))
		{
			// get attached block
			Block attachedBlock = plugin.chestManager.getAttachedBlock(validChestBlock.getLocation().getBlock());

			// if attached block returned null, do nothing and return
			if (attachedBlock != null)
			{
				blockState = attachedBlock.getState();
			}
			else
			{
				return null;
			}
		}


		// if blockState is a chest object, open inventory for player
		if (blockState instanceof Chest)
		{
			return ((Chest) blockState).getInventory();
		}

		return null;
	}


	/**
	 * Transfer the contents of this chest block to player inventory
	 *
	 * @param player the player whose inventory chest items will be placed
	 */
	Collection<ItemStack> transferContents(final ValidChestBlock validChestBlock, final Player player)
	{
		// create empty list to contain items that did not fit in chest
		Collection<ItemStack> remainingItems = new LinkedList<>();

		// if player is null, return empty list
		if (player == null)
		{
			return remainingItems;
		}

		// if DeathBlock location is null, return empty list
		if (validChestBlock.getLocation() == null)
		{
			return remainingItems;
		}

		// get in game block at deathBlock location
		Block block = validChestBlock.getLocation().getBlock();

		// confirm block is still death chest block
		if (plugin.chestManager.isChestBlockChest(block))
		{
			// get player inventory object
			final PlayerInventory playerInventory = player.getInventory();

			// get chest object
			final Chest chest = (Chest) block.getState();

			// get Collection of ItemStack for chest inventory
			final Collection<ItemStack> chestInventory = new LinkedList<>(Arrays.asList(chest.getInventory().getContents()));

			// iterate through all inventory slots in chest inventory
			for (ItemStack itemStack : chestInventory)
			{
				// if inventory slot item is not null...
				if (itemStack != null)
				{
					// remove item from chest inventory
					chest.getInventory().removeItem(itemStack);

					// add item to player inventory
					remainingItems.addAll(playerInventory.addItem(itemStack).values());

					// play inventory add sound
					plugin.soundConfig.playSound(player, SoundId.INVENTORY_ADD_ITEM);
				}
			}
		}
		return remainingItems;
	}


	/**
	 * Destroy chest block, dropping any contents on ground.
	 * Removes block metadata and deletes corresponding block record from block index and datastore.
	 */
	void destroy(final ValidChestBlock validChestBlock)
	{
		// if validChestBlock location is null, do nothing and return
		if (validChestBlock.getLocation() == null)
		{
			return;
		}

		// get in game block at this validChestBlock location
		Block block = validChestBlock.getLocation().getBlock();

		// load chunk if necessary
		if (!block.getChunk().isLoaded())
		{
			block.getChunk().load();
		}

		// remove metadata from block
		plugin.chestManager.removeMetadata(validChestBlock);

		// remove validChestBlock record from datastore
		plugin.chestManager.deleteBlockRecord(validChestBlock);

		// remove validChestBlock from block map
		plugin.chestManager.removeBlock(validChestBlock);

		// set block material to air; this will drop chest contents, but not the block itself
		// Note: this must be performed last, because above methods do checks for valid in-game chest material block
		block.setType(Material.AIR);
	}


	/**
	 * Remove metadata from this chest block
	 */
	void removeMetadata(final ValidChestBlock validChestBlock)
	{
		// if validChestBlock location is null, do nothing and return
		if (validChestBlock.getLocation() == null)
		{
			return;
		}

		// get in game block at this validChestBlock location
		Block block = validChestBlock.getLocation().getBlock();

		block.removeMetadata("deathchest-uuid", plugin);
		block.removeMetadata("deathchest-owner", plugin);
		block.removeMetadata("deathchest-killer", plugin);
	}


	/**
	 * Set block metadata
	 *
	 * @param deathChest the DeathChest whose metadata will be set on this chest block
	 */
	public void setMetadata(final ValidChestBlock validChestBlock, final ValidDeathChest deathChest)
	{
		// check for null object
		if (deathChest == null || deathChest.chestUid() == null)
		{
			return;
		}

		// if DeathBlock location is null, do nothing and return
		if (validChestBlock.getLocation() == null)
		{
			return;
		}

		// get in game block at chest block location
		Block block = validChestBlock.getLocation().getBlock();

		// if block is not death chest material, do nothing and return
		if (!ChestManager.deathChestMaterials.contains(block.getType()))
		{
			return;
		}

		// set chest uuid metadata
		block.setMetadata("deathchest-uuid", new FixedMetadataValue(plugin, deathChest.chestUid()));

		// set owner uuid metadata
		if (deathChest.ownerUid() != null && !deathChest.ownerUid().equals(INVALID_UUID))
		{
			block.setMetadata("deathchest-owner", new FixedMetadataValue(plugin, deathChest.ownerUid()));
		}

		// set killer uuid metadata
		if (deathChest.killerUid() != null && !deathChest.killerUid().equals(INVALID_UUID))
		{
			block.setMetadata("deathchest-killer", new FixedMetadataValue(plugin, deathChest.killerUid()));
		}
	}

}
