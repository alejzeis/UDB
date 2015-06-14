package com.gmail.jython234.udb;

import com.gmail.jython234.udb.io.PlayerDatabase;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * UDB Plugin Main class.
 */
public class UDBPlugin extends JavaPlugin{
    public final static String VERSION = "1.1.2-RC1";
    private int tempBanTime; //Temp ban time is in minutes
    private int startLives; //Amount of lives new players start with.

    private PlayerDatabase db;
    private Economy economy = null;
    private Chat chat = null;

    @Override
    public void onEnable(){
        try {
            loadSettings();
        } catch(Exception e){
            getLogger().severe("Failed to load settings, Exception: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
        linkVault();
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

    private void linkVault() {
        if(getServer().getPluginManager().getPlugin("Vault") != null){
            getLogger().info("Vault found, linking...");
            RegisteredServiceProvider<Economy> econRSP = getServer().getServicesManager().getRegistration(Economy.class);
            RegisteredServiceProvider<Chat> chatRSP = getServer().getServicesManager().getRegistration(Chat.class);
            economy = econRSP.getProvider();
            chat = chatRSP.getProvider();
            getLogger().info("Link complete, all economy and chat support is now enabled.");
        } else {
            getLogger().warning("Vault was not found. All economy and chat support will be disabled.");
        }
    }

    private void loadSettings() {
        getDataFolder().mkdirs();
        saveDefaultConfig();
        if(getConfig().getString("lives.banTimeType").equalsIgnoreCase("hours")){
            tempBanTime = getConfig().getInt("lives.banTime") * 60;
        } else if(getConfig().getString("lives.banTimeType").equalsIgnoreCase("minutes")) {
            tempBanTime = getConfig().getInt("lives.banTime");
        } else {
            getLogger().severe("Ban Time Type is not supported!");
            getServer().getPluginManager().disablePlugin(this);
        }
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
        //UDB Commands
        if(lbl.equalsIgnoreCase("udb")) {
            try {
                String operation = args[0];
                if (operation.equalsIgnoreCase("help")) {
                    displayHelp(sender);
                } else if (operation.equalsIgnoreCase("setlives")) {
                    if (sender.hasPermission("udb.setlives")) {
                        if (args.length > 1) {
                            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                            if (db.isInDatabase(player)) {
                                try {
                                    db.getDataByUUID(player.getUniqueId().toString()).setLives(Integer.parseInt(args[2]));
                                    sendMessage(sender, "Set " + ChatColor.GREEN + args[1] + "/" + player.getUniqueId().toString() + "'s lives to " + args[2]);
                                    if (isPlayerOnline(player)) {
                                        Player player1 = (Player) player;
                                        sendMessage(player1, "Your lives have been set to " + ChatColor.GREEN + args[2]);
                                    }
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
                        sendMessage(sender, ChatColor.RED + "You must have the udb.setlives permission.");
                    }
                } else if (operation.equalsIgnoreCase("addlives")) {
                    if (sender.hasPermission("udb.setlives")) {
                        if (args.length > 1) {
                            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                            if (db.isInDatabase(player)) {
                                try {
                                    PlayerDatabase.PlayerData data = db.getDataByUUID(player.getUniqueId().toString());
                                    data.setLives((data.getLives() + Integer.parseInt(args[2])));
                                    sendMessage(sender, "Set " + ChatColor.GREEN + args[1] + "/" + player.getUniqueId().toString() + "'s lives to " + (data.getLives() + Integer.parseInt(args[2])));
                                    if (isPlayerOnline(player)) {
                                        Player player1 = (Player) player;
                                        sendMessage(player1, "Your lives have been set to " + ChatColor.GREEN + (data.getLives() + Integer.parseInt(args[2])));
                                    }
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
                        sendMessage(sender, ChatColor.RED + "You must have the udb.setlives permission.");
                    }
                } else if (operation.equalsIgnoreCase("unban") || operation.equalsIgnoreCase("revive")) {
                    if (args.length > 1) {
                        if (sender.hasPermission("udb.unban")) {
                            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                            if (db.isInDatabase(player)) {
                                PlayerDatabase.PlayerData data = db.getDataByUUID(player.getUniqueId().toString());
                                data.setBanned(false);
                                data.setLives(1);
                                data.setBanTime(-1);
                                data.save();

                                sendMessage(sender, "Unbanned user " + player.getName() + "/" + player.getUniqueId().toString() + " and set lives to 1.");
                            } else {
                                sendMessage(sender, ChatColor.RED + args[1] + " is not in UDB's database.");
                            }
                        } else {
                            sendMessage(sender, ChatColor.RED + "You must have the udb.unban permission!");
                        }
                    } else {
                        sendMessage(sender, "Usage: /udb unban [player] or /udb revive [player]");
                    }
                } else if (operation.equalsIgnoreCase("save")) {
                    if (sender.hasPermission("udb.db.save")) {
                        sendMessage(sender, "Saving database...");
                        db.saveAll();
                        sendMessage(sender, "Save complete.");
                    } else {
                        sendMessage(sender, ChatColor.RED + "You must have the udb.db.save permission.");
                    }
                } else if (operation.equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("udb.db.reload")) {
                        sendMessage(sender, "Reloading database (All changes will be lost!)");
                        db = null;
                        db = PlayerDatabase.loadDatabase(new File(getDataFolder().getAbsolutePath() + File.separator + "playerDB"), this);
                        sendMessage(sender, "Load complete.");
                    } else {
                        sendMessage(sender, ChatColor.RED + "You must have the udb.db.save permission.");
                    }
                } else if(operation.equalsIgnoreCase("bypass")) {
                    if (sender.hasPermission("udb.bypass")) {
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            if (db.isInDatabase(player)) {
                                PlayerDatabase.PlayerData data = db.getDataByUUID(player.getUniqueId().toString());
                                if (data.isBypassing()) {
                                    data.setBypassing(false);
                                    sendMessage(player, "You are not bypassing anymore.");
                                } else {
                                    data.setBypassing(true);
                                    sendMessage(player, "You are now bypassing.");
                                }
                            } else {
                                sendMessage(player, "You are not in the database (try reconnecting).");
                            }
                        } else {
                            sendMessage(sender, "This command can only be used as a player.");
                        }
                    } else {
                        sendMessage(sender, "You do not have the udb.bypass permission.");
                    }
                } else if(operation.equalsIgnoreCase("whois")){
                    if(args.length > 0){
                        OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                        if(player != null){
                            if(db.isInDatabase(player)) {
                                PlayerDatabase.PlayerData data = db.getDataByUUID(player.getUniqueId().toString());
                                sendMessage(sender, "WHOIS: "+args[1]);
                                sendMessage(sender, "IsOnline: "+isPlayerOnline(player));
                                if(isPlayerOnline(player)) {
                                    sendMessage(sender, "IP: " +getServer().getPlayer(player.getUniqueId().toString()).getAddress().getHostName());
                                }
                                sendMessage(sender, "Lives: "+data.getLives());
                                sendMessage(sender, "Ban time: "+data.getBanTime());
                                sendMessage(sender, "Survival Record: "+data.printSurvivalRecord());
                            } else {
                                sendMessage(sender, args[1]+" is not in the database.");
                            }
                        } else {
                            sendMessage(sender, args[1]+" has never been on this server.");
                        }
                    } else {
                        sendMessage(sender, "Usage: /udb whois [player]");
                    }
                } else {
                    sendMessage(sender, "Usage: /udb [operation]");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                sendMessage(sender, "UDB Version " + VERSION + ", written by " + ChatColor.GREEN + "jython234.");
                sendMessage(sender, "Report bugs at: https://github.com/jython234/UDB/issues");
                sendMessage(sender, "Type /udb help for a list of commands.");
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    sendMessage(player, "You have " + ChatColor.GREEN + db.getDataByUUID(player.getUniqueId().toString()).getLives() + " lives.");
                    sendMessage(player, "Your survival record is " + ChatColor.GREEN + db.getDataByUUID(player.getUniqueId().toString()).printSurvivalRecord());
                }
            } finally {
                return true;
            }
        //UDB-DB commands
        } else if(lbl.equalsIgnoreCase("udb-db") || lbl.equalsIgnoreCase("db")){
            if(sender.hasPermission("udb.db.manage")) {
                try {
                    String operation = args[0];
                    if(operation.equalsIgnoreCase("dump")){
                        if(args.length > 0){
                            OfflinePlayer player = getServer().getOfflinePlayer(args[1]);
                            if(db.isInDatabase(player)){
                                db.dumpData(sender, db.getDataByUUID(player.getUniqueId().toString()));
                            } else {
                                sendMessage(sender, player.getName()+" is not in the UDB database.");
                            }
                        } else {
                            sendMessage(sender, "Usage: /udb-db dump [player]");
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    db.displayInfo(sender);
                }
            } else {
                sendMessage(sender, ChatColor.RED+"You don't have the udb.db.manage permission!");
            }
            return true;
        } else {
            return false;
        }
    }

    private void displayHelp(CommandSender sender) {
        sendMessage(sender, "/udb - Root command for UDB.");
        sendMessage(sender, "/udb setlives - Set a player's lives.");
        sendMessage(sender, "/udb unban - Unban a player.");
        sendMessage(sender, "/udb bypass - Allows you to bypass banning.");
        sendMessage(sender, "Please report bugs at: https://github.com/jython234/UDB/issues");
    }

    public void sendMessage(CommandSender sendTo, String message){
        sendTo.sendMessage(ChatColor.GOLD+"["+ChatColor.AQUA+"UDB"+ChatColor.GOLD+"] "+ChatColor.YELLOW+message);
    }

    public boolean isPlayerOnline(OfflinePlayer player){
        for(Player onlinePlayer: getServer().getOnlinePlayers()){
            if(onlinePlayer.getUniqueId().toString().equals(player.getUniqueId().toString())){
                return true;
            }
        }
        return false;
    }

    public boolean supportsVault(){
        return (economy != null) && (chat != null);
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
