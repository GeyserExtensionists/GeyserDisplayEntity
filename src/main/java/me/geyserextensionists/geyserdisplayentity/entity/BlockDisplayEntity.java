package me.geyserextensionists.geyserdisplayentity.entity;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityDataTypes;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.IntEntityMetadata;

public class BlockDisplayEntity extends SlotDisplayEntity {

    // Do we even need this if we're never gonna use it?? - xSquishyLiam

    // TODO
    // Kastle hasnt finish it
    public BlockDisplayEntity(EntitySpawnContext entitySpawnContext) {
        super(entitySpawnContext);
    }

    public void setDisplayedBlockState(IntEntityMetadata blockState) {
        this.dirtyMetadata.put(EntityDataTypes.DISPLAY_BLOCK_STATE, this.session.getBlockMappings().getBedrockBlock(blockState.getPrimitiveValue()));
    }
}