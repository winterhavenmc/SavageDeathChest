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

import com.winterhavenmc.deathchest.models.deathchest.ValidDeathChest;
import java.util.Collection;


/**
 * An interface that declares methods for managing persistent storage of death chests
 */
public interface ChestRepository
{
	/**
	 * Retrieve a collection of all chest records from the datastore
	 *
	 * @return List of DeathChest
	 */
	Collection<ValidDeathChest> getAll();


	/**
	 * Insert a chest record in the datastore
	 *
	 * @param deathChests a collection of DeathChest objects to insert into the datastore
	 */
	@SuppressWarnings("UnusedReturnValue")
	int save(final Collection<ValidDeathChest> deathChests);


	/**
	 * Delete a chest record from the datastore
	 *
	 * @param deathChest the chest to delete
	 */
	@SuppressWarnings("UnusedReturnValue")
	int delete(final ValidDeathChest deathChest);


	/**
	 * Get number of chests in the datastore
	 *
	 * @return number of chests in the datastore
	 */
	int getCount();
}
