
package me.zimzaza4.geyserdisplayentity.entity;

import me.zimzaza4.geyserdisplayentity.ExtensionMain;
import me.zimzaza4.geyserdisplayentity.Settings;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.matrix.Matrix3f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.MoveEntityAbsolutePacket; // added
import org.geysermc.geyser.entity.EntityDefinition;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.util.MathUtils;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;

import java.util.UUID;

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

        propertyManager.add("geyser:t_x", translation.getX() * 10);
        propertyManager.add("geyser:t_y", translation.getY() * 10);
        propertyManager.add("geyser:t_z", translation.getZ() * 10);

        propertyManager.add("geyser:s_x", scale.getX());
        propertyManager.add("geyser:s_y", scale.getY());
        propertyManager.add("geyser:s_z", scale.getZ());

        if (options == null) {
            options = Settings.IMP.GENERAL;
        }
        if (options.VANILLA_SCALE) {
            applyScale();
        }

        propertyManager.add("geyser:r_x", MathUtils.wrapDegrees(rotation.getX()));
        propertyManager.add("geyser:r_y", MathUtils.wrapDegrees(-rotation.getY()));
        propertyManager.add("geyser:r_z", MathUtils.wrapDegrees(-rotation.getZ()));

        propertyManager.add("geyser:s_q", qScale);

        updateBedrockEntityProperties();
    }

    @Override
    public void updateBedrockEntityProperties() {
        if (!valid) {
            return;
        }

        if (propertyManager.hasProperties()) {
            rotationUpdated = false;
            propertyManager.add("geyser:s_id", (int) (Math.random() * 1000000F));
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
        propertyManager.add("geyser:t_x", translation.getX() * 10);
        propertyManager.add("geyser:t_y", translation.getY() * 10);
        propertyManager.add("geyser:t_z", translation.getZ() * 10);
    }

    public void setScale(EntityMetadata<Vector3f, ?> entityMetadata) {

        this.scale = entityMetadata.getValue();

        if (options.VANILLA_SCALE) {
            applyScale();
        }
        propertyManager.add("geyser:s_x", scale.getX());
        propertyManager.add("geyser:s_y", scale.getY());
        propertyManager.add("geyser:s_z", scale.getZ());
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

        propertyManager.add("geyser:r_x", MathUtils.wrapDegrees(rotation.getX()));
        propertyManager.add("geyser:r_y", MathUtils.wrapDegrees(-rotation.getY()));
        propertyManager.add("geyser:r_z", MathUtils.wrapDegrees(-rotation.getZ()));

        this.qScale = s;
        propertyManager.add("geyser:s_q", qScale);
    }

    protected void applyBedrockYawPitchFromCombined() {
        Quaternionf combined = Quaternionf.from(lastLeft).mul(lastRight).normalize();

        Vector3f fwd = combined.rotate(0f, 0f, 1f);
        float yawDeg = (float) Math.toDegrees(Math.atan2(-fwd.getX(), fwd.getZ()));
        float pitchDeg = (float) Math.toDegrees(Math.asin(MathUtils.clamp(fwd.getY(), -1f, 1f)));

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
        propertyManager.add("geyser:r_x", x);
        propertyManager.add("geyser:r_y", y);
        propertyManager.add("geyser:r_z", z);
        updateBedrockEntityProperties();
    }
}
