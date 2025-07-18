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
import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * A class to manage the configured list of material types that can be replaced by a death chest
 */
public final class ReplaceableBlocks
{
	private final PluginMain plugin;
	private final Set<Material> materialSet;


	/**
	 * Class Constructor<br>
	 * populates set of replaceable blocks from config file
	 */
	ReplaceableBlocks(final PluginMain plugin)
	{
		this.plugin = plugin;
		this.materialSet = Collections.synchronizedSet(new LinkedHashSet<>());
		this.reload();
	}


	/**
	 * Load list of replaceable blocks from config file
	 */
	void reload()
	{
		// clear replaceable blocks
		materialSet.clear();

		// get string list of materials from config file
		Collection<String> materialStringList = plugin.getConfig().getStringList("replaceable-blocks");

		// iterate over string list
		for (String materialString : materialStringList)
		{
			// if material string matches a valid material type, add to replaceableBlocks set
			if (Material.matchMaterial(materialString) != null)
			{
				materialSet.add(Material.matchMaterial(materialString));
			}
		}
	}


	/**
	 * Check if replaceableBlocks set contains passed material
	 *
	 * @param material the material the test for
	 * @return true if replaceBlocks set contains material, false if it does not
	 */
	boolean contains(final Material material)
	{
		return material != null && this.materialSet.contains(material);
	}


	/**
	 * Get string representation of replaceableBlocks set
	 *
	 * @return Formatted string list of materials in replaceableBlocks set
	 */
	@Override
	public String toString()
	{
		return this.materialSet.toString();
	}

}
