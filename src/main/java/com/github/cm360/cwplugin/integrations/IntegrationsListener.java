package com.github.cm360.cwplugin.integrations;

import java.util.stream.Collectors;

import com.github.cm360.cwplugin.CraftWarsPlugin;
import com.github.cm360.cwplugin.integrations.playtime.PlayTimeIntegration;
import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class IntegrationsListener implements Listener {

	private final Plugin plugin;
	private LuckPerms luckPerms;
	private ChatIntegration chatIntegration;
	private PlayerListIntegration playerListIntegration;
	private PlayTimeIntegration playTimeIntegration;
	private WhitelistIntegration whitelistIntegration;
	
	public IntegrationsListener(CraftWarsPlugin plugin, DatagramEndpoint endpoint, LuckPerms luckPerms) {
		this.plugin = plugin;
		this.luckPerms = luckPerms;
		this.chatIntegration = new ChatIntegration(plugin, endpoint);
		this.playerListIntegration = new PlayerListIntegration(plugin, endpoint);
		this.playTimeIntegration = new PlayTimeIntegration(plugin, endpoint, luckPerms);
		this.whitelistIntegration = new WhitelistIntegration(plugin, endpoint);
	}
	
	@EventHandler
	public void onPlayerJoin(PostLoginEvent event) {
		ProxiedPlayer eventPlayer = event.getPlayer();
		// Send message
		chatIntegration.systemMessage(String.format("%s joined the network", eventPlayer.getName()));
		// Update player list
		playerListIntegration.playerListUpdated(ProxyServer.getInstance().getPlayers());
		// Update play time statistic
		playTimeIntegration.playerJoined(eventPlayer);
	}
	
	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		ProxiedPlayer eventPlayer = event.getPlayer();
		// Send message
		chatIntegration.systemMessage(String.format("%s left the network", eventPlayer.getName()));
		// Update player list
		playerListIntegration.playerListUpdated(plugin.getProxy().getPlayers().stream()
				.filter(player -> !player.equals(eventPlayer))
				.collect(Collectors.toSet()));
		// Update play time statistic
		playTimeIntegration.playerLeft(eventPlayer);
	}
	
	@EventHandler
	public void onServerSwitch(ServerSwitchEvent event) {
		ProxiedPlayer eventPlayer = event.getPlayer();
		ServerInfo fromServer = event.getFrom();
		if (fromServer != null)
			chatIntegration.systemMessage(String.format("%s moved from %s to %s", eventPlayer.getName(), fromServer.getName(), eventPlayer.getServer().getInfo().getName()));
	}
	
	@EventHandler
	public void onChatMessage(ChatEvent event) {
		Connection sender = event.getSender();
		String message = event.getMessage();
		if (sender instanceof ProxiedPlayer) {
			if (!message.startsWith("/"))
				chatIntegration.messageSent(((ProxiedPlayer) sender).getName(), message);
		} else {
			chatIntegration.systemMessage("[System Message] " + message);
		}
	}
	
	public void close() {
		playTimeIntegration.sendData();
		playTimeIntegration.saveData();
	}
	
	public void reloadConfig() {
		
	}

}
