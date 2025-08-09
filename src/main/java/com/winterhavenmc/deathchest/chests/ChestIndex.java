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
import com.winterhavenmc.deathchest.models.deathchest.DeathChest;
import com.winterhavenmc.deathchest.models.deathchest.DeathChestReason;
import com.winterhavenmc.deathchest.models.deathchest.InvalidDeathChest;
import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import com.winterhavenmc.deathchest.tasks.ExpireChestTask;
import com.winterhavenmc.library.time.TimeUnit;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


final class ChestIndex
{
	private final Map<UUID, ValidDeathChest> deathChestMap;
	private final Map<UUID, Integer> expireMap;


	/**
	 * Constructor
	 */
	ChestIndex()
	{
		deathChestMap = new HashMap<>();
		expireMap = new HashMap<>();
	}


	/**
	 * Get DeathChest object by chestUUID
	 *
	 * @param chestUid UUID of DeathChest object to retrieve
	 * @return DeathChest object, or null if no DeathChest exists in map with passed chestUUID
	 */
	DeathChest get(final UUID chestUid)
	{
		return (chestUid != null)
				? this.deathChestMap.get(chestUid)
				: new InvalidDeathChest(DeathChestReason.CHEST_UID_NULL);
	}


	int getExpireTaskId(final ValidDeathChest deathChest)
	{
		return expireMap.get(deathChest.chestUid());
	}


	/**
	 * Put ValidDeathChest in map
	 *
	 * @param validDeathChest the ValidDeathChest to put in map
	 */
	void put(final PluginMain plugin, final ValidDeathChest validDeathChest)
	{
		this.deathChestMap.put(validDeathChest.chestUid(), validDeathChest);
		this.expireMap.put(validDeathChest.chestUid(), createExpireTask(plugin, validDeathChest));
	}


	/**
	 * Remove ValidDeathChest from map
	 *
	 * @param validDeathChest the ValidDeathChest to remove from map
	 */
	void remove(final ValidDeathChest validDeathChest)
	{
		this.deathChestMap.remove(validDeathChest.chestUid());
	}


	/**
	 * Check if chestUUID key exists in map
	 *
	 * @param chestUid the chest UUID to check
	 * @return {@code true} if key exists in map, {@code false} if it does not
	 */
	boolean containsKey(final UUID chestUid)
	{
		return chestUid != null && deathChestMap.containsKey(chestUid);
	}


	/**
	 * Get collection of all chests in map
	 *
	 * @return Collection of DeathChests in map
	 */
	Collection<ValidDeathChest> values()
	{
		return deathChestMap.values();
	}


	public int createExpireTask(final PluginMain plugin, final ValidDeathChest deathChest)
	{
		// create task to expire death chest after ticksRemaining
		BukkitTask chestExpireTask = new ExpireChestTask(plugin.chestManager, deathChest)
				.runTaskLater(plugin, ticksUntilExpires(deathChest));

		// return taskId
		return chestExpireTask.getTaskId();
	}


	public long ticksUntilExpires(ValidDeathChest deathChest)
	{
		// if DeathChestBlock expirationTime is zero or less, it is set to never expire
		if (deathChest.expirationTime().isBefore(Instant.EPOCH))
		{
			return -1;
		}

		// compute ticks remaining until expire time (millisecond interval divided by 50 yields ticks)
//		long ticksRemaining = Duration.between(Instant.now(), deathChest.expirationTime()).toMillis() / 50;
		long ticksRemaining = TimeUnit.MILLISECONDS.toTicks(Duration.between(Instant.now(), deathChest.expirationTime()).toMillis());
		if (ticksRemaining < 1)
		{
			ticksRemaining = 1L;
		}

		return ticksRemaining;
	}

}
