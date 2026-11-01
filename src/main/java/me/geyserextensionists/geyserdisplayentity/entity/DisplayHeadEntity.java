package me.geyserextensionists.geyserdisplayentity.entity;

import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.geysermc.geyser.entity.VanillaEntities;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.geyser.entity.type.player.SkullPlayerEntity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.skin.SkullSkinManager;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The skin of a player head shown by an item display. Bedrock has no player head with a custom skin, so this is drawn
 * the way Geyser draws a placed one: a player entity with a head-only skin, free to sit anywhere and at any scale.
 */
public class DisplayHeadEntity extends SkullPlayerEntity {

    private String skinUrl;
    private float headScale = -1;

    public DisplayHeadEntity(GeyserSession session) {
        super(EntitySpawnContext.DUMMY_CONTEXT.apply(session, UUID.randomUUID(), VanillaEntities.PLAYER));
    }

    @Override
    public String getSkinUrl() {
        return skinUrl;
    }

    public void setSkin(String url) {
        if (Objects.equals(url, skinUrl)) return;

        skinUrl = url;
        setFlag(EntityFlag.INVISIBLE, true);
        updateBedrockMetadata();
        SkullSkinManager.requestAndHandleSkin(this, session, skin -> session.scheduleInEventLoop(() -> {
            if (!Objects.equals(url, skinUrl)) return;
            setFlag(EntityFlag.INVISIBLE, false);
            updateBedrockMetadata();
        }, 250, TimeUnit.MILLISECONDS));
    }

    /**
     * @param origin the bottom centre of the head
     * @param scale  the head's edge in blocks over half a block, the edge of an unscaled head
     */
    public void place(Vector3f origin, float yaw, float scale, boolean teleported) {
        if (scale != headScale) {
            headScale = scale;
            metadata.put(EntityDataTypes.SCALE, scale);
            updateBedrockMetadata();
        }
        moveAbsoluteRaw(origin, yaw, 0, yaw, true, teleported);
    }
}
