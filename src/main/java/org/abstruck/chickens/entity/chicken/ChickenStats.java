package org.abstruck.chickens.entity.chicken;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** 鸡的三维属性（各 1~10）：实体上走 SynchedEntityData，鸡物品上走物品 NBT（见 ItemData） */
public record ChickenStats(byte growth, byte gain, byte strength) {
    public static final ChickenStats DEFAULT = new ChickenStats((byte) 1, (byte) 1, (byte) 1);

    public static final Codec<ChickenStats> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<ChickenStats> instance) -> instance.group(
                    Codec.INT.fieldOf("growth").forGetter(stats -> (int) stats.growth()),
                    Codec.INT.fieldOf("gain").forGetter(stats -> (int) stats.gain()),
                    Codec.INT.fieldOf("strength").forGetter(stats -> (int) stats.strength())
            ).apply(instance, (growth, gain, strength) ->
                    new ChickenStats((byte) (int) growth, (byte) (int) gain, (byte) (int) strength)));
}
