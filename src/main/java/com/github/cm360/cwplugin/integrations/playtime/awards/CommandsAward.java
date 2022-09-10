package com.github.cm360.cwplugin.integrations.playtime.awards;

import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;

public class CommandsAward implements PlayTimeAward {

	private long timeNeeded;
	private List<String> commands;
	private boolean asPlayer;

	public CommandsAward(long timeNeeded, List<String> commands, boolean asPlayer) {
		this.commands = commands;
		this.asPlayer = asPlayer;
	}
	
	@Override
	public long getTimeNeeded() {
		return timeNeeded;
	}
	
	@Override
	public void awardPlayer(UUID playerId) {
		CommandSender target = asPlayer ? ProxyServer.getInstance().getPlayer(playerId) : ProxyServer.getInstance().getConsole();
		commands.forEach(command -> target.sendMessage(new TextComponent(command)));
		;
	}

}
