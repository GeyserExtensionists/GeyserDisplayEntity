package me.zimzaza4.geyserdisplayentity.entity;

import me.zimzaza4.geyserdisplayentity.ExtensionMain;
import me.zimzaza4.geyserdisplayentity.Settings;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.matrix.Matrix3f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket; // added
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.properties.type.FloatProperty;
import org.geysermc.geyser.entity.properties.type.IntProperty;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.util.UUID;

import static me.zimzaza4.geyserdisplayentity.ExtensionMain.MAX_VALUE;
import static me.zimzaza4.geyserdisplayentity.ExtensionMain.MIN_VALUE;

public class SlotDisplayEntity extends Entity {

    protected ItemData item = ItemData.AIR;
    protected Vector3f translation = Vector3f.from(0, 0, 0);
    protected Vector3f scale = Vector3f.from(1, 1, 1);
    protected Vector3f rotation = Vector3f.from(0, 0, 0);
    protected float qScale = 1F;
    protected boolean validQScale = false;
    protected boolean rotationUpdated = false;

    protected Settings.DisplayEntityOptions options = Settings.IMP.GENERAL;

    protected Quaternionf lastLeft = Quaternionf.IDENTITY;
    protected Quaternionf lastRight = Quaternionf.IDENTITY;

    public SlotDisplayEntity(GeyserSession session, int entityId, long geyserId, UUID uuid,
                             EntityDefinition<?> definition,
                             Vector3f position, Vector3f motion, float yaw, float pitch, float headYaw) {
        super(session, entityId, geyserId, uuid, definition, position, motion, yaw, pitch, headYaw);
    }

    public void updateMainHand(GeyserSession session) {
    }

