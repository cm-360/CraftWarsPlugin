package com.github.cm360.cwplugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;

import com.github.cm360.cwplugin.commands.DiscordCommand;
import com.github.cm360.cwplugin.commands.RulesCommand;
import com.github.cm360.cwplugin.integrations.IntegrationsListener;
import com.sun.net.httpserver.HttpServer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class CraftWarsPlugin extends Plugin {
	
	private Configuration config;
	private URL discordWebhookUrl;
	private String apiEndpointUrl;
	private String skinIconsUrl;
	
	private HttpServer apiServer;
	private IntegrationsListener integrations;
	
	private LuckPerms luckPerms;
	
	@Override
	public void onEnable() {
		try {
			getOrMakeConfig();
			
			PluginManager pluginManager = getProxy().getPluginManager();
			if (pluginManager.getPlugin("LuckPerms") != null)
				luckPerms = LuckPermsProvider.get();
			if (pluginManager.getPlugin("PlaceholderAPI") != null) {
				// Do nothing for now :D
			}
			
			// Create HTTP server
			String bindAddress = config.getString("http_server.bind_address");
			int bindPort = config.getInt("http_server.bind_port");
			apiServer = HttpServer.create(new InetSocketAddress(bindAddress, bindPort), 0);
			
			// Register Bungeecord API event listeners
			discordWebhookUrl = new URL(config.getString("urls.discord_webhook"));
			apiEndpointUrl = config.getString("urls.api_endpoint");
			skinIconsUrl = config.getString("urls.skin_icons");
			integrations = new IntegrationsListener(this);
			pluginManager.registerListener(this, integrations);
			
			// Register commands
			pluginManager.registerCommand(this, new DiscordCommand(this));
			pluginManager.registerCommand(this, new RulesCommand(this));
			
			// Start HTTP server
			apiServer.start();
			getLogger().info(String.format("HTTP server successfully started on %s:%d", bindAddress, bindPort));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onDisable() {
		integrations.close();
	}
	
	public void getOrMakeConfig() throws IOException {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			// Copy default config
			FileOutputStream outputStream = new FileOutputStream(configFile);
			InputStream in = getResourceAsStream("config.yml");
			in.transferTo(outputStream);
		}
		config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
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
	
	public HttpServer getLocalApiServer() {
		return apiServer;
	}
	
	public LuckPerms getLuckPermsInstance() {
		return luckPerms;
	}
	
	public URL getDiscordWebhookUrl() {
		return discordWebhookUrl;
	}
	
	public String getRemoteApiEndpointUrl() {
		return apiEndpointUrl;
	}
	
	public String getSkinIconsUrl() {
		return skinIconsUrl;
	}

}
