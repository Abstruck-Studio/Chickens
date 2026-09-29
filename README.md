# Chickens 模组（NeoForge 1.21.1）

一个养鸡模组：45 种资源鸡、三维属性育种、杂交、自动化设施，以及完全数据驱动的内容系统。
所有品种、杂交规则、流体蛋、自然生成都能**不写一行 Java 代码**，用数据包（datapack）新增、调整或覆盖。

---

## 一、功能总览

### 1.1 鸡与三维属性

- **45 个品种**：8 种基础鸡（燧石/原木/沙/雪球/火药/石英/灵魂沙/岩浆）、21 种高阶鸡（铁/金/钻石/绿宝石/烈焰棒/末影珍珠……）、16 种染料鸡（红/黄/蓝/绿……，鸡蛋+染料合成投掷获得）
- 每只鸡有 **Growth / Gain / Strength** 三维属性（1~10）：
  - **Growth 生长**：产出更快（每点缩短 10%，配置可调）
  - **Gain 增益**：单次产出更多
  - **Strength 力量**：提高主产物权重，10 点时必出主产物
- 自然生成 / 繁殖所得属性初始为 1，**同品种繁殖**逐代提升属性
- 击杀掉落与原版鸡完全相同（生鸡肉、羽毛、火焰烧熟、抢夺加成）；品种 JSON 可指定自定义掉落表
- 物品鸡可以放进熔炉/烟熏炉/营火烧成鸡肉

### 1.2 繁殖与杂交

- **繁殖箱**（干草+木板合成）：放入两只鸡 + 种子类物品 → 30 秒（配置）产出一只**幼年物品鸡**。同品种走遗传升级（属性 ≥ 父母最大值），异品种走杂交规则表
- **杂交**：34 条内置规则（如 燧石×原木→煤、铁×萤石→金、水×岩浆→黑曜石，染料鸡互相杂交出新颜色），全部数据包可覆盖；杂交出的新品种属性重置为 1
- **培育箱**：幼年物品鸡 + 种子 → 30 秒（配置）成长为成年鸡
- **受精蛋**：开启配置 `vanillaBreedingRework` 后，两只鸡交配不再直接生小鸡，而是掉落携带品种+属性的受精蛋
- 幼年鸡 5 分钟（配置 `babyGrowthTime`）自然长大

### 1.3 设施（全部支持漏斗/管道自动化）

| 设施 | 作用 |
|---|---|
| **繁殖箱** | 两鸡槽 + 种子槽 + 输出：配对产出幼年物品鸡（无种子空窗帘/繁殖中拉窗帘的外观变化） |
| **培育箱** | 幼年鸡 + 种子 → 成年鸡；方块内实时渲染鸡模型 |
| **鸡窝** | 成年鸡每 30 秒（配置，受 Growth 加速）产一次该品种资源，最多同时放多只鸡；方块内实时渲染鸡模型 |

### 1.4 工具与物品

| 物品 | 说明 |
|---|---|
| **鸡捕手** | 右键任意鸡（含原版）→ 变成物品鸡（品种/属性/幼年状态/名字全保留），右键放出 |
| **物品鸡** | 每种鸡一个物品（图标为实体实时渲染），右键放置生成鸡 |
| **鸡分析器** | 右键鸡显示品种、三维属性、距下次产出时间 |
| **受精蛋** | 交配掉落（配置开启时），携带品种与属性 |
| **染料蛋** | 鸡蛋+染料合成（16 色），投掷命中按原版鸡蛋概率孵出 1~4 只对应品种的幼鸡 |
| **流体蛋** | 统一的水蛋/岩浆蛋/自定义流体蛋：右键像桶一样倒出流体源（不能装，可堆叠 64），可与储罐类模组交互 |

### 1.5 自然生成（数据包可配置）

- 主世界：燧石鸡 / 原木鸡 / 沙鸡；下界：石英鸡（灵魂沙峡谷另有灵魂沙鸡）；末地：末影珍珠鸡
- 全部由数据包注册表 `chickens:spawn_rule` 控制（品种、权重、密度、维度、群系），详见 2.5

### 1.6 JEI 联动（装了 JEI 才生效）

- 「鸡杂交」分类：父母 → 全部加权结果（概率标在槽位下方），催化剂为繁殖箱
- 「鸡产出」分类：鸡 → 主产物 + 副产物（概率按权重），催化剂为鸡窝
- 物品鸡的熔炉/烟熏炉/营火烹饪配方（含全部品种变体）

