package com.github.cm360.cwplugin.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.github.cm360.cwplugin.CraftWarsPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class DiscordCommand extends Command {

	private Plugin plugin;
	private String link;
	
	public DiscordCommand(Plugin plugin) {
		super("discord");
		this.plugin = plugin;
		this.link = "";
		reload(null);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if (args.length > 0 && args[0].equals("reload")) {
			if (sender.hasPermission(String.join(".", "craftwars", this.getName(), "reload"))) {
				reload(sender);
			} else {
				// No permission
				sender.sendMessage(CraftWarsPlugin.getNoPermissionMessage());
			}
			return;
		}
		sender.sendMessage(new ComponentBuilder()
				.append(CraftWarsPlugin.getMessagePrefix())
				.append(new TextComponent("Discord invite: ")).color(ChatColor.GREEN)
				.append(new TextComponent(link)).event(new ClickEvent(ClickEvent.Action.OPEN_URL, link)).color(ChatColor.AQUA)
				.create());
	}
	
	private void reload(CommandSender initiator) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			try {
				File discordLinkFile = new File(plugin.getDataFolder(), "discord.txt");
				BufferedReader br = new BufferedReader(new FileReader(discordLinkFile));
				link = br.readLine();
				br.close();
				// Notify command sender
				if (initiator != null) {
					initiator.sendMessage(new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("Reloaded Discord invite!")).color(ChatColor.GREEN)
							.create());
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Notify command sender
				if (initiator != null) {
					initiator.sendMessage(new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("Failed to read 'discord.txt', check the log for more details.")).color(ChatColor.RED)
							.create());
				}
			}
		});
	}

}
