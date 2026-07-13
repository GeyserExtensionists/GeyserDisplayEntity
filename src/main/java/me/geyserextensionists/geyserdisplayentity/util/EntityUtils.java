package me.geyserextensionists.geyserdisplayentity.util;

import me.geyserextensionists.geyserdisplayentity.GeyserDisplayEntity;
import org.geysermc.geyser.api.entity.custom.CustomEntityDefinition;
import org.geysermc.geyser.api.entity.definition.GeyserEntityDefinition;
import org.geysermc.geyser.api.entity.property.GeyserEntityProperty;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntitiesEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineEntityPropertiesEvent;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.BedrockEntityDefinition;
import org.geysermc.geyser.entity.GeyserEntityType;
import org.geysermc.geyser.entity.VanillaEntityType;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Collection;

public class EntityUtils {

    public static BedrockEntityDefinition findOrRegisterCustomDefinition(GeyserDisplayEntity extension, GeyserDefineEntitiesEvent event, Identifier identifier) {
        for (GeyserEntityDefinition definition : event.entities()) {
            if (definition.identifier().equals(identifier)) {
                if (definition instanceof BedrockEntityDefinition bedrockDefinition && !definition.vanilla()) {
                    extension.logger().info("Reusing already registered custom entity " + identifier);
                    return bedrockDefinition;
                }

                throw new IllegalStateException("Entity identifier is already registered by an incompatible definition: " + identifier);
            }
        }

        CustomEntityDefinition customDefinition = CustomEntityDefinition.of(identifier);
        event.register(customDefinition);
        return (BedrockEntityDefinition) customDefinition;
    }

    public static void replaceJavaDefinition(EntityType javaType, VanillaEntityType<?> definition) {
        GeyserEntityType geyserType = GeyserEntityType.ofVanilla(javaType);
        Registries.JAVA_ENTITY_TYPES.register(geyserType, definition);
        Registries.JAVA_ENTITY_IDENTIFIERS.register(geyserType.identifier().toString(), definition);
    }

    public static void registerFloat(GeyserDefineEntityPropertiesEvent event, Collection<GeyserEntityProperty<?>> existing, Identifier entityIdentifier, String propertyIdentifier, float min, float max, float defaultValue) {
        Identifier property = Identifier.of(propertyIdentifier);
        if (existing.stream().noneMatch(candidate -> candidate.identifier().equals(property))) {
            event.registerFloatProperty(entityIdentifier, property, min, max, defaultValue);
        }
    }

    public static void registerInteger(GeyserDefineEntityPropertiesEvent event, Collection<GeyserEntityProperty<?>> existing, Identifier entityIdentifier, String propertyIdentifier, int min, int max, int defaultValue) {
        Identifier property = Identifier.of(propertyIdentifier);
        if (existing.stream().noneMatch(candidate -> candidate.identifier().equals(property))) {
            event.registerIntegerProperty(entityIdentifier, property, min, max, defaultValue);
        }
    }
}