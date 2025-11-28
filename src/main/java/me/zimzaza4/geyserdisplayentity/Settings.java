package me.zimzaza4.geyserdisplayentity;

import net.elytrium.commons.config.YamlConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Settings extends YamlConfig {
    @Ignore
    public static final Settings IMP = new Settings();

    @Comment("General display options")
    @Create
    public DisplayEntityOptions GENERAL;

    @Comment("Invisible item types")
    public List<String> HIDE_TYPES = List.of("minecraft:leather_horse_armor", "minecraft:bone");

    @Comment("Mappings")
    public Map<String, DisplayEntityMapping> MAPPINGS = new HashMap<>(Map.of("example", new DisplayEntityMapping()));

    public static class DisplayEntityMapping {
        @Comment("Item type")
        public String TYPE = "minecraft:iron_sword";
        @Comment("Custom model data, -1 represent ignore custom model data")
        public int MODEL_DATA = -1;
        @Comment("options")
        @Create
        public DisplayEntityOptions OPTIONS;
    }


    public static class DisplayEntityOptions {
        @Comment("The y-offset of display entity")
        public double Y_OFFSET = -0.5;
        @Comment("Use entity scale, instead of entity properties")
        public boolean VANILLA_SCALE = true;
        @Comment("Vanilla scale multiplier")
        public double VANILLA_SCALE_MULTIPLIER = 1.0;
        @Comment("Equip custom items (items with custom mapping) to hand")
        public boolean HAND = false;
    }

}