### 1.7 配置文件

`config/chickens-common.toml`：

| 键 | 默认 | 说明 |
|---|---|---|
| `mutationEnabled` | true | 异品种杂交是否产生新品种 |
| `vanillaBreedingRework` | false | 交配改为掉落受精蛋 |
| `productionIntervalMultiplier` | 1.0 | 实体鸡产出间隔全局乘数 |
| `growthIntervalFactor` | 0.1 | 每点 Growth 的加速比例 |
| `breedingBoxBreedTime` | 600 | 繁殖箱配对耗时（tick） |
| `growerTime` | 600 | 培育箱成长耗时（tick） |
| `nestProductionTime` | 600 | 鸡窝产出间隔（tick） |
| `babyGrowthTime` | 6000 | 幼年鸡成长时间（tick，原版为 24000） |

---

## 二、扩展教程：四个数据包注册表

本模组的内容都由**数据包注册表**驱动（和原版 `tags`、`trim_pattern` 同一个机制）：

| 注册表 | 内容 | JSON 路径 |
|---|---|---|
| `chickens:breed` | 鸡品种（产物、属性、掉落、纹理） | `data/<命名空间>/chickens/breed/<品种id>.json` |
| `chickens:mutation` | 杂交规则（父母 → 加权结果） | `data/<命名空间>/chickens/mutation/<规则id>.json` |
| `chickens:fluid_egg` | 流体蛋定义（流体 + 颜色） | `data/<命名空间>/chickens/fluid_egg/<id>.json` |
| `chickens:spawn_rule` | 自然生成规则（维度 + 群系 + 品种 + 密度） | `data/<命名空间>/chickens/spawn_rule/<规则id>.json` |

这些注册表**自动同步到客户端**（JEI 也能直接读到），加条目只需：写 JSON → 重启游戏。

### 2.1 新增鸡品种（breed）

以内置的燧石鸡为例：

