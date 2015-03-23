package com.gmail.jython234.udb;

import com.gmail.jython234.udb.io.PlayerDatabase;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.Random;

/**
 * Plugin Listener for events.
 */
public class UDBListener implements Listener{
    private UDBPlugin plugin;

    public UDBListener(UDBPlugin plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e){
        Player player = e.getPlayer();
        if(plugin.getPlayerDatabase().isInDatabase(player)){
            PlayerDatabase.PlayerData playerData = plugin.getPlayerDatabase().getDataByUUID(player.getUniqueId().toString());
            if(playerData.isBanned()){
                long elapsed = System.currentTimeMillis() - playerData.getBanTime();
                int minutes = (((int) (elapsed / 1000)) / 60);
                if(minutes >= plugin.getTempBanTime()){
                    playerData.setBanned(false);
                    playerData.setLives(1);
                    playerData.setSurvivalRecord(Long.toString(System.currentTimeMillis()));
                    playerData.setBanTime(-1);
                } else {
                    int difference = plugin.getTempBanTime() - minutes;
                    int hourDiff = difference / 60;
                    int minDiff = difference - (hourDiff * 60);
                    String diff = hourDiff +" hours, "+minDiff+" minutes.";
                    e.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your UDB ban is not over yet. You still have: "+diff);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent e){
        final Player player = e.getPlayer();
        if(!plugin.getPlayerDatabase().isInDatabase(player)){
            try {
                plugin.getPlayerDatabase().createNewData(player);
            } catch (IOException e1) {
                plugin.getLogger().severe("Failed to create data for " + player.getDisplayName() + ", IOException: " + e1.getMessage());
                e1.printStackTrace();
            }
        }
        final PlayerDatabase.PlayerData playerData = plugin.getPlayerDatabase().getDataByUUID(player.getUniqueId().toString());
        new BukkitRunnable() { //Delay this so it shows up after MOTD
            @Override
            public void run() {
                plugin.sendMessage(player,"Welcome to UDB, "+ChatColor.GREEN+player.getDisplayName()+ChatColor.YELLOW+"!");
                plugin.sendMessage(player,"You have "+ChatColor.GREEN+playerData.getLives()+" lives.");
                plugin.sendMessage(player,"Your current survival record is: "+ChatColor.GREEN+playerData.printSurvivalRecord());
            }
        }.runTaskLater(plugin, 20); //Delay this so it shows up after MOTD
        plugin.sendMessage(player, "This server is running UDB, version "+UDBPlugin.VERSION+" by jython234");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent e){
        Player player = e.getPlayer();
        if(plugin.getPlayerDatabase().isInDatabase(player)){
            try {
                plugin.getPlayerDatabase().getDataByUUID(player.getUniqueId().toString()).save();
            } catch (IOException e1) {
                plugin.getLogger().warning("Failed to save data for "+player.getDisplayName()+", IOException: "+e1.getMessage());
                e1.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e){ //TODO: Move this over to EntityDamageEvent so we can get attacker information for bounty system.
        if(e.getEntity() instanceof Player) {
            Player player = e.getEntity();
            if (plugin.getPlayerDatabase().isInDatabase(player)) {
                PlayerDatabase.PlayerData data = plugin.getPlayerDatabase().getDataByUUID(player.getUniqueId().toString());
                int oldLives = data.getLives();
                data.setLives(oldLives - 1);

                plugin.sendMessage(player, "You have lost "+ChatColor.RED+" 1 life!");
                plugin.sendMessage(player, "You now have "+ChatColor.GREEN+data.getLives()+" lives!");

                if(player.getKiller() instanceof Player){
                    if((new Random().nextInt(100)) < 25){
                        plugin.sendMessage(player.getKiller(), "You have gained +1 lives from the kill!");
                        PlayerDatabase.PlayerData playerData = plugin.getPlayerDatabase().getDataByUUID(player.getKiller().getUniqueId().toString());
                        playerData.setLives(playerData.getLives() + 1);
                        plugin.sendMessage(player, "Your killer has gained +1 lives!");
                    } else {
                        plugin.sendMessage(player.getKiller(), "You didn't get a life from the kill!");
                        plugin.sendMessage(player, "Your killer didn't get a life from the kill.");
                    }
                }

                if(data.getLives() <= 0){
                    data.setBanned(true);
                    data.setBanTime(System.currentTimeMillis());
                    if(plugin.getTempBanTime() == 0){ //Permaban
                        player.kickPlayer("You have run out of lives! Ban time on this server is: permanent.");
                        plugin.getServer().broadcastMessage(ChatColor.GOLD+"["+ChatColor.AQUA+"UDB"+ChatColor.GOLD+"] "+ChatColor.YELLOW+player.getDisplayName()+" has been banned due to 0 lives!");
                    } else if(plugin.getTempBanTime() < 0){
                        data.setBanned(false);
                        data.setBanTime(-1);
                        plugin.sendMessage(player, "Banning on this server has been disabled. You're lucky this time...");
                    } else {
                        int hours = plugin.getTempBanTime() / 60;
                        int minutes = plugin.getTempBanTime() - (hours * 60);

                        player.kickPlayer("You have run out of lives! Ban time on this server is: "+hours+" hours, "+minutes+" minutes.");
                        plugin.getServer().broadcastMessage(ChatColor.GOLD+"["+ChatColor.AQUA+"UDB"+ChatColor.GOLD+"] "+ChatColor.YELLOW+player.getDisplayName()+" has been banned due to 0 lives!");
                    }
                }
            }
        }
    }
}
