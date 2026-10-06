package org.abstruck.chickens.entity.chicken;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** 鸡的三维属性（各 1~10）：实体上走 SynchedEntityData，鸡物品上走 {@code chickens:stats} 组件 */
public record ChickenStats(byte growth, byte gain, byte strength) {
    public static final ChickenStats DEFAULT = new ChickenStats((byte) 1, (byte) 1, (byte) 1);

    public static final Codec<ChickenStats> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<ChickenStats> instance) -> instance.group(
                    Codec.BYTE.fieldOf("growth").forGetter(ChickenStats::growth),
                    Codec.BYTE.fieldOf("gain").forGetter(ChickenStats::gain),
                    Codec.BYTE.fieldOf("strength").forGetter(ChickenStats::strength)
            ).apply(instance, ChickenStats::new));

    public static final StreamCodec<ByteBuf, ChickenStats> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BYTE, ChickenStats::growth,
            ByteBufCodecs.BYTE, ChickenStats::gain,
            ByteBufCodecs.BYTE, ChickenStats::strength,
            ChickenStats::new);
}
