package me.geyserextensionists.geyserdisplayentity.managers;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.util.FileConfiguration;
import me.geyserextensionists.geyserdisplayentity.util.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class ConfigManager {

    private FileConfiguration config, lang;

    private LinkedHashMap<String, FileConfiguration> configMappingsCache;

    private Set<String> hideTypes = Set.of();
    private Set<String> hideCustomTypes = Set.of();
    private boolean hideUnmappedVanilla = true;
    private boolean logDisplays = false;

    public ConfigManager() {
        load();
    }

    public void load() {
        this.config = new FileConfiguration("config.yml");
        this.lang = new FileConfiguration("Lang/messages.yml");

        this.hideTypes = new HashSet<>(config.getStringList("hide-types"));
        this.hideCustomTypes = config.contains("hide-custom-types")
                ? new HashSet<>(config.getStringList("hide-custom-types"))
                : this.hideTypes;
        this.hideUnmappedVanilla = !config.contains("hide-unmapped-vanilla-displays")
                || config.getBoolean("hide-unmapped-vanilla-displays");
        this.logDisplays = config.getBoolean("settings.debug.log-displays");

        if (!Files.exists(GeyserDisplayEntity.getExtension().dataFolder().resolve("Mappings"))) FileUtils.createFiles(GeyserDisplayEntity.getExtension(), "Mappings/example.yml");

        loadConfigMappings();
    }

    private void loadConfigMappings() {
        LinkedHashMap<String, FileConfiguration> tempConfigMappingsCache = new LinkedHashMap<>();
        List<File> mappingFiles = new ArrayList<>(FileUtils.getAllFiles(GeyserDisplayEntity.getExtension().dataFolder().resolve("Mappings").toFile(), ".yml"));
        mappingFiles.sort(Comparator.comparing(File::getAbsolutePath, String.CASE_INSENSITIVE_ORDER));

        for (File file : mappingFiles) {
            FileConfiguration mappingsConfigFile = new FileConfiguration("Mappings/" + file.getName());
            FileConfiguration mappingsConfig = mappingsConfigFile.getConfigurationSection("mappings");
            if (mappingsConfig == null) continue;

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

    public LinkedHashMap<String, FileConfiguration> getConfigMappingsCache() {
        return configMappingsCache;
    }

    public Set<String> getHideTypes() {
        return hideTypes;
    }

    public Set<String> getHideCustomTypes() {
        return hideCustomTypes;
    }

    public boolean isHideUnmappedVanilla() {
        return hideUnmappedVanilla;
    }

    public boolean isLogDisplays() {
        return logDisplays;
    }
}
