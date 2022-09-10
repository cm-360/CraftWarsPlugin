package com.github.cm360.cwplugin.integrations;

import java.util.logging.Level;

import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.md_5.bungee.api.plugin.Plugin;

public class WhitelistIntegration {

	private final Plugin plugin;
	private DatagramEndpoint endpoint;

	public WhitelistIntegration(Plugin plugin, DatagramEndpoint endpoint) {
		this.plugin = plugin;
		this.endpoint = endpoint;
		endpoint.registerListener("register", data -> dispatchWhitelistCommand("add", data));
		endpoint.registerListener("unregister", data -> dispatchWhitelistCommand("remove", data));
	}
	
	private void dispatchWhitelistCommand(String command, String data) {
		String[] dataSplit = data.split(" ", 2);
		switch (dataSplit[0].toLowerCase()) {
		case "bedrock":
			command = "x-" + command;
		default:
			plugin.getLogger().log(Level.INFO, String.format("Whitelist action performed: %s %s", command, dataSplit[1]));
			plugin.getProxy().getPluginManager().dispatchCommand(plugin.getProxy().getConsole(),
					String.format("whitelist %s %s", command, dataSplit[1]));
		}
	}

}
