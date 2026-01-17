package me.geyserextensionists.geyserdisplayentity.util;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FileConfiguration {

    private final GeyserDisplayEntity extension = GeyserDisplayEntity.getExtension();

    private final Path dataDirectory = extension.dataFolder();

    protected final String configFile;
    private final CommentedConfigurationNode configurationNode;

    public FileConfiguration(String configFile) {
        this.configFile = configFile;
        this.configurationNode = load(configFile);
    }

    public FileConfiguration(CommentedConfigurationNode configurationNode, String configFile) {
        this.configFile = configFile;
        this.configurationNode = configurationNode;
    }

    private CommentedConfigurationNode load(String fileName) {
        try {
            FileUtils.createFiles(extension, fileName);

            Path config = this.dataDirectory.resolve(fileName);
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder().path(config).build();

            return loader.load();
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    public FileConfiguration getConfigurationSection(String path) {
        CommentedConfigurationNode sectionNode = getConfigurationSectionNode(path);
        if (sectionNode == null || sectionNode.virtual()) return null;
        return new FileConfiguration(sectionNode, this.configFile);
    }

    public String getString(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null) return null;
        return node.getString();
    }

    public List<String> getStringList(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null || node.virtual()) return List.of();

        try {
            return node.getList(String.class, List.of());
        } catch (SerializationException err) {
            throw new RuntimeException(err);
        }
    }

    public int getInt(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null) return 0;
        return node.getInt();
    }

    public List<Integer> getIntegerList(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null || node.virtual()) return List.of();

        try {
            return node.getList(Integer.class, List.of());
        } catch (SerializationException err) {
            throw new RuntimeException(err);
        }
    }

    public double getDouble(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null) return 0;
        return node.getDouble();
    }

    public Long getLong(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null) return 0L;
        return node.getLong();
    }

    public List<Long> getLongList(String path) {
        CommentedConfigurationNode node = getInternal(path);
        if (node == null || node.virtual()) return List.of();

        try {
            return node.getList(Long.class, List.of());
        } catch (SerializationException err) {
            throw new RuntimeException(err);
        }
    }

    public boolean getBoolean(String path) {
         CommentedConfigurationNode node = getInternal(path);
        if (node == null) return false;
        return node.getBoolean();
    }

    public boolean isBoolean(String path) {
        CommentedConfigurationNode node = getInternal(path);
        return node != null && node.raw() instanceof Boolean;
    }

    public boolean contains(String path) {
        return getInternal(path) != null;
    }

    private CommentedConfigurationNode getInternal(String path) {
        CommentedConfigurationNode node = toSplitRoot(path, this.configurationNode);
        if (node.virtual()) return null;
        return node;
    }

    private CommentedConfigurationNode toSplitRoot(String path, CommentedConfigurationNode node) {
        if (path == null) return node;
        path = path.startsWith(".") ? path.substring(1) : path;
        return node.node(path.contains(".") ? path.split("\\.") : new Object[]{path});
    }

    private CommentedConfigurationNode getConfigurationSectionNode(String path) {
        return getInternal(path);
    }

    public CommentedConfigurationNode getRootNode() {
        return configurationNode;
    }
}
