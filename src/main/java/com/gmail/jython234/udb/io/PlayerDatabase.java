package com.gmail.jython234.udb.io;

import com.gmail.jython234.udb.UDBPlugin;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Main database class for storing Player data.
 */
public class PlayerDatabase {
    private HashMap<String, PlayerData> data = new HashMap();
    private File dir;
    private UDBPlugin plugin;

    private PlayerDatabase(File dir, UDBPlugin plugin){
        this.dir = dir;
        this.plugin = plugin;
    }

    public static PlayerDatabase loadDatabase(File dbDir, UDBPlugin plugin) throws IOException {
        PlayerDatabase db = new PlayerDatabase(dbDir, plugin);
        if(!dbDir.exists()){
            db.createNew();
        } else {
            db.load();
        }
        return db;
    }

    private void createNew(){
        dir.mkdirs();
    }

    private void load() throws IOException {
        File[] files = dir.listFiles();
        for(File file: files){
            if(file.getName().endsWith(".pdb")){
                PlayerData data = PlayerData.load(file);
                addToDatabase(data);
            }
        }
    }

    public PlayerData getDataByUUID(String uuid){
        return data.get(uuid);
    }

    public void saveAll() throws IOException {
        for(PlayerData pd: data.values()){
            pd.save();
        }
    }

    public void displayInfo(CommandSender sender){
        List<File> files = listValidFiles();
        plugin.sendMessage(sender, "The database has "+ ChatColor.GREEN+files.size()+" valid database files.");
        plugin.sendMessage(sender, "Use /udb-db delete [file] to delete data files.");
        plugin.sendMessage(sender, "Use /udb save to save or /udb reload to reload.");
        plugin.sendMessage(sender, "Dump data for a player: /udb-db dump [player]");
    }

    public void dumpData(CommandSender sender, PlayerData data){
        plugin.sendMessage(sender, "Database dump for "+ChatColor.GREEN+data.getUUID()+".pdb");
        plugin.sendMessage(sender, "Lives: "+ChatColor.GREEN+data.getLives());
        plugin.sendMessage(sender, "isBanned: "+ChatColor.GREEN+Boolean.toString(data.isBanned()));
        plugin.sendMessage(sender, "Ban time: "+ChatColor.GREEN+data.getBanTime());
        plugin.sendMessage(sender, "Survival Record: "+ChatColor.GREEN+data.printSurvivalRecord());
        plugin.sendMessage(sender, "End Dump.");
    }

    public List<File> listValidFiles(){
        ArrayList<File> validFiles = new ArrayList<File>();
        for(File file: dir.listFiles()){
            if(file.getName().endsWith(".pdb")){
                validFiles.add(file);
            }
        }
        return validFiles;
    }

    public boolean isInDatabase(OfflinePlayer player){
        return data.containsKey(player.getUniqueId().toString());
    }

    public void createNewData(OfflinePlayer player) throws IOException {
        File location = new File(dir.getAbsolutePath()+File.separator+player.getUniqueId().toString()+".pdb");
        location.createNewFile();
        PlayerData data = new PlayerData(location);
        data.UUID = player.getUniqueId().toString();
        data.lives = plugin.getStartLives();
        data.isBanned = false;
        data.banTime = -1;
        data.survivalRecord = getCurrentTime();
        data.save();

        addToDatabase(data);
    }

    public void addToDatabase(PlayerData data){
        this.data.put(data.getUUID(), data);
    }

    public String getCurrentTime(){
        return Long.toString(System.currentTimeMillis());
    }

    public static class PlayerData{
        private File file;
        private String UUID;
        private int lives;
        private boolean isBanned;
        private long banTime;
        private String survivalRecord;
        private boolean isBypassing = false;

        public PlayerData(File file) { this.file = file; }

        public static PlayerData load(File file) throws IOException {
            PlayerData pd = new PlayerDatabase.PlayerData(file);
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            short idLen = in.readShort();
            byte[] idBytes = new byte[idLen];
            in.read(idBytes);
            pd.UUID = new String(idBytes, "UTF-8");

            pd.lives = in.readInt();
            pd.isBanned = in.readBoolean();
            pd.banTime = in.readLong();

            short strLen = in.readShort();
            byte[] recordBytes = new byte[strLen];
            in.read(recordBytes);
            pd.survivalRecord = new String(recordBytes, "UTF-8");

            in.close();
            return pd;
        }

        public String getUUID(){
            return UUID;
        }

        public int getLives(){
            return lives;
        }

        public boolean isBanned(){
            return isBanned;
        }

        public long getBanTime(){
            return banTime;
        }

        public String getSurvivalRecord(){
            return survivalRecord;
        }

        public File getFile(){
            return file;
        }

        public void setUUID(String uuid){
            UUID = uuid;
        }

        public void setLives(int lives){
            this.lives = lives;
        }

        public void setBanned(boolean banned){
            this.isBanned = banned;
        }

        public void setBanTime(long banTime){
            this.banTime = banTime;
        }

        public void setSurvivalRecord(String survivalRecord){
            this.survivalRecord = survivalRecord;
        }

        public String printSurvivalRecord(){
            long time = Long.parseLong(survivalRecord);
            long elapsed = System.currentTimeMillis() - time;
            int sec = (int) (elapsed / 1000);
            int min = sec / 60;
            int hours = min / 60;
            int days = hours / 24;
            int weeks = days / 7;

            min = min - (hours * 60);
            hours = hours - (days * 24);
            days = days - (weeks * 7);

            return weeks +" weeks, "+days+" days, "+hours+" hours, "+min+" minutes";
        }

        public void save() throws IOException{
            DataOutputStream out = new DataOutputStream(new FileOutputStream(file));

            out.writeShort(UUID.length());
            out.write(UUID.getBytes("UTF-8"));

            out.writeInt(lives);
            out.writeBoolean(isBanned);
            out.writeLong(banTime);

            out.writeShort(survivalRecord.length());
            out.write(survivalRecord.getBytes("UTF-8"));

            out.close();
        }

        public boolean isBypassing() {
            return isBypassing;
        }

        public void setBypassing(boolean isBypassing) {
            this.isBypassing = isBypassing;
        }
    }
}
