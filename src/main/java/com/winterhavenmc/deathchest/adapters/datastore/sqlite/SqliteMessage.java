/*
 * Copyright (c) 2025 Tim Savage.
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

package com.winterhavenmc.deathchest.adapters.datastore.sqlite;

import com.winterhavenmc.deathchest.util.Notice;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public enum SqliteMessage implements Notice
{
	INITIALIZE_DATASTORE_ERROR("An error occurred while trying to initialize the datastore."),
	CREATE_CHEST_TABLE_ERROR("An error occurred while creating the 'chest' table in the Sqlite datastore."),
	CREATE_BLOCK_TABLE_ERROR("An error occurred while creating the 'block' table in the Sqlite datastore."),
	SELECT_ALL_CHESTS_ERROR("An error occurred while trying to select all chest records from the SQLite datastore."),
	INSERT_CHEST_ERROR("An error occurred while inserting a DeathChest into the SQLite datastore."),
	DELETE_CHEST_ERROR("An error occurred while attempting to delete a chest record from the SQLite datastore."),
	GET_CHEST_COUNT_ERROR("An error occurred while attempting to retrieve a count of chest records from the SQLite datastore."),
	DELETE_ORPHANED_CHESTS_ERROR("An error occurred while attempting to delete orphaned chests from the datastore."),
	SELECT_ALL_BLOCKS_ERROR("An error occurred while trying to select all block records from the SQLite datastore."),
	INSERT_BLOCK_ERROR("An error occurred while inserting a death chest block into the SQLite datastore."),
	DELETE_BLOCK_ERROR("An error occurred while attempting to delete a block record from the SQLite datastore."),
	CLOSE_DATASTORE_ERROR("An error occurred while closing the SQLite datastore connection."),
	NO_SCHEMA_VERSION_ERROR("Could not read schema version for the SQLite datastore."),
	CONNECTION_CLOSED_NOTICE("SQLite datastore connection closed."),
	ALREADY_INITIALIZED_NOTICE("SQLite datastore already initialized."),
	ENABLE_FOREIGN_KEYS_ERROR("An error occurred while attempting to enable foreign keys in the SQLIte datastore."),
	SCHEMA_UPDATE_ERROR("An error occurred while updating chest records in the SQLite datastore during schema migration."),
	SCHEMA_UP_TO_DATE_NOTICE("Current schema is up to date."),
	;

	final String defaultMessage;


	SqliteMessage(final String defaultMessage)
	{
		this.defaultMessage = defaultMessage;
	}


	public String getLocalizeMessage(final Locale locale)
	{
		try
		{
			final ResourceBundle bundle = ResourceBundle.getBundle(getClass().getSimpleName(), locale);
			return bundle.getString(name());
		}
		catch (MissingResourceException exception)
		{
			return this.defaultMessage;
		}
	}

}
