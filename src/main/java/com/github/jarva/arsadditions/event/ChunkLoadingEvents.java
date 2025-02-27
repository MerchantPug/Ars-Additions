package com.github.jarva.arsadditions.event;

import com.github.jarva.arsadditions.ArsAdditions;
import com.github.jarva.arsadditions.server.storage.ChunkLoadingData;
import com.github.jarva.arsadditions.setup.config.ServerConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

@EventBusSubscriber(modid = ArsAdditions.MODID)
public class ChunkLoadingEvents {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        MinecraftServer server = serverLevel.getServer();

        ChunkLoadingData.loadChunks(server, uuid, true);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUUID();

        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (!ServerConfig.SERVER.chunkloading_require_online.get()) return;

        MinecraftServer server = serverLevel.getServer();

        ChunkLoadingData.loadChunks(server, uuid, false);
    }
}
