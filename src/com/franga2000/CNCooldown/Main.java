package com.franga2000.CNCooldown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.entity.BoardColls;
import com.massivecraft.massivecore.ps.PS;

public class Main extends JavaPlugin implements Listener {
	
	HashMap<String, Integer> cooldown = new HashMap<String, Integer>();
	List<String> warmups = new ArrayList<String>();
	String prefix;
	ConfigurationSection commands;
	
	public void onEnable() {
		saveDefaultConfig();
		saveConfig();
		
		this.getServer().getPluginManager().registerEvents(this, this);
		
		prefix = getConfig().getString("prefix");
		commands = getConfig().getConfigurationSection("commands");
	}
	
	public void onDisable() {
		//saveConfig();
	}
	
	public int Cooldown(String p, int cdTime) {
		cdTime = cdTime * 1000;
		if(cooldown.containsKey(p)) {
			int calcTime = (int) (System.currentTimeMillis() - cooldown.get(p));
			if (calcTime > cdTime) {
				return 0;
			}
			else {
				int calcCooldownTime = Math.round(((cdTime - calcTime) / 1000) + 1);
				return calcCooldownTime;
			}
		}
		else {
			return 0;
		}	
	}
	
	public int getValue(String command, String type, String what) {
		if (commands.getString(command + "." + what + "." + type) != null) {
			return commands.getInt(command + "." + what + "." + type);
		} else {
			return commands.getInt(command + "." + what + ".default");
		}
	}
	
	public String getType(Player p) {	
		//Factions integration
		if (getConfig().getBoolean("integration.factions")) {
			if (BoardColls.get().getFactionAt(PS.valueOf(p.getLocation())).getName().equalsIgnoreCase("WarZone")) return "warzone";
		}
		
		return "default";
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerCommandPreprocess(final PlayerCommandPreprocessEvent e) {
		if(!(e.getPlayer() instanceof Player)) {
			return;
		}
		
		String cmd = e.getMessage().replaceFirst("/", "");
		cmd = cmd.split(" ")[0];
		
		if (commands.getKeys(false).contains(cmd)) {
			final Player player = (Player) e.getPlayer();
			String type = getType(player);
			
			//Check for cooldown
			int cooldownLeft = Cooldown(player.getName(), getValue(cmd, type, "cooldown"));
			if (cooldownLeft != 0) {
				e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages.CommandOnCooldown").replaceAll("\\{cooldown\\}", cooldownLeft + "").replaceAll("\\{command\\}", cmd)));
				e.setCancelled(true);
				return;
			}
			
			//Check for warmup
			if (!warmups.contains(player.getName())) {
				int warmup = getValue(cmd, type, "warmup");
				
				e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages.CommandWarmingUp").replaceAll("\\{warmup\\}", warmup + "")));
				warmups.add(player.getName());
				this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
					@Override
					public void run() {
						if (warmups.contains(e.getPlayer().getName())) {
							getServer().dispatchCommand(player, e.getMessage().replaceFirst("/", ""));
							
							cooldown.put(player.getName(), (int) System.currentTimeMillis());
							warmups.remove(player.getName());
							e.setCancelled(false);
						}
					}
				}, warmup * 20);
			}

			e.setCancelled(true);
			return;
		}
		return;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		if (warmups.contains(e.getPlayer().getName())) {
			e.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + getConfig().getString("messages.WarmupCanceled")));
			warmups.remove(e.getPlayer().getName());
		}
	}
}