```json
{
  "item": {
    "item": "minecraft:flint",
    "weight": 10
  },
  "count": [1, 1],
  "gain_multiplier": 0.2,
  "tier": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `item` | 产物对象 | ✅ | 主产物（见下方产物对象格式） |
| `count` | `[min, max]` | ✅ | 单次产出的数量区间 |
| `gain_multiplier` | double | 否，默认 `0.2` | 数量增益系数 |
| `byproducts` | 产物对象列表 | 否，默认空 | 副产物（可多个，按权重随机出） |
| `loot` | ResourceLocation | 否 | 击杀掉落表；缺省掉落与原版鸡完全相同 |
| `texture` | ResourceLocation | 否 | 实体纹理位置（见 2.6） |
| `tier` | int | 否，默认 `0` | 等级标记，**仅用于创造 Tab 分组排序**，无玩法效果 |

**产物对象格式**（`item` 与 `byproducts[]` 共用）：

```json
{
  "item": "minecraft:flint",
  "weight": 10,
  "fluid": "minecraft:water"   // 可选：产出流体蛋时携带的流体 id
}
```

- `item`：产出物品 id（**可以是任何模组的物品**）。
- `weight`：相对权重。主产物有效权重 = `weight × 力量`，力量 10 时必出主产物；副产物按 `weight` 相对抽取。
- `fluid`：仅当产出物是 `chickens:fluid_egg` 时填写（例如内置的水鸡）。

**产出计算规则**（供调参参考）：

```
鸡窝：实际间隔 = nestProductionTime ÷ (1 + growth × growthIntervalFactor)
实体鸡：实际间隔 = 原版鸡下蛋节奏（5~10 分钟随机）× productionIntervalMultiplier ÷ (1 + growth × growthIntervalFactor)
实际数量 = 随机[count.min, count.max] × (1 + gain × gain_multiplier)
```

### 2.2 新增杂交规则（mutation）

```json
{
  "parents": ["chickens:flint", "chickens:log"],
  "results": [
    { "breed": "chickens:coal", "weight": 0.9 },
    { "breed": "chickens:flint", "weight": 0.1 }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `parents` | 恰好两个品种 id（顺序无关，繁殖箱里左右槽放哪只都行） |
| `results` | 加权结果表：**同一父母对命中全部规则的所有结果合并**后按 weight 抽取 |

- `results` 里写回父母品种（如上例）就是「杂交失败回退」；
- 规则 id 随意（只要求唯一），建议 `父母A_父母B_to_结果` 命名；
- 杂交发生在**繁殖箱**里（两只成年鸡 + 种子）；实体鸡交配走同一规则表。

### 2.3 新增流体蛋（fluid_egg）

```json
{
  "fluid": "minecraft:water",
  "color": 4159204
}
```

| 字段 | 说明 |
|---|---|
| `fluid` | 流体 id。**可以注册任意模组的流体**——没有对应流体时该蛋显示灰色、倒不出东西 |
| `color` | 蛋的渲染颜色（RGB 整数），物品图标与创造 Tab 条目实时染色 |

注册后自动：创造 Tab 多一个该颜色的流体蛋条目、JEI 列表出现该变体、右键向目标格倒出对应流体（像水桶一样，可堆叠 64）。

### 2.4 让新鸡自然生成（spawn_rule）

```json
{
  "dimension": "minecraft:the_end",
  "biomes": ["#minecraft:is_end", "somemod:weird_biome"],
  "chance": 0.25,
  "breeds": [
    { "breed": "chickens:ender", "weight": 2 },
    { "breed": "mymod:dirt", "weight": 1 }
  ]
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `dimension` | 维度 id | 否 | 缺省 = 任意维度（可写任意模组的维度 id） |
| `biomes` | 字符串列表 | 否 | 群系过滤：元素 = 群系 id 或 **`#` 开头的群系标签**（原版/NeoForge/任意模组标签均可）；缺省 = 任意群系 |
| `breeds` | 品种权重表 | ✅ | 生成时按权重抽取品种 |
| `chance` | 0~1 小数 | 否，默认 `1.0` | **生成密度**：每次生成尝试被放行的概率（`0.25` = 密度降到 1/4）。多条规则命中时取**最小值**——调密度只改带 chance 的规则，加新品种的规则不写 chance 不影响密度 |

语义：

- **增量式**：同一生成位置命中多条规则时**合并权重**——想给某群系加新品种，只需新增一条规则，无需覆盖整条；
- **未命中 = 不生成**：规则表是唯一真相。覆盖/移除某条规则即可关闭对应群系的生成；
- 内置规则 id：`overworld` / `nether` / `nether_soul_sand_valley` / `end`（数据包写同名文件即可覆盖）。

**生成入口**：鸡要能进某群系的生成池，该群系还必须在标签 `#chickens:spawnable_biomes` 里（内置 = 主世界 + 下界 + 末地）。让你的群系生成鸡：

```json
// data/<你的命名空间>/tags/worldgen/biome/spawnable_biomes.json
{ "values": ["<你的群系id>", "#<某个包含你群系的标签>"] }
```

> 机制说明：`add_spawns` 修饰器强制按实体类别注入 creature 池；下界/末地的正常密度由
> 本模组内置的群系 JSON 覆盖实现（monster 池直写）。自定义维度只能走 creature 通道。

### 2.5 纹理

渲染器按以下顺序找实体纹理：

1. 品种 JSON 的 `texture` 字段（如 `"texture": "mymod:entity/chicken/dirt"`，**不含 .png 后缀**，可指向其他命名空间）；
2. 约定路径 `assets/chickens/textures/entity/chicken/<品种id>.png`（注意固定是 `chickens` 命名空间）；
3. 都没有 → 回退**原版鸡**纹理。

纹理就是原版鸡的 UV 展开（64×32，头身两层），直接拿内置纹理改色即可。

### 2.6 语言文件

```json
// assets/chickens/lang/zh_cn.json（资源包里）
"breed.chickens.dirt": "泥土鸡"
```

实体名、物品工具提示、JEI 里都显示这个名字。键的前缀固定是 `breed.chickens.` + 品种 id 的 path 部分——即使品种来自其他命名空间（如 `mymod:dirt`），语言键也仍是 `breed.chickens.dirt`。

### 2.7 自动获得的东西（无需额外配置）

新品种一旦注册：

- ✅ 创造 Tab「鸡」里自动出现对应物品鸡（按 tier 分组、组内按 id 排序）；
- ✅ JEI 物品列表、**鸡产出**分类、**熔炉/烟熏炉/营火烹饪**配方全部自动出现；
- ✅ 能被鸡捕手抓取、放入繁殖箱/培育箱/鸡窝、参与繁殖遗传。

### 2.8 覆盖 / 修改内置内容

数据包的优先级高于 mod 自带的 `src/main/resources/data`。**同路径写同名 JSON 即覆盖**：

- 改燧石鸡的产出 → 写 `data/chickens/chickens/breed/flint.json`（完整内容，不能只写想改的字段）；
- 改/删某条杂交配方 → 同名 JSON 覆盖（注册表不支持删除内置条目，把 results 改成回退父母即等效删除）；
- 同理可覆盖 `fluid_egg`、`spawn_rule`。

资源包同理：同名纹理、语言键直接替换。

---

## 三、完整示例：添加「泥土鸡」（零代码）

目标：新鸡产泥土，由燧石鸡 + 沙子鸡杂交出，在沼泽群系自然生成。

**① 品种** `dirtpack/data/mymod/chickens/breed/dirt.json`

```json
{
  "item": { "item": "minecraft:dirt", "weight": 10 },
  "count": [1, 1],
  "gain_multiplier": 0.2,
  "tier": 1
}
```

**② 杂交** `dirtpack/data/mymod/chickens/mutation/flint_sand_to_dirt.json`

```json
{
  "parents": ["chickens:flint", "chickens:sand"],
  "results": [
    { "breed": "mymod:dirt", "weight": 0.9 },
    { "breed": "chickens:flint", "weight": 0.1 }
  ]
}
```

**③ 自然生成** `dirtpack/data/mymod/chickens/spawn_rule/dirt_swamp.json`

```json
{
  "dimension": "minecraft:overworld",
  "biomes": ["minecraft:swamp", "minecraft:mangrove_swamp"],
  "breeds": [ { "breed": "mymod:dirt", "weight": 1 } ]
}
```

（主世界群系已在 `#chickens:spawnable_biomes` 标签里，无需加标签。）

**④ 资源包纹理**：`assets/chickens/textures/entity/chicken/dirt.png`（复制 `flint.png` 改成土色）。

**⑤ 资源包语言**：`assets/chickens/lang/zh_cn.json` 加 `"breed.chickens.dirt": "泥土鸡"`。

**⑥ 打包**（pack_format 48）装进世界或游戏内 `/datapack` 启用 → 完成。

> 小提示：如果鸡显示成原版白鸡，检查注册表是否加载成功（`/datapack list`）以及纹理路径是否正确。

---

## 四、给模组作者的 Java API

程序化注册品种（datagen 写 JSON 随 jar 发布，效果等同手写）：

```java
import org.abstruck.chickens.api.ChickenBreedBuilder;
import org.abstruck.chickens.api.ChickenBreedProvider;

// 在 GatherDataEvent 里：
event.addProvider(new ChickenBreedProvider(event.getGenerator().getPackOutput(), "mymod")
        .addBreed(ResourceLocation.fromNamespaceAndPath("mymod", "dirt"),
                ChickenBreedBuilder.of(ResourceLocation.withDefaultNamespace("dirt"))
                        .count(1, 1)
                        .tier(1)
                        .build()));
```

数据模型与注册表键（`org.abstruck.chickens.breed` 包）：

- `ChickenBreed` / `MutationRule` / `FluidEggEntry` / `SpawnRule` — 四个注册表条目的 record；
- `ChickenRegistries.BREED` / `MUTATION` / `FLUID_EGG` / `SPAWN_RULE` — 注册表键（`RegistryAccess` 里查）；
- `BreedLookups` — 查询工具（按 id 解析品种、按力量抽产物、自然生成规则查询等）。

---

## 五、常见问题

| 现象 | 原因 |
|---|---|
| 鸡是原版白鸡纹理 | 纹理路径不对 / 注册表没加载到条目（`/datapack list` 检查） |
| 物品显示 `breed.chickens.xxx` 裸键 | 资源包缺语言文件 |
| 杂交不出新品种 | `parents` 品种 id 写错（看鸡分析器显示的实际品种 id）或 `mutationEnabled` 关闭 |
| 流体蛋倒不出液体 | `fluid` 指向的流体在你的包里不存在 |
| 某群系不生成鸡 | 群系不在 `#chickens:spawnable_biomes` 标签 / 没有命中的 spawn_rule / chance 过低 |
| 创造 Tab 顺序不对 | `tier` 没设置（同 tier 内按 id 排序；染料鸡固定 tier 0） |
| 击杀没有掉落 | 品种 `loot` 字段指向了不存在的掉落表（缺省掉落与原版鸡相同） |
