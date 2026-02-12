package me.geyserextensionists.geyserdisplayentity.entity;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import me.geyserextensionists.geyserdisplayentity.type.DisplayType;
import me.geyserextensionists.geyserdisplayentity.util.DeltaUtils;
import me.geyserextensionists.geyserdisplayentity.util.FileConfiguration;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.item.type.DyeableArmorItem;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.item.ItemTranslator;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.CustomModelData;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;

import java.util.Arrays;
import java.util.List;

public class ItemDisplayEntity extends SlotDisplayEntity {

    private DisplayType displayType = DisplayType.NONE;
    private Byte color;
    private boolean custom = false;
    private boolean needHide = false;
    private double lastOffset = 0;

    public ItemDisplayEntity(EntitySpawnContext entitySpawnContext) {
        super(entitySpawnContext);
    }

    public void setOffset(double offset) {
        moveRelative(0, offset - lastOffset, 0, yaw, pitch, headYaw, false);
        this.lastOffset = offset;
    }

    public void setDisplayedItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemStack stack = entityMetadata.getValue();
        if (stack == null) {
            this.item = ItemData.AIR;
            setInvisible(true);
            return;
        }

        // Reset config to general defaults so unmapped items always have valid display options
        config = GeyserDisplayEntity.getExtension().getConfigManager().getConfig().getConfigurationSection("general");

        ItemData item = ItemTranslator.translateToBedrock(session, stack);
        this.item = item;

        if (item instanceof DyeableArmorItem) {
            DataComponents data = stack.getDataComponentsPatch();
            if (data != null) {
                Integer dyed = data.get(DataComponentTypes.DYED_COLOR);
                if (dyed != null) {
                    this.color = Byte.parseByte(String.valueOf(getColor(dyed)));
                }
            }
        }

        String type = session.getItemMappings().getMapping(stack.getId()).getJavaItem().javaIdentifier();

        boolean mappingApplied = false;

        for (FileConfiguration mappingsConfig : GeyserDisplayEntity.getExtension().getConfigManager().getConfigMappingsCache().values()) {
            if (mappingsConfig == null) continue;

            for (Object mappingKey : mappingsConfig.getRootNode().childrenMap().keySet()) {
                String mappingString = mappingKey.toString();

                FileConfiguration mappingConfig = mappingsConfig.getConfigurationSection(mappingString);
                if (mappingConfig == null) continue;
                String mappingType = mappingConfig.getString("type");
                if (mappingType == null || !mappingType.trim().equals(type)) continue;

                boolean applied;
                if (shouldUseLegacyMapping(mappingConfig)) {
                    applied = applyLegacyModelData(stack, mappingConfig);
                } else {
                    applied = applyModernItemModels(item, mappingConfig);
                }

                if (applied) {
                    mappingApplied = true;
                    break;
                }
            }

            if (mappingApplied) break;
        }

        if (!item.getDefinition().getIdentifier().startsWith("minecraft:")) {
            custom = true;
            if (color != null) {
                getDirtyMetadata().put(EntityDataTypes.COLOR, color);
            }
        } else {
            custom = false;
        }

        // HIDE_TYPES check only if stack is present (it is)
        String javaID = session.getItemMappings().getMapping(stack).getJavaItem().javaIdentifier();
        boolean wasHiddenByType = needHide;

        // Keep hide-types behavior for vanilla items, but allow custom translated items
        // (e.g. custom model data on minecraft:bone) to remain visible unless explicitly forced.
        FileConfiguration rootConfig = GeyserDisplayEntity.getExtension().getConfigManager().getConfig();
        boolean hiddenByType = rootConfig.getStringList("hide-types").contains(javaID);
        boolean forceHiddenCustomType = rootConfig.getStringList("hide-custom-types").contains(javaID);

        if ((hiddenByType && !custom) || forceHiddenCustomType) {
            setInvisible(true);
            needHide = true;
            this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        } else {
            needHide = false;
            if (wasHiddenByType) {
                setInvisible(false);

                if (config != null && config.getBoolean("vanilla-scale")) {
                    applyScale();
                } else {
                    this.dirtyMetadata.put(EntityDataTypes.SCALE, 1f);
                }
            }
        }

