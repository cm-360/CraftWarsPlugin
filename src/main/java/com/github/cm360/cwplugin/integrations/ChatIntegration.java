package com.github.cm360.cwplugin.integrations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.github.cm360.cwplugin.CraftWarsPlugin;
import com.github.cm360.cwplugin.network.HttpPostClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Plugin;

public class ChatIntegration {

	private final Plugin plugin;
	private URL discordWebhookUrl;
	private String skinIconUrlTemplate;
	
	public ChatIntegration(CraftWarsPlugin plugin) {
		this.plugin = plugin;
		this.discordWebhookUrl = plugin.getDiscordWebhookUrl();
		this.skinIconUrlTemplate = plugin.getSkinIconsUrl();
		plugin.getLocalApiServer().createContext("/chat_message", new HttpHandler() {
			@Override
			public void handle(HttpExchange arg0) throws IOException {
				JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(arg0.getRequestBody())));
				ChatMessage message = new Gson().fromJson(jsonReader, ChatMessage.class);
				messageReceived(message);
				jsonReader.close();
				arg0.sendResponseHeaders(200, -1);
				arg0.close();
			}
		});
	}
	
	public void messageReceived(ChatMessage message) {
		plugin.getProxy().broadcast(new ComponentBuilder()
				.append("[").color(ChatColor.DARK_BLUE)
				.append("Discord").color(ChatColor.BLUE)
				.append("] ").color(ChatColor.DARK_BLUE)
				.append(message.username).color(ChatColor.AQUA)
				.append(" > ").color(ChatColor.GREEN)
				.append(message.content).color(ChatColor.WHITE)
				.create());
	}
	
	public void messageSent(String username, String message) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			try {
				JsonObject postData = new JsonObject();
				postData.addProperty("content", message);
				postData.addProperty("username", username);
				postData.addProperty("avatar_url", String.format(skinIconUrlTemplate, username));
				HttpPostClient.doJsonPost(discordWebhookUrl, postData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	public void systemMessage(String message) {
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			try {
				JsonObject postData = new JsonObject();
				postData.addProperty("content", "*" + message + "*");
				HttpPostClient.doJsonPost(discordWebhookUrl, postData);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}
	
	class ChatMessage {
		String username;
		String content;
	}

}
