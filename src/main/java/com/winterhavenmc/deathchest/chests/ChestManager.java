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
import com.winterhavenmc.deathchest.models.chestblock.*;
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.DeathChestReason;
import com.winterhavenmc.deathchest.models.deathchest.InvalidDeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.sounds.SoundId;
import com.winterhavenmc.deathchest.storage.DataStore;

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
import java.util.stream.Collectors;

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
	private final DataStore dataStore;

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
		dataStore = DataStore.connect(plugin);

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
			// if parent chest is not in chest map, delete block record
			if (!chestIndex.containsKey(validChestBlock.getChestUid()))
			{
				dataStore.chestBlocks().delete(validChestBlock);
			}
			else
			{
				// get chest block type constant from object type
				ChestBlockType chestBlockType = switch (validChestBlock)
				{
					case LeftChestBlock ignored -> ChestBlockType.LEFT_CHEST;
					case RightChestBlock ignored -> ChestBlockType.RIGHT_CHEST;
					case SignChestBlock ignored -> ChestBlockType.SIGN;
				};

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
			// if DeathChest is after epoch and before current time, expire chest
			else if (isExpired(validDeathChest))
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


	private static boolean isExpired(ValidDeathChest validDeathChest)
	{
		return validDeathChest.expirationTime().isAfter(Instant.EPOCH)
				&& validDeathChest.expirationTime().isBefore(Instant.now());
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
	 * Get valid DeathChest by chestUUID
	 *
	 * @param chestUid UUID of DeathChest object to retrieve
	 * @return ValidDeathChest, or InvalidDeathChest if no DeathChest exists in map with passed chestUUID
	 */
	public DeathChest getChest(final UUID chestUid)
	{
		return this.chestIndex.get(chestUid);
	}


	/**
	 * Get valid DeathChest by block
	 *
	 * @param block the block to retrieve DeathChest object
	 * @return ValidDeathChest object, or InvalidDeathChest if no DeathChest exists in map that contains
	 * passed block location
	 */
	public DeathChest getChest(final Block block)
	{
		if (block != null)
		{
			// get chest block from index by location
			ChestBlock chestBlock = this.blockIndex.get(block.getLocation());

			// return death chest referenced by uid in chest block
			return (chestBlock instanceof ValidChestBlock validChestBlock)
					? getChest(validChestBlock.getChestUid())
					: new InvalidDeathChest(DeathChestReason.BLOCK_INVALID);
		}
		else
		{
			return new InvalidDeathChest(DeathChestReason.BLOCK_NULL);
		}
	}


	/**
	 * Get valid DeathChest from Inventory
	 *
	 * @param inventory the inventory from which to retrieve a valid DeathChest
	 * @return ValidDeathChest associated with the inventory, or InvalidDeathChest if the inventory is not associated
	 * with a death chest.
	 */
	public DeathChest getChest(final Inventory inventory)
	{
		Block block = null;

		if (plugin.chestManager.isDeathChestInventory(inventory) && inventory.getHolder() instanceof Chest chest)
		{
			block = chest.getBlock();
		}

		// return death chest for block (returns InvalidDeathChest if block is not valid chest block)
		return getChest(block);
	}


	/**
	 * Remove DeathChest object from chest index
	 *
	 * @param validDeathChest the DeathChest object to remove from map
	 */
	void removeChest(final ValidDeathChest validDeathChest)
	{
		this.chestIndex.remove(validDeathChest);
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
		return block != null
				&& deathChestMaterials.contains(block.getType())
				&& this.blockIndex.containsKey(block.getLocation());
	}


	/**
	 * Test if a block is a DeathChest chest block
	 *
	 * @param block The block to test
	 * @return {@code true} if block is Chest and block location exists in block index, or {@code false} if not
	 */
	public boolean isChestBlockChest(final Block block)
	{
		return block != null
				&& block.getState() instanceof Chest
				&& blockIndex.containsKey(block.getLocation());
	}


	/**
	 * Test if a block is a deathchest sign
	 *
	 * @param block The block to test if it is a DeathSign
	 * @return {@code true} if block is Sign or WallSign and block location exists in block index,
	 * or {@code false} if not
	 */
	public boolean isChestBlockSign(final Block block)
	{
		return block != null
				&& (block.getState() instanceof Sign || block.getState() instanceof WallSign)
				&& blockIndex.containsKey(block.getLocation());
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
		Set<ValidChestBlock> chestBlocks = deathChests.stream()
				.map(ValidDeathChest::chestUid)
				.map(this::getBlocks)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		dataStore.deathChests().save(deathChests);
		dataStore.chestBlocks().save(chestBlocks);
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
		for (ValidChestBlock validChestBlock : validChestBlocks)
		{
			DeathChest deathChest = getChest(validChestBlock.getChestUid());
			if (deathChest instanceof ValidDeathChest validDeathChest)
			{
				setMetadata(validChestBlock, validDeathChest);
			}
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


	public Optional<Inventory> getInventory(final ValidDeathChest deathChest)
	{
		return getInventory(getBlockMap(deathChest.chestUid()));
	}


	/**
	 * Get inventory associated with this death chest
	 *
	 * @return Inventory - the inventory associated with this death chest;
	 * returns null if both right and left chest block inventories are invalid
	 */
	public Optional<Inventory> getInventory(Map<ChestBlockType, ValidChestBlock> blockMap)
	{
		// get right chest inventory
		Optional<Inventory> rightChestInventory = plugin.chestManager.getInventory(blockMap.get(ChestBlockType.RIGHT_CHEST));

		return rightChestInventory.isEmpty()
				? plugin.chestManager.getInventory(blockMap.get(ChestBlockType.LEFT_CHEST))
				: rightChestInventory;
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
		Optional<Inventory> inventory = getInventory(validDeathChest);

		// if inventory is present, add itemStacks to inventory and put leftovers in remainingItems
		if (inventory.isPresent())
		{
			remainingItems = new LinkedList<>(inventory.get().addItem(itemStacks.toArray(new ItemStack[0])).values());
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
			Optional<Inventory> inventory = getInventory(validDeathChest);
			if (inventory.isPresent())
			{
				ItemStack[] contents = inventory.get().getStorageContents();

				inventory.get().clear();

				for (ItemStack stack : contents)
				{
					if (stack != null)
					{
						location.getWorld().dropItemNaturally(location, stack);
					}
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
		return getInventory(validDeathChest).map(itemStacks -> itemStacks.getViewers().size()).orElse(0);
	}


	/**
	 * Transfer all chest contents to player inventory and remove in-game chest if empty.
	 * Items that do not fit in player inventory will be retained in chest.
	 *
	 * @param player the player whose inventory the chest contents will be transferred
	 */
	public void autoLoot(final Player player, final ValidDeathChest validDeathChest)
	{
		// if player is not null auto-loot chest into player inventory
		if (player != null)
		{
			// create collection to hold items that did not fit in player inventory
			Collection<ItemStack> remainingItems = new ArrayList<>();

			// transfer contents of any chest blocks to player, putting any items that did not fit in remainingItems
			for (ValidChestBlock validChestBlock : getBlocks(validDeathChest.chestUid()))
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
	}


	/**
	 * Get DeathChest chest block that DeathChest sign is attached
	 *
	 * @return Block - DeathChest chest block;
	 * returns null if sign is not a DeathChest sign or attached block is not a DeathChest chest block
	 */
	public Optional<Block> getAttachedBlock(final Block block)
	{
		if (!plugin.chestManager.isChestBlockSign(block))
		{
			return Optional.empty();
		}

		Block attached = switch (block.getBlockData())
		{
			case WallSign wallSign -> block.getRelative(wallSign.getFacing().getOppositeFace());
			case Sign ignored -> block.getRelative(0, 1, 0);
			default -> null;
		};

		return plugin.chestManager.isChestBlockChest(attached)
				? Optional.of(attached)
				: Optional.empty();
	}


	/**
	 * Get the inventory of a valid ChestBlock
	 *
	 * @return Inventory - the inventory of this validChestBlock;
	 * if validChestBlock is a sign, return inventory of attached validChestBlock;
	 * returns null if this validChestBlock (or attached block) is not a chest
	 */
	Optional<Inventory> getInventory(final ValidChestBlock validChestBlock)
	{
		// get the block state of block represented by this validChestBlock
		BlockState blockState = validChestBlock.getLocation().getBlock().getState();

		// if block is a sign or wall sign, get attached block
		if (blockState instanceof Sign || blockState instanceof WallSign)
		{
			// get attached block
			Optional<Block> attachedBlock = plugin.chestManager.getAttachedBlock(validChestBlock.getLocation().getBlock());

			// if attached block is present get block state, otherwise return null
			if (attachedBlock.isPresent())
			{
				blockState = attachedBlock.get().getState();
			}
		}

		// if blockState is a Chest object return optional inventory, else return empty optional
		return (blockState instanceof Chest chest)
				? Optional.of(chest.getInventory())
				: Optional.empty();
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
		if (validChestBlock.getLocation() != null)
		{
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
	}


	/**
	 * Remove metadata from this chest block
	 */
	void removeMetadata(final ValidChestBlock validChestBlock)
	{
		// if validChestBlock location is null, do nothing and return
		if (validChestBlock.getLocation() != null)
		{
			// get in game block at this validChestBlock location
			Block block = validChestBlock.getLocation().getBlock();

			block.removeMetadata("deathchest-uuid", plugin);
			block.removeMetadata("deathchest-owner", plugin);
			block.removeMetadata("deathchest-killer", plugin);
		}
	}


	/**
	 * Set block metadata
	 *
	 * @param deathChest the DeathChest whose metadata will be set on this chest block
	 */
	public void setMetadata(final ValidChestBlock validChestBlock, final ValidDeathChest deathChest)
	{
		// get in game block at chest block location
		Block block = validChestBlock.getLocation().getBlock();

		// if block is not death chest material, do nothing and return
		if (ChestManager.deathChestMaterials.contains(block.getType()))
		{
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

}
