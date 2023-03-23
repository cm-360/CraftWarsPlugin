package com.github.cm360.cwplugin.integrations;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import com.github.cm360.cwplugin.CraftWarsPlugin;
import com.github.cm360.cwplugin.network.HttpPostClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class PlayerListIntegration {

	private final Plugin plugin;
	private String apiUrlTemplate;

	public PlayerListIntegration(CraftWarsPlugin plugin) {
		this.plugin = plugin;
		this.apiUrlTemplate = plugin.getRemoteApiEndpointUrl();
	}
	
	public void playerListUpdated(Collection<ProxiedPlayer> players) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			JsonObject postData = new JsonObject();
			JsonArray playerNames = new JsonArray();			
			players.stream().map(ProxiedPlayer::getName).sorted().forEach(p -> playerNames.add(p));
			postData.add("players", playerNames);
			try {
				HttpPostClient.doJsonPost(new URL(apiUrlTemplate + "update_player_list"), postData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

}
