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
import com.winterhavenmc.deathchest.tasks.ExpireChestTask;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


final class ChestIndex
{
	// map of DeathChests
	private final Map<UUID, DeathChestRecord> deathChestMap;
	private final Map<UUID, Integer> expireMap;


	/**
	 * Constructor
	 */
	ChestIndex()
	{
		deathChestMap = new ConcurrentHashMap<>();
		expireMap = new ConcurrentHashMap<>();
	}


	/**
	 * Get DeathChest object by chestUUID
	 *
	 * @param chestUid UUID of DeathChest object to retrieve
	 * @return DeathChest object, or null if no DeathChest exists in map with passed chestUUID
	 */
	DeathChestRecord get(final UUID chestUid)
	{
		// check for null key
		if (chestUid == null) {
			return null;
		}

		return this.deathChestMap.get(chestUid);
	}


	int getExpireTaskId(final DeathChestRecord deathChest)
	{
		return expireMap.get(deathChest.chestUid());
	}


	/**
	 * Put DeathChest object in map
	 *
	 * @param deathChest the DeathChest object to put in map
	 */
	void put(final PluginMain plugin, final DeathChestRecord deathChest)
	{
		// check for null key
		if (deathChest == null || deathChest.chestUid() == null)
		{
			return;
		}

		this.deathChestMap.put(deathChest.chestUid(), deathChest);
		this.expireMap.put(deathChest.chestUid(), createExpireTask(plugin, deathChest));
	}


	/**
	 * Remove DeathChest object from map
	 *
	 * @param deathChest the DeathChest object to remove from map
	 */
	void remove(final DeathChestRecord deathChest)
	{
		// check for null key
		if (deathChest == null || deathChest.chestUid() == null)
		{
			return;
		}

		this.deathChestMap.remove(deathChest.chestUid());
	}


	/**
	 * Check if chestUUID key exists in map
	 *
	 * @param chestUid the chest UUID to check
	 * @return {@code true} if key exists in map, {@code false} if it does not
	 */
	boolean containsKey(final UUID chestUid)
	{
		// check for null chestUUID
		if (chestUid == null)
		{
			return false;
		}

		return deathChestMap.containsKey(chestUid);
	}


	/**
	 * Get collection of all chests in map
	 *
	 * @return Collection of DeathChests in map
	 */
	Collection<DeathChestRecord> values()
	{
		return deathChestMap.values();
	}


	public int createExpireTask(final PluginMain plugin, final DeathChestRecord deathChest)
	{
		// create task to expire death chest after ticksRemaining
		BukkitTask chestExpireTask = new ExpireChestTask(plugin.chestManager, deathChest)
				.runTaskLater(plugin, ticksUntilExpires(deathChest));

		// return taskId
		return chestExpireTask.getTaskId();
	}


	public long ticksUntilExpires(DeathChestRecord deathChest)
	{
		// if DeathChestBlock expirationTime is zero or less, it is set to never expire
		if (deathChest.expirationTime().isBefore(Instant.EPOCH))
		{
			return -1;
		}

		// compute ticks remaining until expire time (millisecond interval divided by 50 yields ticks)
		long ticksRemaining = Duration.between(Instant.now(), deathChest.expirationTime()).toMillis() / 50;
		if (ticksRemaining < 1)
		{
			ticksRemaining = 1L;
		}

		return ticksRemaining;
	}


}
