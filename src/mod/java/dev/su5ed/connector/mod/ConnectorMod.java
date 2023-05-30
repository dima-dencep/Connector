package dev.su5ed.connector.mod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod("connectormod")
public class ConnectorMod {

    public ConnectorMod() {
        ModList modList = ModList.get();

        if (modList.isLoaded("fabric_entity_events_v1")) {
            MinecraftForge.EVENT_BUS.register(EntityApiEvents.class);
        }
        if (modList.isLoaded("fabric_content_registries_v0")) {
            MinecraftForge.EVENT_BUS.register(ContentRegistriesApiEvents.class);
        }
        if (modList.isLoaded("fabric_registry_sync_v0")) {
            MinecraftForge.EVENT_BUS.register(RegistrySyncApiEvents.class);
        }
        if (modList.isLoaded("fabric_events_interaction_v0")) {
            MinecraftForge.EVENT_BUS.register(EntityInteractionApiEvents.class);
        }
        if (modList.isLoaded("fabric_item_api_v1")) {
            MinecraftForge.EVENT_BUS.register(ItemApiEvents.class);
        }
    }
}
