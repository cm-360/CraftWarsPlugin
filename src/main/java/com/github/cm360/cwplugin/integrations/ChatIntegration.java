package com.github.cm360.cwplugin.integrations;

import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;

public class ChatIntegration {

	private final Plugin plugin;
	private DatagramEndpoint endpoint;
	
	public ChatIntegration(Plugin plugin, DatagramEndpoint endpoint) {
		this.plugin = plugin;
		this.endpoint = endpoint;
		endpoint.registerListener("chat", data -> messageReceived(data));
	}
	
	public void messageReceived(String content) {
		String[] contentSplit = content.split("(?<=#\\d+)\\s", 2);
		plugin.getProxy().broadcast(new ComponentBuilder()
				.append("[").color(ChatColor.DARK_BLUE)
				.append("Discord").color(ChatColor.BLUE)
				.append("] ").color(ChatColor.DARK_BLUE)
				.append(contentSplit[0]).color(ChatColor.AQUA)
				.append(" > ").color(ChatColor.GREEN)
				.append(contentSplit[1]).color(ChatColor.WHITE)
				.create());
	}
	
	public void messageSent(String username, String message) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			endpoint.send("chat", username + " " + message);
		});
	}
	
	public void systemMessage(String message) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			endpoint.send("chat_system", message);
		});
	}

}
