package org.abstruck.chickens.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.abstruck.chickens.breed.ChickenData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：把四个数据包注册表（breed / mutation / fluid_egg / spawn_rule）的原始 JSON 全量下发。
 * 1.19.2 没有数据包注册表的自动同步，因此用这个包替代；客户端收到后走与服务器完全相同的解析路径。
 */
public class SyncChickenDataPacket {
    private final CompoundTag data;

    public SyncChickenDataPacket(Map<String, String> raw) {
        this.data = new CompoundTag();
        raw.forEach(this.data::putString);
    }

    private SyncChickenDataPacket(CompoundTag data) {
        this.data = data;
    }

    public static void encode(SyncChickenDataPacket msg, FriendlyByteBuf buffer) {
        buffer.writeNbt(msg.data);
    }

    public static SyncChickenDataPacket decode(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return new SyncChickenDataPacket(tag != null ? tag : new CompoundTag());
    }

    public static void handle(SyncChickenDataPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Map<String, String> raw = new HashMap<>();
            for (String key : msg.data.getAllKeys()) {
                raw.put(key, msg.data.getString(key));
            }
            ChickenData.applyRaw(raw);
            // 数据晚于 JEI 配方构建时，补注册一次 JEI 配方（未安装 JEI 时内部直接返回）
            org.abstruck.chickens.compat.JeiDataHooks.onDataSynced();
        });
        context.setPacketHandled(true);
    }
}
