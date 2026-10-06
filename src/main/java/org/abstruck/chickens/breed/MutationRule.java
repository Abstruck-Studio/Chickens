package org.abstruck.chickens.breed;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.abstruck.chickens.Chickens;

import java.util.List;
import java.util.Optional;

/**
 * 杂交规则：一对父母 + 加权结果表（数据包注册表 {@code chickens:mutation} 的条目）。
 * 父母可写精确品种 id 或 {@code #品种标签}；标签成员来自品种 JSON 自声明的 tags 字段，
 * 索引构建时展开成具体品种（见 BreedLookups.mutationIndex）。
 */
public record MutationRule(List<BreedRef> parents, List<WeightedResult> results) {

    /** 父母引用：精确品种 id 或 #命名空间:标签（标签让一条规则覆盖整组品种） */
    public record BreedRef(Either<ResourceLocation, TagKey<ChickenBreed>> value) {
        public static final Codec<BreedRef> CODEC = Codec.either(
                ResourceLocation.CODEC,
                TagKey.hashedCodec(ChickenRegistries.BREED)
        ).xmap(BreedRef::new, BreedRef::value);

        public static BreedRef of(ResourceLocation breed) {
            return new BreedRef(Either.left(breed));
        }

        public static BreedRef of(TagKey<ChickenBreed> tag) {
            return new BreedRef(Either.right(tag));
        }

        public Optional<ResourceLocation> breedId() {
            return this.value.left();
        }

        public Optional<TagKey<ChickenBreed>> tag() {
            return this.value.right();
        }
    }

    /** 结果引用：精确品种 id 或 {@code $parent:标签} —— 返还实际参与本次繁殖的、带该标签的父本
     *  （两只都带随机取一；都没带（规则写错）记日志后随机取一兜底）；$parent 后缺命名空间按 chickens: 解析。
     *  解析在 {@link BreedLookups#roll} 里结合本次实际父母完成。 */
    public record ResultRef(Either<ResourceLocation, TagKey<ChickenBreed>> value) {
        public static final String PARENT_PREFIX = "$parent:";

        public static final Codec<ResultRef> CODEC =
                Codec.STRING.comapFlatMap(ResultRef::parse, ResultRef::encode);

        public static ResultRef of(ResourceLocation breed) {
            return new ResultRef(Either.left(breed));
        }

        public static ResultRef of(TagKey<ChickenBreed> tag) {
            return new ResultRef(Either.right(tag));
        }

        public Optional<ResourceLocation> breedId() {
            return this.value.left();
        }

        public Optional<TagKey<ChickenBreed>> tag() {
            return this.value.right();
        }

        private static DataResult<ResultRef> parse(String value) {
            if (value.startsWith(PARENT_PREFIX)) {
                String rest = value.substring(PARENT_PREFIX.length());
                ResourceLocation tagId = ResourceLocation.tryParse(
                        rest.indexOf(':') >= 0 ? rest : Chickens.MODID + ":" + rest);
                if (tagId == null) {
                    return DataResult.error(() -> "无效的 $parent 标记（标签 id 不合法）: " + value);
                }
                return DataResult.success(of(TagKey.create(ChickenRegistries.BREED, tagId)));
            }
            ResourceLocation breedId = ResourceLocation.tryParse(value);
            if (breedId == null) {
                return DataResult.error(() -> "无效的品种 id: " + value);
            }
            return DataResult.success(of(breedId));
        }

        private static String encode(ResultRef ref) {
            if (ref.breedId().isPresent()) {
                return ref.breedId().get().toString();
            }
            ResourceLocation tagId = ref.tag().orElseThrow().location();
            return PARENT_PREFIX + (tagId.getNamespace().equals(Chickens.MODID)
                    ? tagId.getPath() : tagId.toString());
        }
    }

    /** 加权结果：同一父母对命中的全部规则合并后按权重抽取 */
    public record WeightedResult(ResultRef breed, double weight) {
        public static final Codec<WeightedResult> CODEC = RecordCodecBuilder.create(
                (RecordCodecBuilder.Instance<WeightedResult> instance) -> instance.group(
                        ResultRef.CODEC.fieldOf("breed").forGetter(WeightedResult::breed),
                        Codec.DOUBLE.fieldOf("weight").forGetter(WeightedResult::weight)
                ).apply(instance, WeightedResult::new));
    }

    public static final Codec<MutationRule> CODEC = RecordCodecBuilder.create(
            (RecordCodecBuilder.Instance<MutationRule> instance) -> instance.group(
                    BreedRef.CODEC.listOf().fieldOf("parents").forGetter(MutationRule::parents),
                    WeightedResult.CODEC.listOf().fieldOf("results").forGetter(MutationRule::results)
            ).apply(instance, MutationRule::new));
}
