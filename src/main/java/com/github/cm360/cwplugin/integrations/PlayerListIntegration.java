package com.github.cm360.cwplugin.integrations;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class PlayerListIntegration {

	private final Plugin plugin;
	private DatagramEndpoint endpoint;

	public PlayerListIntegration(Plugin plugin, DatagramEndpoint endpoint) {
		this.plugin = plugin;
		this.endpoint = endpoint;
	}
	
	public void playerListUpdated(Collection<ProxiedPlayer> players) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			List<String> sortedPlayerList = players.stream().map(player -> player.getName()).sorted().collect(Collectors.toList());
			endpoint.send("playerlist", String.join(",", sortedPlayerList));
		});
	}

}
