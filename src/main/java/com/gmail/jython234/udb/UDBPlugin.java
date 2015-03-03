package com.gmail.jython234.udb;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * UDB Plugin Main class.
 */
public class UDBPlugin extends JavaPlugin{
    private int tempBanTime; //Temp ban time is in hours

    @Override
    public void onEnable(){
        loadSettings();
        getLogger().info("UDB Enabled!");
    }

    private void loadSettings() {
        saveDefaultConfig();
    }

    @Override
    public void onDisable(){
        getLogger().info("UDB Disabled!");
    }
}
