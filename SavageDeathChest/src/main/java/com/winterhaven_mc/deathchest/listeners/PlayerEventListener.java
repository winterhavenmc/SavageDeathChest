package com.winterhaven_mc.deathchest.listeners;

import com.winterhaven_mc.deathchest.PluginMain;
import com.winterhaven_mc.deathchest.messages.Message;
import com.winterhaven_mc.deathchest.chests.search.ProtectionPlugin;
import com.winterhaven_mc.deathchest.chests.ChestBlock;
import com.winterhaven_mc.deathchest.chests.DeathChest;
import com.winterhaven_mc.deathchest.chests.Deployment;
import com.winterhaven_mc.deathchest.sounds.SoundId;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import static com.winterhaven_mc.deathchest.messages.Macro.*;
import static com.winterhaven_mc.deathchest.messages.Macro.VIEWER;
import static com.winterhaven_mc.deathchest.messages.MessageId.*;


/**
 * A class that contains {@code EventHandler} methods to process player related events
 */

public final class PlayerEventListener implements Listener {

	// reference to main class
	private final PluginMain plugin;


	/**
	 * class constructor
	 *
	 * @param plugin reference to main class
	 */
	public PlayerEventListener(final PluginMain plugin) {

		// set reference to main class
		this.plugin = plugin;

		// register event handlers in this class
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
	public final void onPlayerDeath(final PlayerDeathEvent event) {

		// deploy DeathChest
		new Deployment(plugin, event.getEntity(), event.getDrops());
	}


	/**
	 * Prevent deathchest opening by non-owners or creative players.<br>
	 * Listens at EventPriority.LOW to handle event before protection plugins
	 *
	 * @param event PlayerInteractEvent
	 */
	@EventHandler(priority = EventPriority.LOW)
	public final void onPlayerInteract(final PlayerInteractEvent event) {

		// get player
		final Player player = event.getPlayer();

		// get block
		final Block block = event.getClickedBlock();

		// if block is not DeathChest block, do nothing and return
		if (!plugin.chestManager.isChestBlock(block)) {
			return;
		}

		// get ChestBlock at clicked block location
		ChestBlock chestBlock = plugin.chestManager.getChestBlock(block.getLocation());

		// if chest block returned null, do nothing and return
		if (chestBlock == null) {
			return;
		}

		// get DeathChest from ChestBlock
		DeathChest deathChest = plugin.chestManager.getDeathChest(chestBlock.getChestUUID());

		// if DeathChest returned null, do nothing and return
		if (deathChest == null) {
			return;
		}

		// if access is blocked by a protection plugin, do nothing and return (allow protection plugin to handle event)
		final ProtectionPlugin blockingPlugin = ProtectionPlugin.allowChestAccess(player, block);
		if (blockingPlugin != null) {
			if (plugin.debug) {
				plugin.getLogger().info("Death chest playerInteractEvent was blocked by "
						+ blockingPlugin.getPluginName());
			}
			return;
		}

		// if player is in creative mode,
		// and creative-access is configured false,
		// and player does not have override permission,
		// then cancel event, send message and return
		if (player.getGameMode().equals(GameMode.CREATIVE)
				&& !plugin.getConfig().getBoolean("creative-access")
				&& !player.hasPermission("deathchest.creative-access")) {
			event.setCancelled(true);
			Message.create(player, NO_CREATIVE_ACCESS)
					.setMacro(LOCATION, player.getLocation()
					).send();
			return;
		}

		// if chest inventory is already being viewed: cancel event, send message and return
		if (deathChest.getViewerCount() > 0) {

			// cancel event
			event.setCancelled(true);

			// only one chest viewer allowed, so get name viewer at index 0
			String viewerName = deathChest.getInventory().getViewers().get(0).getName();

			// get owner name from uuid
			String ownerName = "-";
			if (deathChest.getOwnerUUID() != null) {
				ownerName = plugin.getServer().getOfflinePlayer(deathChest.getOwnerUUID()).getName();
			}

			// get killer name from uuid
			String killerName = "-";
			if (deathChest.getKillerUUID() != null) {
				killerName = plugin.getServer().getOfflinePlayer(deathChest.getKillerUUID()).getName();
			}

			// send player message
			Message.create(player, CHEST_CURRENTLY_OPEN)
					.setMacro(LOCATION, deathChest.getLocation())
					.setMacro(OWNER, ownerName)
					.setMacro(KILLER, killerName)
					.setMacro(VIEWER, viewerName)
					.send();

			// play denied access sound
			plugin.soundConfig.playSound(player, SoundId.CHEST_DENIED_ACCESS);
			return;
		}

		// if player sneak punched block and quick-loot is enabled, try auto-loot
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)
				&& player.isSneaking()
				&& plugin.getConfig().getBoolean("quick-loot")
				&& player.hasPermission("deathchest.loot")) {

			// cancel event
			event.setCancelled(true);

			// if chest protection is not enabled, loot chest and return
			if (!plugin.getConfig().getBoolean("chest-protection")) {
				deathChest.autoLoot(player);
				return;
			}

			// if player is owner or has deathchest.loot.other permission, loot chest and return
			if (deathChest.isOwner(player) || player.hasPermission("deathchest.loot.other")) {
				deathChest.autoLoot(player);
				return;
			}

			// if killer looting is enabled and player is killer and has permission, loot chest and return
			if (plugin.getConfig().getBoolean("killer-looting")
					&& deathChest.isKiller(player)
					&& player.hasPermission("deathchest.loot.killer")) {
				deathChest.autoLoot(player);
				return;
			}
			else {
				// send player not-owner message
				Message.create(player, NOT_OWNER).send();

				// play denied access sound
				plugin.soundConfig.playSound(player, SoundId.CHEST_DENIED_ACCESS);
			}
		}

		// if player did not right click block, do nothing and return
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}

		// if chest-protection option is not enabled, do nothing and return
		if (!plugin.getConfig().getBoolean("chest-protection")) {
			return;
		}

		// cancel event
		event.setCancelled(true);

		// if player is owner or has deathchest.loot.other permission, open chest inventory and return
		if (deathChest.isOwner(player) || player.hasPermission("deathchest.loot.other")) {
			chestBlock.openInventory(player);
			return;
		}

		// if killer looting is enabled  and player is killer and has permission, open chest inventory and return
		if (plugin.getConfig().getBoolean("killer-looting")
				&& deathChest.isKiller(player)
				&& player.hasPermission("deathchest.loot.killer")) {
			chestBlock.openInventory(player);
		}
		else {
			// send player not-owner message
			Message.create(player, NOT_OWNER).send();

			// play denied access sound
			plugin.soundConfig.playSound(player, SoundId.CHEST_DENIED_ACCESS);
		}
	}

}
