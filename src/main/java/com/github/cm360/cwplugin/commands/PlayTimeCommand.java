package com.github.cm360.cwplugin.commands;

import java.time.Duration;
import java.util.UUID;

import com.github.cm360.cwplugin.CraftWarsPlugin;
import com.github.cm360.cwplugin.integrations.playtime.PlayTimeIntegration;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class PlayTimeCommand extends Command {

	private final Plugin plugin;
	private PlayTimeIntegration integration;
	
	public PlayTimeCommand(Plugin plugin, PlayTimeIntegration integration) {
		super("playtime");
		this.plugin = plugin;
		this.integration = integration;
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			integration.updateAllPlayTimes();
			integration.sendData();
			if (args.length > 0) {
				// Get player ID from username
				UUID playerId = integration.getUUID(args[0]);
				if (playerId != null) {
					// Get playtime data
					Long playTimeLong = integration.getPlayTime(playerId);
					if (playTimeLong != null) {
						Duration playTime = Duration.ofMillis(playTimeLong);
						sender.sendMessage(new ComponentBuilder()
								.append(CraftWarsPlugin.getMessagePrefix())
								.append(new TextComponent(args[0])).color(ChatColor.AQUA)
								.append(new TextComponent(" has played for ")).color(ChatColor.GREEN)
								.append(new TextComponent(Long.toString(playTime.toHours()))).color(ChatColor.AQUA)
								.append(new TextComponent("h "))
								.append(new TextComponent(Long.toString(playTime.toMinutes() % 60L)))
								.append(new TextComponent("m"))
								.append(new TextComponent(".")).color(ChatColor.GREEN)
								.create());
						return;
					}
				}
				// No player data
				sender.sendMessage(new ComponentBuilder()
						.append(CraftWarsPlugin.getMessagePrefix())
						.append(new TextComponent("Could not find any data for player ")).color(ChatColor.RED)
						.append(new TextComponent(args[0])).color(ChatColor.DARK_RED)
						.append(new TextComponent("!")).color(ChatColor.RED)
						.create());
			} else {
				if (sender instanceof ProxiedPlayer) {
					ProxiedPlayer senderPlayer = (ProxiedPlayer) sender;
					Long playTimeLong = integration.getPlayTime(senderPlayer.getUniqueId());
					if (playTimeLong != null) {
						Duration playTime = Duration.ofMillis(playTimeLong);
						sender.sendMessage(new ComponentBuilder()
								.append(CraftWarsPlugin.getMessagePrefix())
								.append(new TextComponent("You have played for ")).color(ChatColor.GREEN)
								.append(new TextComponent(Long.toString(playTime.toHours()))).color(ChatColor.AQUA)
								.append(new TextComponent("h "))
								.append(new TextComponent(Long.toString(playTime.toMinutes() % 60L)))
								.append(new TextComponent("m"))
								.append(new TextComponent(".")).color(ChatColor.GREEN)
								.create());
						return;
					}
					// No data for sender (This should never happen you can never be too safe I guess?)
					sender.sendMessage(new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("Could not find any data for you!")).color(ChatColor.RED)
							.create());
				} else {
					
				}

			}
		});
	}

}
