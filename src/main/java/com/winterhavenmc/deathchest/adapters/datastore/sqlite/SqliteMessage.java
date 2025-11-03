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

import com.winterhavenmc.deathchest.core.util.Notice;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


public enum SqliteMessage implements Notice
{
	INITIALIZE_DATASTORE_NOTICE("Datastore initialized."),
	INITIALIZE_DATASTORE_ERROR("An error occurred while trying to initialize the datastore."),
	CREATE_CHEST_TABLE_ERROR("An error occurred while creating the 'chest' table in the Sqlite datastore."),
	CREATE_BLOCK_TABLE_ERROR("An error occurred while creating the 'block' table in the Sqlite datastore."),
	SELECT_ALL_CHESTS_ERROR("An error occurred while trying to select all chest records from the SQLite datastore."),
	INSERT_CHEST_ERROR("An error occurred while inserting a DeathChest into the SQLite datastore."),
	DELETE_CHEST_ERROR("An error occurred while attempting to delete a chest record from the SQLite datastore."),
	GET_CHEST_COUNT_ERROR("An error occurred while attempting to retrieve a count of chest records from the SQLite datastore."),
	DELETE_ORPHANED_CHESTS_NOTICE("{0} orphaned chests in world {1} removed."),
	DELETE_ORPHANED_CHESTS_ERROR("An error occurred while attempting to delete orphaned chests from the datastore."),
	SELECT_ALL_BLOCKS_ERROR("An error occurred while trying to select all block records from the SQLite datastore."),
	INSERT_BLOCK_ERROR("An error occurred while inserting a death chest block into the SQLite datastore."),
	DELETE_BLOCK_ERROR("An error occurred while attempting to delete a block record from the SQLite datastore."),
	CLOSE_DATASTORE_ERROR("An error occurred while closing the SQLite datastore connection."),
	SCHEMA_VERSION_ERROR("Could not read schema version for the SQLite datastore."),
	CONNECTION_CLOSED_NOTICE("SQLite datastore connection closed."),
	ALREADY_INITIALIZED_NOTICE("SQLite datastore already initialized."),
	ENABLE_FOREIGN_KEYS_ERROR("An error occurred while attempting to enable foreign keys in the SQLIte datastore."),
	SCHEMA_UPDATE_ERROR("An error occurred while updating chest records in the SQLite datastore during schema migration."),
	SCHEMA_UP_TO_DATE_NOTICE("Current schema is up to date."),
	SCHEMA_CHESTS_MIGRATED_NOTICE("{0} death chest records migrated to current schema in the SQLite datastore."),
	SCHEMA_BLOCKS_MIGRATED_NOTICE("{0} chest block records migrated to current schema in the SQLite datastore."),
	SCHEMA_WORLD_NOT_LOADED_ERROR("World name ''{0}'' does not match a loaded world on the server. Skipping record for migration."),
	SCHEMA_VERSION_NOTICE("Schema version detected: {0}"),
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


	public String getLocalizeMessage(final Locale locale, final Object... objects)
	{
		try
		{
			final ResourceBundle bundle = ResourceBundle.getBundle(getClass().getSimpleName(), locale);
			String pattern = bundle.getString(name());
			return MessageFormat.format(pattern, objects);
		}
		catch (MissingResourceException exception)
		{
			return this.defaultMessage;
		}
	}

}
