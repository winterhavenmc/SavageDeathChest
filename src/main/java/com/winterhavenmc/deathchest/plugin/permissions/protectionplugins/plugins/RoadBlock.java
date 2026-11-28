package com.winterhavenmc.deathchest.plugin.permissions.protectionplugins.plugins;

import com.winterhavenmc.deathchest.plugin.permissions.protectionplugins.ProtectionPlugin;
import com.winterhavenmc.deathchest.plugin.permissions.protectionplugins.ProtectionPluginAbstract;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;


public final class RoadBlock extends ProtectionPluginAbstract implements ProtectionPlugin
{
	public RoadBlock(final JavaPlugin plugin, final String name, final String version)
	{
		this.plugin = plugin;
		this.name = name;
		this.version = version;
	}


	@Override
	public boolean allowChestPlacement(final Player player, final Location location)
	{
		return true;
	}


	@Override
	public boolean allowChestAccess(final Player player, final Location location)
	{
		return true;
	}
}
