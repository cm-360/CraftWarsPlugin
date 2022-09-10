package com.github.cm360.cwplugin.integrations.playtime.awards;

import java.util.UUID;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;

public interface PlayTimeAward {
	
	public void awardPlayer(UUID playerId);

	public long getTimeNeeded();
	
	public default void notifyPlayer(UUID playerId, BaseComponent... message) {
		ProxyServer.getInstance().getPlayer(playerId).sendMessage(message);
	}

}
