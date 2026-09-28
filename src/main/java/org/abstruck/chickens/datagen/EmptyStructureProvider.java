package org.abstruck.chickens.datagen;

import com.google.common.hash.Hashing;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPOutputStream;

/**
 * 生成 GameTest 用的 3×3×3 空结构模板（data/chickens/structure/gametest_empty.nbt）。
 * 结构 NBT 手工构造：size + 一个空 palette + 空 blocks/entities + DataVersion。
 * 注意：必须经 CachedOutput.writeIfNeeded 写文件，直接 Files 写的文件会被
 * datagen 框架当作过期文件在结束时删除。
 */
public final class EmptyStructureProvider implements DataProvider {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private final PackOutput output;

    public EmptyStructureProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        CompoundTag tag = new CompoundTag();
        tag.put("size", intList(3, 3, 3));
        tag.put("blocks", new ListTag());
        tag.put("entities", new ListTag());
        ListTag palettes = new ListTag();
        palettes.add(new ListTag());
        tag.put("palettes", palettes);
        tag.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());

        // gzip 压缩的 NBT（与 StructureTemplateManager 的读取格式一致）
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos); DataOutputStream out = new DataOutputStream(gzip)) {
            NbtIo.write(tag, out);
        } catch (IOException e) {
            throw new RuntimeException("无法序列化空结构模板", e);
        }
        byte[] bytes = bos.toByteArray();
        // GameTest 框架的结构名 = <测试类名小写>.<template>，即 productiongametest.gametest_empty
        Path path = this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("chickens/structure/productiongametest.gametest_empty.nbt");
        try {
            cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
            LOGGER.info("[chickens] 空结构模板已生成: {}", path);
        } catch (IOException e) {
            throw new RuntimeException("无法写入空结构模板", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private static ListTag intList(int x, int y, int z) {
        ListTag list = new ListTag();
        list.add(IntTag.valueOf(x));
        list.add(IntTag.valueOf(y));
        list.add(IntTag.valueOf(z));
        return list;
    }

    @Override
    public String getName() {
        return "chickens empty game test structure";
    }
}
