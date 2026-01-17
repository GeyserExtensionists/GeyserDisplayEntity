package me.geyserextensionists.geyserdisplayentity.managers;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.util.FileConfiguration;
import me.geyserextensionists.geyserdisplayentity.util.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;

public class ConfigManager {

    private FileConfiguration config, lang;

    private HashMap<String, FileConfiguration> configMappingsCache;

    public ConfigManager() {
        load();
        loadConfigMappings();
    }

    public void load() {
        this.config = new FileConfiguration("config.yml");
        this.lang = new FileConfiguration("Lang/messages.yml");

        if (!Files.exists(GeyserDisplayEntity.getExtension().dataFolder().resolve("Mappings"))) FileUtils.createFiles(GeyserDisplayEntity.getExtension(), "Mappings/example.yml");

        loadConfigMappings();
    }

    private void loadConfigMappings() {
        HashMap<String, FileConfiguration> tempConfigMappingsCache = new HashMap<>();

        for (File file : FileUtils.getAllFiles(GeyserDisplayEntity.getExtension().dataFolder().resolve("Mappings").toFile(), ".yml")) {
            FileConfiguration mappingsConfigFile = new FileConfiguration("Mappings/" + file.getName());
            FileConfiguration mappingsConfig = mappingsConfigFile.getConfigurationSection("mappings");

            tempConfigMappingsCache.put(file.getName().replace(".yml", ""), mappingsConfig);
        }

        this.configMappingsCache = tempConfigMappingsCache;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLang() {
        return lang;
    }

    public HashMap<String, FileConfiguration> getConfigMappingsCache() {
        return configMappingsCache;
    }
}
