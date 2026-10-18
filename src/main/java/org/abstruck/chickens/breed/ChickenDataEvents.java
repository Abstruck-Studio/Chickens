package org.abstruck.chickens.breed;

import com.mojang.logging.LogUtils;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.abstruck.chickens.network.ChickensNetwork;
import org.slf4j.Logger;

/** 数据内容的生命周期：服务器资源重载时载入，玩家加入 / {@code /reload} 时同步给客户端 */
public final class ChickenDataEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ChickenDataEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ChickenReloadListener());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ChickensNetwork.syncTo(event.getPlayer());
        } else {
            ChickensNetwork.syncToAll();
        }
    }

    /** 启动自检：确认数据内容与配方都正确加载（便于排查数据包问题） */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        int recipes = event.getServer().getRecipeManager().getRecipes().size();
        ChickenData.Snapshot snapshot = ChickenData.get();
        LOGGER.info("[chickens] 启动自检：配方 {} 个，品种 {} 个，杂交规则 {} 条，流体蛋 {} 个，生成规则 {} 条",
                recipes, snapshot.breeds().size(), snapshot.mutations().size(),
                snapshot.fluidEggs().size(), snapshot.spawnRules().size());
    }
}
