package com.winterhavenmc.deathchest.commands;

import com.winterhavenmc.deathchest.PluginMain;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import com.winterhavenmc.deathchest.messages.MessageId;
import com.winterhavenmc.deathchest.sounds.SoundId;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 * Class that implements the help subcommand. Displays help and usage for plugin subcommands.
 */
final class HelpCommand extends SubcommandAbstract {

	private final PluginMain plugin;
	private final SubcommandRegistry subcommandRegistry;


	/**
	 * Class constructor
	 * @param plugin reference to the plugin main class
	 * @param subcommandRegistry reference to the subcommand registry
	 */
	HelpCommand(final PluginMain plugin, final SubcommandRegistry subcommandRegistry) {
		this.plugin = Objects.requireNonNull(plugin);
		this.subcommandRegistry = Objects.requireNonNull(subcommandRegistry);
		this.name = "help";
		this.usageString = "/deathchest help [command]";
		this.description = MessageId.COMMAND_HELP_HELP;
		this.maxArgs = 1;
	}


	@Override
	public List<String> onTabComplete(final @Nonnull CommandSender sender,
	                                  final @Nonnull Command command,
	                                  final @Nonnull String alias,
	                                  final String[] args) {

		List<String> returnList = new LinkedList<>();

		if (args.length == 2) {
			for (String subcommand : subcommandRegistry.getNames()) {
				if (sender.hasPermission("deathchest." + subcommand)
						&& subcommand.startsWith(args[1].toLowerCase())
						&& !subcommand.equalsIgnoreCase("help")) {
					returnList.add(subcommand);
				}
			}
		}

		return returnList;
	}


	@Override
	public boolean onCommand(final CommandSender sender, final List<String> args) {

		// if command sender does not have permission to display help, output error message and return true
		if (!sender.hasPermission("deathchest.help")) {
			plugin.messageBuilder.build(sender, MessageId.COMMAND_FAIL_HELP_PERMISSION).send();
			plugin.soundConfig.playSound(sender, SoundId.COMMAND_FAIL);
			return true;
		}

		// if no arguments, display usage for all commands
		if (args.size() == 0) {
			displayUsageAll(sender);
			return true;
		}

		// get subcommand name
		String subcommandName = args.get(0);
		displayHelp(sender, subcommandName);
		return true;
	}


	/**
	 * Display help message and usage for a command
	 *
	 * @param sender the command sender
	 * @param commandName the name of the command for which to show help and usage
	 */
	void displayHelp(final CommandSender sender, final String commandName) {

		// get subcommand from map by name
		Subcommand subcommand = subcommandRegistry.getCommand(commandName);

		// if subcommand found in map, display help message and usage
		if (subcommand != null) {
			plugin.messageBuilder.build(sender, subcommand.getDescription()).send();
			subcommand.displayUsage(sender);
		}

		// else display invalid command help message and usage for all commands
		else {
			plugin.messageBuilder.build(sender, MessageId.COMMAND_HELP_INVALID).send();
			plugin.soundConfig.playSound(sender, SoundId.COMMAND_INVALID);
			displayUsageAll(sender);
		}
	}


	/**
	 * Display usage message for all commands
	 *
	 * @param sender the command sender
	 */
	void displayUsageAll(final CommandSender sender) {

		plugin.messageBuilder.build(sender, MessageId.COMMAND_HELP_USAGE).send();

		for (String subcommandName : subcommandRegistry.getNames()) {
			if (subcommandRegistry.getCommand(subcommandName) != null) {
				subcommandRegistry.getCommand(subcommandName).displayUsage(sender);
			}
		}
	}

}
