package com.github.cm360.cwplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;

import com.github.cm360.cwplugin.commands.DiscordCommand;
import com.github.cm360.cwplugin.commands.RulesCommand;
import com.github.cm360.cwplugin.integrations.IntegrationsListener;
import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

public class CraftWarsPlugin extends Plugin {

	private DatagramEndpoint endpoint;
	private IntegrationsListener integrations;
	private LuckPerms luckPerms;
	
	@Override
	public void onEnable() {
		try {
			PluginManager pluginManager = getProxy().getPluginManager();
			// Obtain LuckPerms API object if loaded
			if (pluginManager.getPlugin("LuckPerms") != null)
				luckPerms = LuckPermsProvider.get();
			// PlaceholderAPI
			if (getProxy().getPluginManager().getPlugin("PlaceholderAPI") != null) {
				// Do nothing for now :D
			}
			// Create UDP endpoint
			InetAddress localhost = InetAddress.getLoopbackAddress();
			int bindPort = 9899; // 98 and 99 are the ASCII character codes for B and C (for BungeeCord!)
			int sendPort = 9989;
			long guildId = readGuildId();
			endpoint = new DatagramEndpoint(this, localhost, bindPort, localhost, sendPort, guildId);
			getLogger().info(String.format("UDP socket successfully bound to %s:%d, sending packets to %s:%d", localhost, bindPort, localhost, sendPort));
			// Register Bungeecord API event listeners
			integrations = new IntegrationsListener(this, endpoint, luckPerms);
			getProxy().getPluginManager().registerListener(this, integrations);
			// Register commands
			getProxy().getPluginManager().registerCommand(this, new DiscordCommand(this));
			getProxy().getPluginManager().registerCommand(this, new RulesCommand(this));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		integrations.close();
		endpoint.close();
	}
	
	public static BaseComponent[] getMessagePrefix() {
		return new ComponentBuilder()
				.append(new TextComponent("[")).color(ChatColor.BLUE)
				.append(new TextComponent("CraftWars")).color(ChatColor.GOLD)
				.append(new TextComponent("]")).color(ChatColor.BLUE)
				.append(new TextComponent(" ")).color(ChatColor.RESET)
				.create();
	}
	
	public static BaseComponent[] getNoPermissionMessage() {
		return new ComponentBuilder()
				.append(getMessagePrefix())
				.append(new TextComponent("You do not have permission to run this command!")).color(ChatColor.RED)
				.create();
	}
	
	private long readGuildId() throws IOException {
		File guildIdFile = new File(getDataFolder(), "guild_id.txt");
		BufferedReader br = new BufferedReader(new FileReader(guildIdFile));
		long guildId = Long.parseLong(br.readLine());
		br.close();
		return guildId;
	}

}
