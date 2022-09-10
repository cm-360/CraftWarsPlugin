package com.github.cm360.cwplugin.integrations.playtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.github.cm360.cwplugin.commands.PlayTimeCommand;
import com.github.cm360.cwplugin.integrations.playtime.awards.CommandsAward;
import com.github.cm360.cwplugin.integrations.playtime.awards.PermissionGroupAward;
import com.github.cm360.cwplugin.integrations.playtime.awards.PlayTimeAward;
import com.github.cm360.cwplugin.network.DatagramEndpoint;

import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class PlayTimeIntegration {

	private final Plugin plugin;
	private LuckPerms luckPerms;
	private ScheduledTask checkTask;
	private ScheduledTask saveTask;
	private DatagramEndpoint endpoint;
	private Map<UUID, Long> playTimes;
	private Map<UUID, Long> startTimes;
	private Map<UUID, String> usernameCache;
	private Map<UUID, List<String>> earnedAwards;
	private Map<String, PlayTimeAward> configuredAwards;
	
	
	public PlayTimeIntegration(Plugin plugin, DatagramEndpoint endpoint, LuckPerms luckPerms) {
		this.plugin = plugin;
		this.luckPerms = luckPerms;
		this.endpoint = endpoint;
		this.playTimes = new HashMap<UUID, Long>();
		this.startTimes = new HashMap<UUID, Long>();
		this.usernameCache = new HashMap<UUID, String>();
		this.earnedAwards = new HashMap<UUID, List<String>>();
		this.configuredAwards = new HashMap<String, PlayTimeAward>();
		// Register playtime command
		plugin.getProxy().getPluginManager().registerCommand(plugin, new PlayTimeCommand(plugin, this));
		// Read awards
		try {
			Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(plugin.getDataFolder(), "awards.yml"));
			config.getKeys().stream().forEach(awardId -> {
				try {
					Configuration section = config.getSection(awardId);
					configuredAwards.put(awardId, createAwardFromConfig(section));
				} catch (Exception e) {
					System.out.printf("Error while parsing '%s'\n", awardId);
					e.printStackTrace();
				}
			});
		} catch (IOException e) {
			// File does not exist
			e.printStackTrace();
		}
		// Read player data
		try {
			Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(plugin.getDataFolder(), "playerdata.yml"));
			config.getKeys().forEach(uuidString -> {
				UUID uuid = UUID.fromString(uuidString);
				Configuration section = config.getSection(uuidString);
				playTimes.put(uuid, section.getLong("time_played"));
				usernameCache.put(uuid, section.getString("username"));
				earnedAwards.put(uuid, section.getStringList("awards"));
			});
		} catch (IOException e) {
			saveData();
		}
		setCheckPeriod(5, TimeUnit.MINUTES);
		setSavePeriod(1, TimeUnit.HOURS);
	}
	
	public PlayTimeAward createAwardFromConfig(Configuration awardConfig) {
		String awardType = awardConfig.getString("type");
		Configuration timeConfig = awardConfig.getSection("time");
		long timeNeeded = TimeUnit.MILLISECONDS.convert(timeConfig.getInt("value"), TimeUnit.valueOf(timeConfig.getString("unit").toUpperCase()));
		if (awardType.equalsIgnoreCase("permission_group")) {
			if (luckPerms == null) {
				System.out.printf("Award type '%s' cannot be used because LuckPerms is not installed!\n", awardType.toLowerCase());
			} else {
				Configuration contextsConfig = awardConfig.getSection("contexts");
				Map<String, String> contexts = contextsConfig.getKeys().stream().collect(Collectors.toMap(key -> key, key -> contextsConfig.getString(key)));
				return new PermissionGroupAward(timeNeeded, luckPerms, awardConfig.getString("track"), contexts);
			}
		} else if (awardType.equalsIgnoreCase("commands")) {
			return new CommandsAward(timeNeeded, awardConfig.getStringList("commands"), awardConfig.getBoolean("as_player"));
		} else {
			System.out.printf("Unrecognized award type '%s'", awardType);
		}
		return null;
	}
	
	public synchronized UUID getUUID(String username) {
		ProxyServer proxy = plugin.getProxy();
		ProxiedPlayer player = proxy.getPlayer(username);
		if (player == null) {
			for (UUID uuid : usernameCache.keySet()) {
				if (usernameCache.get(uuid).equalsIgnoreCase(username))
					return uuid;
			}
			return null;
		} else {
			return player.getUniqueId();
		}
	}
	
	public synchronized Long getPlayTime(UUID playerId) {
		if (plugin.getProxy().getPlayer(playerId) != null)
			updatePlayTime(playerId);
		return playTimes.get(playerId);
		
	}
	
	public synchronized void updatePlayTime(UUID playerId) {
		try {
			long currentTime = System.currentTimeMillis();
			// Update play time and reset start time
			playTimes.put(playerId, playTimes.get(playerId) + (currentTime - startTimes.get(playerId)));
			startTimes.put(playerId, currentTime);
			// Check if any awards should be given
			checkPlayTimeAwards(playerId);
		} catch (Exception e) { // NullPointerException if player is somehow not in one of the maps
			System.out.printf("Error while updating playtime for %s\n", playerId);
			System.out.printf("  Player's start time: %d\n", startTimes.getOrDefault(playerId, -1L));
			System.out.printf("  Player's play time: %d\n", playTimes.getOrDefault(playerId, -1L));
			e.printStackTrace();
		}
	}
	
	public synchronized void checkPlayTimeAwards(UUID playerId) {
		configuredAwards.forEach((awardId, award) -> {
			// Check that the player has played longer than required and has not already earned this
			if (playTimes.get(playerId) >= award.getTimeNeeded() && !earnedAwards.get(playerId).contains(awardId)) {
				award.awardPlayer(playerId);
				earnedAwards.get(playerId).add(awardId);
				System.out.printf("Awarded '%s' to %s\n", awardId, playerId);
			}
		});
	}
	
	public synchronized void updateAllPlayTimes() {
		// Update play time of each online player
		plugin.getProxy().getPlayers().forEach(player -> updatePlayTime(player.getUniqueId()));
	}
	
	public synchronized SortedMap<UUID, Long> getSortedPlayTimes() {
		SortedMap<UUID, Long> topPlayers = new TreeMap<UUID, Long>((id1, id2) -> {
			return Long.signum(playTimes.getOrDefault(id2, 0L) - playTimes.getOrDefault(id1, 0L));
		});
		topPlayers.putAll(playTimes);
		return topPlayers;
	}

	public synchronized void playerJoined(ProxiedPlayer eventPlayer) {
		UUID playerId = eventPlayer.getUniqueId();
		long currentTime = System.currentTimeMillis();
		// Check if player has joined before
		if (playTimes.containsKey(playerId)) {
			checkPlayTimeAwards(playerId);
			// Update username
			usernameCache.put(playerId, eventPlayer.getName());
		} else {
			// Add player to info maps
			playTimes.put(playerId, 0L);
			usernameCache.put(playerId, eventPlayer.getName());
			earnedAwards.put(playerId, new ArrayList<String>());
			// Send to stats tracker and save
			sendData();
			saveData();
		}
		// Set join time of player
		startTimes.put(playerId, currentTime);
	}

	public synchronized void playerLeft(ProxiedPlayer eventPlayer) {
		UUID playerId = eventPlayer.getUniqueId();
		// Update play time and reset start time
		updatePlayTime(playerId);
		startTimes.remove(playerId);
		// Send to stats tracker
		sendData();
	}
	
	public synchronized void sendData() {
		// Send data over UDP
		SortedMap<UUID, Long> playerTimesSorted = getSortedPlayTimes();
		plugin.getProxy().getScheduler().runAsync(plugin, () -> {
			endpoint.send("playtimes", String.join(",", playerTimesSorted.keySet().stream()
					.map(uuid -> {
						ProxiedPlayer player = plugin.getProxy().getPlayer(uuid);
						return String.format("%s %d", player == null ? usernameCache.get(uuid) : player.getName(), playerTimesSorted.get(uuid));
					}).collect(Collectors.toList())));
		});
	}
	
	public synchronized void saveData() {
		try {
			Configuration config = new Configuration();
			for (UUID playerId : playTimes.keySet()) {
				Map<String, Object> playerData = new HashMap<String, Object>();
				playerData.put("time_played", playTimes.get(playerId));
				playerData.put("username", usernameCache.get(playerId));
				playerData.put("awards", earnedAwards.get(playerId));
				config.set(playerId.toString(), playerData);
			}
			if (!plugin.getDataFolder().exists())
				plugin.getDataFolder().mkdir();
			ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(plugin.getDataFolder(), "playerdata.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setCheckPeriod(int period, TimeUnit unit) {
		TaskScheduler scheduler = plugin.getProxy().getScheduler();
		if (checkTask != null)
			scheduler.cancel(checkTask);
		checkTask = scheduler.schedule(plugin, () -> {
			updateAllPlayTimes();
			sendData();
		}, 0L, period, unit);
	}
	
	public void setSavePeriod(int period, TimeUnit unit) {
		TaskScheduler scheduler = plugin.getProxy().getScheduler();
		if (saveTask != null)
			scheduler.cancel(saveTask);
		saveTask = scheduler.schedule(plugin, () -> saveData(), 0L, period, unit);
	}

}
