package com.github.cm360.cwplugin.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import com.github.cm360.cwplugin.CraftWarsPlugin;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;

public class RulesCommand extends Command {

	private Plugin plugin;
	private List<String> rules;
	
	public RulesCommand(Plugin plugin) {
		super("rules");
		this.plugin = plugin;
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
				.append("Server rules:").color(ChatColor.GREEN)
				.append(IntStream.range(0, rules.size()).mapToObj(i -> {
					return new TextComponent(new ComponentBuilder()
							.append(new TextComponent(String.format("\n%d. ", i + 1))).color(ChatColor.AQUA)
							.append(new TextComponent(rules.get(i))).color(ChatColor.YELLOW)
							.create());
				}).toArray(BaseComponent[]::new)).color(ChatColor.AQUA)
				.create());
	}
	
	private void reload(CommandSender initiator) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			try {
				this.rules = new ArrayList<String>();
				File rulesFile = new File(plugin.getDataFolder(), "rules.txt");
				BufferedReader br = new BufferedReader(new FileReader(rulesFile));
				String line;
				while ((line = br.readLine()) != null)
					rules.add(line);
				br.close();
				// Notify command sender
				if (initiator != null) {
					initiator.sendMessage(new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("Reloaded rules list!")).color(ChatColor.GREEN)
							.create());
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Notify command sender
				if (initiator != null) {
					initiator.sendMessage(new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("Failed to read 'rules.txt', check the log for more details.")).color(ChatColor.RED)
							.create());
				}
			}
		});
	}

}
