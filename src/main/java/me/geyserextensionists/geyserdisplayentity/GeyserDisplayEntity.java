package me.geyserextensionists.geyserdisplayentity;

import me.geyserextensionists.geyserdisplayentity.entity.BlockDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.ItemDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.entity.SlotDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.managers.ConfigManager;
import me.geyserextensionists.geyserdisplayentity.util.EntityUtils;
import me.geyserextensionists.geyserdisplayentity.util.FileConfiguration;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.*;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Collection;

public class GeyserDisplayEntity implements Extension {

    private static GeyserDisplayEntity extension;

    private ConfigManager configManager;

    private static BedrockEntityDefinition ITEM_DISPLAY_BEDROCK;
    private static BedrockEntityDefinition BLOCK_DISPLAY_BEDROCK;

    private static VanillaEntityType<ItemDisplayEntity> ITEM_DISPLAY;
    private static VanillaEntityType<BlockDisplayEntity> BLOCK_DISPLAY;

    public static final Integer MAX_VALUE = 1000000;
    public static final Integer MIN_VALUE = -1000000;

    @Subscribe
    public void onLoad(GeyserPreInitializeEvent event) {
        extension = this;
        loadManagers();
    }

    @Subscribe
    public void onDefineEntities(GeyserDefineEntitiesEvent event) {
        //TODO loop through entity type hashmap

        ITEM_DISPLAY_BEDROCK = EntityUtils.findOrRegisterCustomDefinition(this, event, Identifier.of("geyser:item_display"));
        BLOCK_DISPLAY_BEDROCK = EntityUtils.findOrRegisterCustomDefinition(this, event, Identifier.of("geyser:block_display"));
    }

    @Subscribe
    public void onEntityPropertiesEvent(GeyserDefineEntityPropertiesEvent event) {
        try {
            registerDisplayProperties(event, Identifier.of("geyser:item_display"));
            registerDisplayProperties(event, Identifier.of("geyser:block_display"));

            EntityTypeBase<Entity> entityBase = EntityTypeDefinition.baseBuilder(Entity.class)
                    .addTranslator(MetadataTypes.BYTE, Entity::setFlags)
                    .addTranslator(MetadataTypes.INT, Entity::setAir) // Air/bubbles
                    .addTranslator(MetadataTypes.OPTIONAL_COMPONENT, Entity::setCustomName)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setCustomNameVisible)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setSilent)
                    .addTranslator(MetadataTypes.BOOLEAN, Entity::setGravity)
                    .addTranslator(MetadataTypes.POSE, (entity, entityMetadata) -> entity.setPose(entityMetadata.getValue()))
                    .addTranslator(MetadataTypes.INT, Entity::setFreezing)
                    .build();

            EntityTypeBase<SlotDisplayEntity> slotDisplayBase = EntityTypeBase.baseInherited(SlotDisplayEntity.class, entityBase)
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

            BLOCK_DISPLAY = VanillaEntityType.inherited(BlockDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.BLOCK_DISPLAY)
                    .height(configManager.getConfig().getFloat("general.height")).width(0.001f)
                    .bedrockDefinition(BLOCK_DISPLAY_BEDROCK)
                    .addTranslator(MetadataTypes.BLOCK_STATE, BlockDisplayEntity::setDisplayedBlockState)
                    .build();

            ITEM_DISPLAY = VanillaEntityType.inherited(ItemDisplayEntity::new, slotDisplayBase)
                    .type(EntityType.ITEM_DISPLAY)
                    .height(configManager.getConfig().getFloat("general.height")).width(0.001f)
                    .bedrockDefinition(ITEM_DISPLAY_BEDROCK)
                    .addTranslator(MetadataTypes.ITEM_STACK, ItemDisplayEntity::setDisplayedItem)
                    .addTranslator(MetadataTypes.BYTE, ItemDisplayEntity::setDisplayType)
                    .build();

            EntityUtils.replaceJavaDefinition(EntityType.BLOCK_DISPLAY, BLOCK_DISPLAY);
            EntityUtils.replaceJavaDefinition(EntityType.ITEM_DISPLAY, ITEM_DISPLAY);
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

    private void registerDisplayProperties(GeyserDefineEntityPropertiesEvent event, Identifier entityIdentifier) {
        Collection<GeyserEntityProperty<?>> existing = event.properties(entityIdentifier);

        FileConfiguration entityConfig = configManager.getEntityTypesCache().get(entityIdentifier);
        for (Object entityKey : entityConfig.getConfigurationSection("properties").getRootNode().childrenMap().keySet()) {
            String entityString = entityKey.toString();
            FileConfiguration propertyConfig = entityConfig.getConfigurationSection("properties." + entityString);

            String propertyType = propertyConfig.getString("property-type");

            if (propertyType.equals("integer")) {
                EntityUtils.registerInteger(event, existing, entityIdentifier, propertyConfig.getString("id"), propertyConfig.getInt("min-value"), propertyConfig.getInt("max-value"), propertyConfig.getInt("default-value"));
            } else if (propertyType.equals("float")) {
                EntityUtils.registerFloat(event, existing, entityIdentifier, propertyConfig.getString("id"), propertyConfig.getInt("min-value"), propertyConfig.getInt("max-value"), propertyConfig.getFloat("default-value"));
            }
        }
    }

    private void loadManagers() {
        this.configManager = new ConfigManager(this);
    }

    public static GeyserDisplayEntity getExtension() {
        return extension;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
