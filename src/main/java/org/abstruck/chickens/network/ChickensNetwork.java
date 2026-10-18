package org.abstruck.chickens.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.abstruck.chickens.Chickens;
import org.abstruck.chickens.breed.ChickenData;

/** 网络通道：目前只有「数据内容全量同步」一个包 */
public final class ChickensNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(Chickens.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ChickensNetwork() {
    }

    /** 必须在 mod 构造期调用（注册消息） */
    public static void register() {
        CHANNEL.messageBuilder(SyncChickenDataPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncChickenDataPacket::encode)
                .decoder(SyncChickenDataPacket::decode)
                .consumerMainThread(SyncChickenDataPacket::handle)
                .add();
    }

    /** 把当前服务端数据内容发给一名玩家 */
    public static void syncTo(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SyncChickenDataPacket(ChickenData.raw()));
    }

    /** 把当前服务端数据内容发给所有玩家 */
    public static void syncToAll() {
        CHANNEL.send(PacketDistributor.ALL.noArg(),
                new SyncChickenDataPacket(ChickenData.raw()));
    }
}
