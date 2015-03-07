package com.gmail.jython234.udb;

import com.gmail.jython234.udb.io.PlayerDatabase;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * UDB Plugin Main class.
 */
public class UDBPlugin extends JavaPlugin{
    public final static String VERSION = "1.0-BETA_BUILD";
    private int tempBanTime; //Temp ban time is in hours
    private int startLives; //Amount of lives new players start with.

    private PlayerDatabase db;

    @Override
    public void onEnable(){
        try {
            loadSettings();
        } catch(Exception e){
            getLogger().severe("Failed to load settings, Exception: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
        try {
            loadData();
        } catch (IOException e) {
            getLogger().severe("Failed to load data, IOException: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
        getServer().getPluginManager().registerEvents(new UDBListener(this), this);
        getLogger().info("UDB Enabled!");
    }

    private void loadSettings() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        tempBanTime = getConfig().getInt("lives.banTime");
        startLives = getConfig().getInt("lives.firstLives");
        getLogger().info("Settings loaded!");
    }

    private void loadData() throws IOException {
        db = PlayerDatabase.loadDatabase(new File(getDataFolder().getAbsolutePath()+File.separator+"playerDB"), this);
        getLogger().info("Database loaded!");
    }

    @Override
    public void onDisable(){
        try {
            db.saveAll();
        } catch (IOException e) {
            getLogger().warning("Failed to save data, IOException: "+e.getMessage());
            e.printStackTrace();
        }
        getLogger().info("UDB Disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args){
        if(lbl.equalsIgnoreCase("udb")){
            try {
                String operation = args[0];
                if(operation.equalsIgnoreCase("help")){
                    displayHelp(sender);
                } else if(operation.equalsIgnoreCase("setlives")){
                    if(sender.hasPermission("udb.setlives")) {
                        if (args.length > 1) {
                            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                            if (db.isInDatabase(player)) {
                                try {
                                    db.getDataByUUID(player.getUniqueId().toString()).setLives(Integer.parseInt(args[2]));
                                    sendMessage(sender, "Set " + ChatColor.GREEN + args[1] + "/" + player.getUniqueId().toString() + "'s lives to " + args[2]);
                                } catch (NumberFormatException e) {
                                    sendMessage(sender, ChatColor.RED + "Please enter a valid Integer.");
                                }
                            } else {
                                sendMessage(sender, ChatColor.RED + args[1] + " is not in UDB's database.");
                            }
                        } else {
                            sendMessage(sender, "Usage: /udb setlives [player] [lives]");
                        }
                    } else {
                        sendMessage(sender, ChatColor.RED+"You must have the udb.setlives permission.");
                    }
                } else {
                    sendMessage(sender, "Usage: /udb [operation]");
                }
            } catch(ArrayIndexOutOfBoundsException e){
                sendMessage(sender, "UDB Version "+VERSION);
                sendMessage(sender, "Report bugs at: https://github.com/jython234/UDB/issues");
                sendMessage(sender, "Type /udb help for a list of commands.");
                if(sender instanceof Player){
                    Player player = (Player) sender;
                    sendMessage(player, "You have "+ChatColor.GREEN+db.getDataByUUID(player.getUniqueId().toString()).getLives()+" lives.");
                    sendMessage(player, "Your survival record is "+ChatColor.GREEN+db.getDataByUUID(player.getUniqueId().toString()).printSurvivalRecord());
                }
            } finally {
                return true;
            }
        } else {
            return false;
        }
    }

    private void displayHelp(CommandSender sender) {
        sendMessage(sender, "/udb - Root command for UDB.");
        sendMessage(sender, "/udb setlives - Set a player's lives.");
        sendMessage(sender, "/udb bypass - Allows you to bypass banning.");
        sendMessage(sender, "Please report bugs at: https://github.com/jython234/UDB/issues");
    }

    public void sendMessage(CommandSender sendTo, String message){
        sendTo.sendMessage(ChatColor.GOLD+"["+ChatColor.AQUA+"UDB"+ChatColor.GOLD+"] "+ChatColor.YELLOW+message);
    }

    public PlayerDatabase getPlayerDatabase(){
        return db;
    }

    public int getTempBanTime(){
        return tempBanTime;
    }

    public int getStartLives(){
        return startLives;
    }
}
