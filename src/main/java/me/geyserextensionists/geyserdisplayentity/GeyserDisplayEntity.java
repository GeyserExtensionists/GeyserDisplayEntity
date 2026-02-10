package me.geyserextensionists.geyserdisplayentity;

import me.geyserextensionists.geyserdisplayentity.entity.BlockDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.ItemDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.SlotDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.managers.ConfigManager;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.properties.GeyserEntityProperties;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.ArrayList;
import java.util.List;

// From Kastle's Geyser branch
public class GeyserDisplayEntity implements Extension {

    private static GeyserDisplayEntity extension;

    private ConfigManager configManager;

    private static EntityDefinition<ItemDisplayEntity> ITEM_DISPLAY;
    private static EntityDefinition<BlockDisplayEntity> BLOCK_DISPLAY;

    public static final Integer MAX_VALUE = 1000000;
    public static final Integer MIN_VALUE = -1000000;

    @Subscribe
    public void onLoad(GeyserPreInitializeEvent event) {
        extension = this;
        loadManagers();
    }

    @Subscribe
    public void onEntityPropertiesEvent(GeyserDefineEntityPropertiesEvent event) {
        try {
            EntityDefinition<Entity> entityBase = EntityDefinition.builder(Entity::new)
                    .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                    .addTranslator(MetadataTypes.INT, Entity::setAir) // Air/bubbles
                    .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setCustomName)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setCustomNameVisible)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
                    .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.INT, Entity::setFreezing)
                    .build();

            GeyserEntityProperties.Builder displayPropBuilder = new GeyserEntityProperties.Builder("geyser:item_display")
                    .add(new IntProperty(Identifier.of("geyser:s_id"), MAX_VALUE, MIN_VALUE, 0))
                    .add(new IntProperty(Identifier.of("geyser:s_int"), MAX_VALUE, MIN_VALUE, 0))
                    .add(new FloatProperty(Identifier.of("geyser:i_dur"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:r_x"), 180F, -180F, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:r_y"), 180F, -180F, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:r_z"), 180F, -180F, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:t_x"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:t_y"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:t_z"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:s_x"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:s_y"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:s_z"), MAX_VALUE, MIN_VALUE, 0F))
                    .add(new FloatProperty(Identifier.of("geyser:s_q"), MAX_VALUE, MIN_VALUE, 0F));
            EntityDefinition<SlotDisplayEntity> slotDisplayBase = EntityDefinition.inherited(SlotDisplayEntity::new, entityBase)
                    .addTranslator(null) // Interpolation start ticks
                    .addTranslator(null) // Interpolation duration ID
                    .addTranslator(null) // Position/Rotation interpolation duration
                    .addTranslator(MetadataTypes.VECTOR3, SlotDisplayEntity::setTranslation) // Translation
                    .addTranslator(MetadataTypes.VECTOR3, SlotDisplayEntity::setScale) // Scale
                    .addTranslator(MetadataTypes.QUATERNION, SlotDisplayEntity::setLeftRotation) // Left rotation
                    .addTranslator(MetadataTypes.QUATERNION, SlotDisplayEntity::setRightRotation) // Right rotation
                    .addTranslator(null) // Billboard render constraints
                    .addTranslator(null) // Brightness override
                    .addTranslator(null) // View range
                    .addTranslator(null) // Shadow radius
                    .addTranslator(null) // Shadow strength
                    .addTranslator(null) // Width
                    .addTranslator(null) // Height
                    .addTranslator(null) // Glow color override
                    .build();

            BLOCK_DISPLAY = EntityDefinition.inherited(BlockDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.BLOCK_DISPLAY)
                    .height(configManager.getConfig().getInt("general.height")).width(0.2f)
                    .propertiesBuilder(displayPropBuilder)
                    .identifier("geyser:block_display")
                    .addTranslator(MetadataTypes.BLOCK_STATE, BlockDisplayEntity::setDisplayedBlockState)
                    .build();

            ITEM_DISPLAY = EntityDefinition.inherited(ItemDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.ITEM_DISPLAY)
                    .height(configManager.getConfig().getInt("general.height")).width(0.2f)
                    .propertiesBuilder(displayPropBuilder)
                    .identifier("geyser:item_display")
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemDisplayEntity::setDisplayedItem)
                    .addTranslator(MetadataTypes.BYTE, ItemDisplayEntity::setDisplayType)
                    .build();

            Registries.ENTITY_DEFINITIONS.register(EntityType.BLOCK_DISPLAY, BLOCK_DISPLAY);
            Registries.ENTITY_DEFINITIONS.register(EntityType.ITEM_DISPLAY, ITEM_DISPLAY);
            registerIdentifier(ITEM_DISPLAY.identifier());
            registerIdentifier(BLOCK_DISPLAY.identifier());

        } catch (Throwable err) {
            logger().error("Error in load", err);
        }

        logger().info("Done");
    }

    @Subscribe
    public void onDefineCommand(GeyserDefineCommandsEvent event) {
        event.register(Command.builder(this)
                .name("reload")
                .source(CommandSource.class)
                .playerOnly(false)
                .description("GeyserDisplayEntity Reload Command")
                .permission("geyserdisplayentity.commands.reload")
                .executor((source, command, args) -> {
                    configManager.load();
                    source.sendMessage(configManager.getLang().getString("commands.geyserdisplayentity.reload.successfully-reloaded"));
                })
                .build());
    }

    public void registerIdentifier(String id) {
        NbtMap registry = Registries.BEDROCK_ENTITY_IDENTIFIERS.get();
        List<NbtMap> idList = new ArrayList<>(registry.getList("idlist", NbtType.COMPOUND));
        idList.add(NbtMap.builder()
                .putString("id", id)
                .putString("bid", "")
                .putBoolean("hasspawnegg", false)
                .putInt("rid", idList.size() + 1)
                .putBoolean("summonable", false).build()
        );

        Registries.BEDROCK_ENTITY_IDENTIFIERS.set(NbtMap.builder().putList("idlist", NbtType.COMPOUND, idList).build());
    }

    private void loadManagers() {
        this.configManager = new ConfigManager();
    }

    public static GeyserDisplayEntity getExtension() {
        return extension;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
