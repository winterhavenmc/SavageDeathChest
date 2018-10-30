package com.winterhaven_mc.deathchest.tasks;

import org.bukkit.scheduler.BukkitTask;

import com.winterhaven_mc.deathchest.DeathChestBlock;
import com.winterhaven_mc.deathchest.PluginMain;

public final class TaskManager {
	
	// reference to main class
	private final PluginMain plugin;
	

	/**
	 * Class constructor
	 * @param plugin reference to main class
	 */
	public TaskManager(final PluginMain plugin) {
		
		// set reference to main class
		this.plugin = plugin;
	}

	
	// start death chest block expire task
	public final void createExpireBlockTask(final DeathChestBlock deathChestBlock) {

		// if DeathChestBlock expiration is zero or less, it is set to never expire; output debug message and return.
		if (deathChestBlock.getExpiration() < 1) {
			return;
		}
	
		// get current time
		Long currentTime = System.currentTimeMillis();
		
		// get death chest block expire time
		Long expireTime = deathChestBlock.getExpiration();
		
		// compute ticks remaining until expire time
		long ticksRemaining = (expireTime - currentTime) / 50;
		if (ticksRemaining < 1) {
			ticksRemaining = (long) 1;
		}
		
		// create task to expire death chest block after ticksRemaining
		BukkitTask blockExpireTask = new BlockExpireTask(deathChestBlock).runTaskLater(plugin, ticksRemaining);
		
		// set taskId in deathChestBlock
		deathChestBlock.setExpireTaskId(blockExpireTask.getTaskId());
	}
	
}