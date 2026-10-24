package org.abstruck.chickens.breed;

import com.mojang.datafixers.util.Unit;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 服务端数据重载监听器：在 {@code AddReloadListenerEvent} 里注册，随服务器资源重载（启动、{@code /reload}）执行。
 * 读取 data/&lt;ns&gt;/chickens/* 下的全部 JSON 并替换 {@link ChickenData} 快照；
 * 重载完成后由 {@code OnDatapackSyncEvent} 把原始 JSON 同步给客户端。
 */
public final class ChickenReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        Map<String, String> raw = ChickenData.readRaw(resourceManager);
        return stage.wait(Unit.INSTANCE)
                .thenRunAsync(() -> ChickenData.applyRaw(raw), gameExecutor);
    }
}
