package com.github.cm360.cwplugin.integrations.playtime.awards;

import java.util.Map;
import java.util.UUID;

import com.github.cm360.cwplugin.CraftWarsPlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.track.PromotionResult;
import net.luckperms.api.track.Track;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;

public class PermissionGroupAward implements PlayTimeAward 	{

	private long timeNeeded;
	private LuckPerms luckPerms;
	private String trackName;
	private Map<String, String> contexts;
	
	public PermissionGroupAward(long timeNeeded, LuckPerms luckPerms, String trackName, Map<String, String> contexts) {
		this.timeNeeded = timeNeeded;
		this.luckPerms = luckPerms;
		this.trackName = trackName;
		this.contexts = contexts;
	}
	
	@Override
	public long getTimeNeeded() {
		return timeNeeded;
	}
	
	@Override
	public void awardPlayer(UUID playerId) {
		luckPerms.getUserManager().loadUser(playerId).thenAcceptAsync(user -> {
			try {
				// Get track by name
				Track track = luckPerms.getTrackManager().getTrack(trackName);
				// Build context set from key-value pairs
				ImmutableContextSet.Builder contextBuilder = ImmutableContextSet.builder();
				contexts.keySet().forEach(contextName -> contextBuilder.add(contextName, contexts.get(contextName)));
				ImmutableContextSet contextSet = contextBuilder.build();
				// Promote user and save
				PromotionResult result = track.promote(user, contextSet);
				if (result.wasSuccessful()) {
					luckPerms.getUserManager().saveUser(user);
					// Notify player of earning
					notifyPlayer(playerId, new ComponentBuilder()
							.append(CraftWarsPlugin.getMessagePrefix())
							.append(new TextComponent("As a thank-you for playing, you have been promoted to ")).color(ChatColor.GREEN)
							.append(new TextComponent(result.getGroupTo().get())).color(ChatColor.AQUA)
							.append(new TextComponent("!")).color(ChatColor.GREEN)
							.create());
				} else {
					throw new Exception(String.format("Failed to promote along track '%s'", trackName));
				}
			} catch (Exception e) {
				// Log error
				System.out.printf("Error while awarding user %s (%s)!\n", user.getFriendlyName(), playerId);
				e.printStackTrace();
				// Notify player
				notifyPlayer(playerId, new ComponentBuilder()
						.append(CraftWarsPlugin.getMessagePrefix())
						.append(new TextComponent("Oops, an internal error occured while processing your award! Please notify an admin."))
						.color(ChatColor.RED)
						.create());
			}
		});
	}

}
