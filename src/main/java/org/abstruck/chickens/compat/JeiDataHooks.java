package org.abstruck.chickens.compat;

import net.minecraftforge.fml.ModList;

/**
 * JEI 是可选的客户端模组：这里用 ModList 判断后再调用 JEI 插件类，
 * 没装 JEI 时不会加载任何 JEI / 客户端类，专用服务器也安全。
 */
public final class JeiDataHooks {
    private JeiDataHooks() {
    }

    /** 客户端收到服务端数据同步后调用（补注册 JEI 配方） */
    public static void onDataSynced() {
        if (ModList.get().isLoaded("jei")) {
            org.abstruck.chickens.compat.jei.ChickensJeiPlugin.tryLateRegister();
        }
    }
}