    @Override
    public void initializeMetadata() {
        super.initializeMetadata();

        item = ItemData.AIR;
        translation = Vector3f.from(0, 0, 0);
        scale = Vector3f.from(1, 1, 1);
        rotation = Vector3f.from(0, 0, 0);
        qScale = 1F;
        validQScale = false;
        rotationUpdated = false;

        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_x"), MAX_VALUE, MIN_VALUE, 0F), translation.getX() * 10);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_y"), MAX_VALUE, MIN_VALUE, 0F), translation.getY() * 10);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_z"), MAX_VALUE, MIN_VALUE, 0F), translation.getZ() * 10);

        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_x"), MAX_VALUE, MIN_VALUE, 0F), scale.getX());
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_y"), MAX_VALUE, MIN_VALUE, 0F), scale.getY());
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_z"), MAX_VALUE, MIN_VALUE, 0F), scale.getZ());

        if (options == null) {
            options = Settings.IMP.GENERAL;
        }
        if (options.VANILLA_SCALE) {
            applyScale();
        }

        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_x"), 180F, -180F, 0F), MathUtils.wrapDegrees(rotation.getX()));
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_y"), 180F, -180F, 0F), MathUtils.wrapDegrees(-rotation.getY()));
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_z"), 180F, -180F, 0F), MathUtils.wrapDegrees(-rotation.getZ()));

        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_q"), MAX_VALUE, MIN_VALUE, 0F), qScale);

        updateBedrockEntityProperties();
    }

    @Override
    public void updateBedrockEntityProperties() {
        if (!valid) {
            return;
        }

        if (propertyManager.hasProperties()) {
            rotationUpdated = false;
            propertyManager.addProperty(new IntProperty(Identifier.of("geyser:s_id"), MAX_VALUE, MIN_VALUE, 0), (int) (Math.random() * MAX_VALUE));
        }

        super.updateBedrockEntityProperties();
    }

    @Override
    public void updateBedrockMetadata() {
        updateBedrockEntityProperties();
        super.updateBedrockMetadata();
    }

    public void setTranslation(EntityMetadata<Vector3f, ?> entityMetadata) {
        this.translation = entityMetadata.getValue();
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_x"), MAX_VALUE, MIN_VALUE, 0F), translation.getX() * 10);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_y"), MAX_VALUE, MIN_VALUE, 0F), translation.getY() * 10);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:t_z"), MAX_VALUE, MIN_VALUE, 0F), translation.getZ() * 10);
    }

    public void setScale(EntityMetadata<Vector3f, ?> entityMetadata) {

        this.scale = entityMetadata.getValue();

        if (options.VANILLA_SCALE) {
            applyScale();
        }
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_x"), MAX_VALUE, MIN_VALUE, 0F), scale.getX());
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_y"), MAX_VALUE, MIN_VALUE, 0F), scale.getY());
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_z"), MAX_VALUE, MIN_VALUE, 0F), scale.getZ());
    }


    protected void applyScale() {
        Vector3f vector3f = this.scale;
        float scale = (vector3f.getX() + vector3f.getY() + vector3f.getZ()) / 3;
        this.dirtyMetadata.put(EntityDataTypes.SCALE, scale);
    }

    public void setLeftRotation(EntityMetadata<Quaternionf, ?> entityMetadata) {
        Quaternionf quaternion = entityMetadata.getValue();
        this.lastLeft = quaternion;
        setRotation(quaternion);
        rotationUpdated = true;
        applyBedrockYawPitchFromCombined();
    }

    public void setRightRotation(EntityMetadata<Quaternionf, ?> entityMetadata) {
        Quaternionf quaternion = entityMetadata.getValue();
        this.lastRight = quaternion;
        setRotation(quaternion);
        rotationUpdated = true;
        applyBedrockYawPitchFromCombined();
    }

    protected void setRotation(Quaternionf q) {
        float s = magnitude(q);
        Vector3f r = toEulerZYX(q);

        // this.scale = scale.mul(s);
        if (rotationUpdated) {
            this.rotation = rotation.add(r);
        } else {
            this.rotation = r;
        }

        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_x"), 180F, -180F, 0F), MathUtils.wrapDegrees(rotation.getX()));
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_y"), 180F, -180F, 0F), MathUtils.wrapDegrees(-rotation.getY()));
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_z"), 180F, -180F, 0F), MathUtils.wrapDegrees(-rotation.getZ()));

        this.qScale = s;
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:s_q"), MAX_VALUE, MIN_VALUE, 0F), qScale);
    }

    protected void applyBedrockYawPitchFromCombined() {
        Quaternionf combined = Quaternionf.from(lastLeft).mul(lastRight).normalize();

        Vector3f fwd = combined.rotate(0f, 0f, 1f);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-fwd.getX(), fwd.getZ()));
        float pitchDeg = (float) Math.toDegrees(Math.asin(MathUtils.clamp(fwd.getY(), -1f, 1f)));

        yawDeg += getYaw();

        yawDeg = MathUtils.wrapDegrees(yawDeg);

        setYaw(yawDeg);
        setHeadYaw(yawDeg);
        setPitch(pitchDeg);

        MoveEntityAbsolutePacket rotPkt = new MoveEntityAbsolutePacket();
        rotPkt.setRuntimeEntityId(geyserId);
        rotPkt.setPosition(position);
        rotPkt.setRotation(getBedrockRotation());
        rotPkt.setTeleported(false);
        session.sendUpstreamPacket(rotPkt);
    }

    protected Vector3f toEulerZYX(Quaternionf q) {
        Quaternionf qn = q.normalize();

        Matrix3f m = Matrix3f.createRotation(qn);

        float r20 = m.get(2, 0);
        float r21 = m.get(2, 1);
        float r22 = m.get(2, 2);
        float r00 = m.get(0, 0);
        float r01 = m.get(0, 1);
        float r10 = m.get(1, 0);
        float r11 = m.get(1, 1);

        float x, y, z;

        if (Math.abs(r20) < 0.9999999F) {
            x = (float) Math.atan2(r21, r22);
            y = (float) Math.asin(-r20);
            z = (float) Math.atan2(r10, r00);
        } else {
            // Gimbal lock: pitch is approximately ±90°
            x = 0;
            y = (r20 > 0) ? -(float) Math.PI / 2 : (float) Math.PI / 2;
            z = (float) Math.atan2(-r01, r11);
        }

        return Vector3f.from(Math.toDegrees(x), Math.toDegrees(y), Math.toDegrees(z));
    }

    public float magnitude(Quaternionf q) {
        return (float) q.getW() * q.getW() + q.getX() * q.getX() + q.getY() * q.getY() + q.getZ() * q.getZ();
    }

    protected void hackRotation(float x, float y, float z) {
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_x"), 180F, -180F, 0F), x);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_y"), 180F, -180F, 0F), y);
        propertyManager.addProperty(new FloatProperty(Identifier.of("geyser:r_z"), 180F, -180F, 0F), z);
        updateBedrockEntityProperties();
    }
}