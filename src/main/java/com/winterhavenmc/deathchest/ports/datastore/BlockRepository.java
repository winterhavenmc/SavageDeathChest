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

package com.winterhavenmc.deathchest.ports.datastore;

import com.winterhavenmc.deathchest.models.chestblock.ValidChestBlock;

import java.util.Collection;


/**
 * An interface that declares methods for managing persistent storage of death chests and chest blocks.
 */
public interface BlockRepository
{
	/**
	 * Retrieve a collection of all block records from the datastore
	 *
	 * @return List of ValidChestBlock
	 */
	Collection<ValidChestBlock> getAll();


	/**
	 * Insert block records in the datastore
	 *
	 * @param blockRecords a collection of LegacyChestBlock objects to insert in the datastore
	 */
	int save(final Collection<ValidChestBlock> blockRecords);


	/**
	 * Delete a block record from the datastore
	 *
	 * @param validChestBlock the chest block to delete
	 */
	int delete(final ValidChestBlock validChestBlock);
}
