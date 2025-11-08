package me.zimzaza4.geyserdisplayentity.entity;

import me.zimzaza4.geyserdisplayentity.ExtensionMain;
import me.zimzaza4.geyserdisplayentity.Settings;
import me.zimzaza4.geyserdisplayentity.type.DisplayType;
import me.zimzaza4.geyserdisplayentity.util.DeltaUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerId;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MobArmorEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket;
import org.geysermc.geyser.entity.EntityDefinition;
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
import java.util.UUID;

public class ItemDisplayEntity extends SlotDisplayEntity {
    private DisplayType displayType = DisplayType.NONE;
    private Byte color;
    private boolean custom = false;
    private boolean needHide = false;
    private double lastOffset = 0;

    public ItemDisplayEntity(GeyserSession session, int entityId, long geyserId, UUID uuid,
                             EntityDefinition<?> definition,
                             Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void setOffset(double offset) {
        moveRelative(0, offset - lastOffset, 0, pitch, yaw, false);
        this.lastOffset = offset;
    }

    public void setDisplayedItem(EntityMetadata<ItemStack, ?> entityMetadata) {
        ItemStack stack = entityMetadata.getValue();
        if (stack == null) {
            this.item = ItemData.AIR;
            setInvisible(true);
            return;
        }
    
        ItemData item = ItemTranslator.translateToBedrock(session, stack);
        this.item = item;
    
        if (item instanceof DyeableArmorItem) {
            @Nullable DataComponents data = stack.getDataComponentsPatch();
            if (data != null) {
                Integer dyed = data.get(DataComponentTypes.DYED_COLOR);
                if (dyed != null) {
                    this.color = Byte.parseByte(String.valueOf(getColor(dyed)));
                }
            }
        }
    
        String type = session.getItemMappings()
                .getMapping(stack.getId())
                .getJavaItem()
                .javaIdentifier();
    
        CustomModelData modelData = null;
        @Nullable DataComponents components = stack.getDataComponentsPatch();
        if (components != null) {
            modelData = components.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        }
    
        for (Settings.DisplayEntityMapping mapping : Settings.IMP.MAPPINGS.values()) {
            if (mapping.TYPE.equals(type)) {
                if (mapping.MODEL_DATA == -1) {
                    if (mapping.OPTIONS != null) {
                        options = mapping.OPTIONS;
                        setOffset(options.Y_OFFSET);
                    }
                    break;
                }
                if (modelData != null && Math.abs(mapping.MODEL_DATA - modelData.floats().get(0)) < 0.5) {
                    if (mapping.OPTIONS != null) {
                        options = mapping.OPTIONS;
                        setOffset(options.Y_OFFSET);
                    }
                    break;
                }
            }
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
        String javaId = session.getItemMappings()
                .getMapping(stack)
                .getJavaItem()
                .javaIdentifier();
        if (Settings.IMP.HIDE_TYPES.contains(javaId)) {
            setInvisible(true);
            needHide = true;
            this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        } else {
            needHide = false;
        }
    
        updateMainHand(session);
    }

    @Override
    protected void applyScale() {
        if (needHide) {
            this.dirtyMetadata.put(EntityDataTypes.SCALE, 0f);
        } else {
            super.applyScale();
        }
    }

    @Override
    public void updateMainHand(GeyserSession session) {
        if (!valid)
            return;

        ItemData helmet = ItemData.AIR; // TODO
        ItemData chest = item;

        if (custom && !options.HAND) {
            MobArmorEquipmentPacket armorEquipmentPacket = new MobArmorEquipmentPacket();
            armorEquipmentPacket.setRuntimeEntityId(this.geyserId);
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
        double yOffset = (options != null) ? options.Y_OFFSET : 0;
    
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