        updateMainHand(session);
    }

    private boolean shouldUseLegacyMapping(FileConfiguration mappingConfig) {
        // Type + options only is a wildcard mapping; treat as modern-style default.
        if (!mappingConfig.contains("item-identifier") && !mappingConfig.contains("model-data")) {
            return false;
        }

        if (mappingConfig.contains("item-identifier")) return false;
        if (mappingConfig.contains("model-data")) return true;

        FileConfiguration rootConfig = GeyserDisplayEntity.getExtension().getConfigManager().getConfig();
        if (rootConfig.isBoolean("general.use-legacy-models")) {
            return rootConfig.getBoolean("general.use-legacy-models");
        }

        return false;
    }

    private boolean applyLegacyModelData(ItemStack item, FileConfiguration mappingConfig) {
        CustomModelData modelData = null;
        DataComponents components = item.getDataComponentsPatch();

        if (components != null) modelData = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);

        if (!mappingConfig.contains("model-data")) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        if (mappingConfig.getInt("model-data") == -1) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        if (modelData == null) return false;

        List<Float> modelDataFloats = modelData.floats();
        if (modelDataFloats == null || modelDataFloats.isEmpty()) return false;

        if (Math.abs(mappingConfig.getInt("model-data") - modelDataFloats.get(0)) < 0.5) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        return false;
    }

    private boolean applyModernItemModels(ItemData itemData, FileConfiguration mappingConfig) {
        String itemBedrockIdentifier = itemData.getDefinition().getIdentifier().replace("geyser_custom:", "");
        String mappingIdentifier = mappingConfig.getString("item-identifier");

        if (mappingIdentifier == null) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        String normalizedIdentifier = mappingIdentifier.trim();
        if (normalizedIdentifier.isEmpty() || normalizedIdentifier.equalsIgnoreCase("none")) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        if (normalizedIdentifier.equalsIgnoreCase(itemBedrockIdentifier)) {
            return entityApplyDisplayConfig(mappingConfig);
        }

        return false;
    }

    private boolean entityApplyDisplayConfig(FileConfiguration mappingConfig) {
        FileConfiguration mappingOptions = mappingConfig.getConfigurationSection("displayentityoptions");
        if (mappingOptions == null) {
            // Legacy compatibility for pre-migration config.yml format
            mappingOptions = mappingConfig.getConfigurationSection("options");
        }

        if (mappingOptions != null) {
            config = mappingOptions;
        } else if (config == null) {
            return false;
        }

        setOffset(config.getDouble("y-offset"));
        if (config.getBoolean("vanilla-scale")) applyScale();
        return true;
    }

    @Override
    protected void applyScale() {
        if (needHide) {
            dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        } else {
            super.applyScale();
        }
    }

    @Override
    public void updateMainHand(GeyserSession session) {
        if (!valid) return;

        ItemData helmet = ItemData.AIR; // TODO
        ItemData chest = item;

        if (custom && (config == null || !config.getBoolean("hand"))) {
            MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
            armorEquipmentPacket.setRuntimeEntityId(geyserId);
            armorEquipmentPacket.setHelmet(helmet);
            armorEquipmentPacket.setBoots(ItemData.AIR);
            armorEquipmentPacket.setChestplate(chest);
            armorEquipmentPacket.setLeggings(ItemData.AIR);
            armorEquipmentPacket.setBody(chest); // WHY

            session.sendUpstreamPacket(armorEquipmentPacket);
        } else {
            MobEquipmentPacket handPacket = new MobEquipmentPacket();
            handPacket.setRuntimeEntityId(geyserId);
            handPacket.setItem(item);
            handPacket.setHotbarSlot(-1);
            handPacket.setInventorySlot(0);
            handPacket.setContainerId(ContainerId.INVENTORY);

            session.sendUpstreamPacket(handPacket);
        }
    }

    public void setDisplayType(ByteEntityMetadata entityMetadata) {
        int i = entityMetadata.getPrimitiveValue();
        displayType = DisplayType.values()[i];
    }

    private static int getColor(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;

        double[] colorLab = DeltaUtils.rgbToLab(r, g, b);

        List<int[]> colors = Arrays.asList(
                new int[]{249, 255, 254},    // 0: White
                new int[]{249, 128, 29},     // 1: Orange
                new int[]{199, 78, 189},     // 2: Magenta
                new int[]{58, 179, 218},     // 3: Light Blue
                new int[]{254, 216, 61},     // 4: Yellow
                new int[]{128, 199, 31},     // 5: Lime
                new int[]{243, 139, 170},    // 6: Pink
                new int[]{71, 79, 82},       // 7: Gray
                new int[]{159, 157, 151},    // 8: Light Gray
                new int[]{22, 156, 156},     // 9: Cyan
                new int[]{137, 50, 184},     // 10: Purple
                new int[]{60, 68, 170},      // 11: Blue
                new int[]{131, 84, 50},      // 12: Brown
                new int[]{94, 124, 22},      // 13: Green
                new int[]{176, 46, 38},      // 14: Red
                new int[]{29, 29, 33}        // 15: Black
        );

        int closestColorIndex = -1;
        double minDeltaE = Double.MAX_VALUE;

        for (int i = 0; i < colors.size(); i++) {
            int[] rgb = colors.get(i);
            double[] lab = DeltaUtils.rgbToLab(rgb[0], rgb[1], rgb[2]);
            double deltaE = DeltaUtils.calculateDeltaE(colorLab, lab);
            if (deltaE < minDeltaE) {
                minDeltaE = deltaE;
                closestColorIndex = i;
            }
        }

        return closestColorIndex;
    }

    public void moveAbsolute(Vector3f position, float yaw, float pitch, float headYaw, boolean isOnGround, boolean teleported) {
        double yOffset = (config != null) ? config.getDouble("y-offset") : 0;

        position = position.clone().add(0, yOffset, 0);

        Quaternionf combined = Quaternionf.from(lastLeft).mul(lastRight).normalize();

        Vector3f fwd = combined.rotate(0f, 0f, 1f);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-fwd.getX(), fwd.getZ()));
        float pitchDeg = (float) Math.toDegrees(Math.asin(MathUtils.clamp(fwd.getY(), -1f, 1f)));
        yawDeg += getYaw();
        yawDeg = MathUtils.wrapDegrees(yawDeg);
        setYaw(yawDeg);
        setHeadYaw(yawDeg);
        setPitch(pitchDeg);
        setPosition(position);
        setOnGround(isOnGround);

        MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
        moveEntityPacket.setRuntimeEntityId(geyserId);
        moveEntityPacket.setPosition(position);
        moveEntityPacket.setRotation(getBedrockRotation());
        moveEntityPacket.setOnGround(isOnGround);
        moveEntityPacket.setTeleported(teleported);

        session.sendUpstreamPacket(moveEntityPacket);
    }
}
