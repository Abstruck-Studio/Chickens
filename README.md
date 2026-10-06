# Chickens 模组（NeoForge 1.21.1）

一个养鸡模组：71 种资源鸡、三维属性育种、55 条杂交配方、自动化设施，以及完全数据驱动的内容系统。
所有品种、杂交规则、流体蛋、自然生成都能**不写一行 Java 代码**，用数据包（datapack）新增、调整或覆盖。

---

## 一、功能总览

### 1.1 鸡与三维属性

- **71 个品种**，按 Tier 0~8 分级（tier 越高越靠杂交链深层，见下表）
- 每只鸡有 **Growth / Gain / Strength** 三维属性（1~10）：
  - **Growth 生长**：产出更快（每点缩短 10%，配置可调）
  - **Gain 增益**：单次产出更多
  - **Strength 力量**：提高主产物权重，10 点时必出主产物
- 自然生成 / 繁殖所得属性初始为 1，**同品种繁殖**逐代提升属性
- 击杀掉落与原版鸡完全相同（生鸡肉、羽毛、火焰烧熟、抢夺加成）；品种 JSON 可指定自定义掉落表
- 部分品种自带**环境粒子**（末影鸡=传送门、灵魂沙鸡=灵魂、熔岩鸡=岩浆滴落、樱花原木鸡=樱花飘落），品种 JSON `particles` 字段可配任意模组粒子（见 2.1）
- 物品鸡可以放进熔炉/烟熏炉/营火烧成鸡肉

**Tier 分级与获取方式**：

| Tier | 品种（数量） | 获取方式 |
|---|---|---|
| 0 | 16 色染料鸡（白/橙/品红/淡蓝/黄/黄绿/粉红/灰/淡灰/青/紫/蓝/棕/绿/红/黑） | 鸡蛋+染料合成**染料蛋**投掷孵出 |
| 1 | 燧石 / 沙 / 下界石英 / 灵魂沙（4） | 自然生成 |
| 2 | 10 原木/菌鸡 + 竹子 + 红蘑菇 / 棕蘑菇（13） | 自然生成；蘑菇与竹也可由杂交获得 |
| 3 | 10 岩石鸡：石头 / 安山岩 / 闪长岩 / 花岗岩 / 凝灰岩 / 方解石 / 深板岩 / 玄武岩 / 黑石 / 下界岩 | 全部由**石头鸡 + 染料**杂交产出（石头鸡自带 `#chickens:rock_chicken` 标签） |
| 4 | 雪球 / 煤 / 火药 / 下界疣 / 线 / 红石 / 萤石 / 铁 / 铜 / 紫水晶（10） | 杂交链第一层（父母均为 Tier 0~3） |
| 5 | 熔岩 / 水 / 玻璃 / 青金石 / 金 / 皮革（6） | 杂交链第二层 |
| 6 | 粘土 / 海晶碎片 / 黑曜石 / 烈焰棒 / 钻石（5） | 杂交链第三层 |
| 7 | 史莱姆 / 末影珍珠 / 恶魂之泪 / 绿宝石（4） | 杂交链第四层（末影珍珠鸡亦可自然生成） |
| 8 | 海晶砂粒 / 岩浆膏 / 远古残骸（3） | 杂交链顶层（科技树终点） |

> 10 只原木/菌鸡带 `#chickens:log_chicken` 标签（杂交规则可用 `#标签` 引用整组、`$parent:标签` 返还实际参与的那只，见 2.1）。

### 1.2 繁殖与杂交

- **繁殖箱**（干草+木板合成）：放入两只鸡 + 种子类物品 → 30 秒（配置）产出一只**幼年物品鸡**。同品种走遗传升级（属性 ≥ 父母最大值），异品种走杂交规则表
- **杂交**：**55 条内置规则**，完整覆盖 Tier 0~8 的科技树：燧石×沙→火药、石头×燧石→铁、铁×铜→金、水×熔岩→黑曜石、钻石×海晶碎片→绿宝石、绿宝石×金→远古残骸，染料鸡互相杂交出新颜色、石头鸡+各色染料培育岩石鸡、橡木原木鸡+各色染料培育其他树种……全部数据包可覆盖；杂交出的新品种属性重置为 1；未命中任何规则时**优先产出 tier 较低的一方**（同级随机），防止用低级鸡蹭高级鸡提前解锁
- **培育箱**：幼年物品鸡 + 种子 → 30 秒（配置）成长为成年鸡
- 幼年鸡 5 分钟（配置 `babyGrowthTime`）自然长大

### 1.3 设施（全部支持漏斗/管道自动化）

| 设施 | 作用 |
|---|---|
| **繁殖箱** | 两鸡槽 + 种子槽 + 输出：配对产出幼年物品鸡（无种子空窗帘/繁殖中拉窗帘的外观变化） |
| **培育箱** | 幼年鸡 + 种子 → 成年鸡；方块内实时渲染鸡模型 |
| **鸡窝** | 成年鸡按品种节奏产出该品种资源（JSON `interval`，缺省 30 秒；受 Growth 加速），最多同时放多只鸡；方块内实时渲染鸡模型 |

### 1.4 工具与物品

| 物品 | 说明 |
|---|---|
| **鸡捕手** | 右键任意鸡（含原版）→ 变成物品鸡（品种/属性/幼年状态/名字全保留），右键放出 |
| **物品鸡** | 每种鸡一个物品（图标为实体实时渲染），右键放置生成鸡 |
| **鸡分析器** | 右键鸡显示品种、三维属性、距下次产出时间 |
| **染料蛋** | 鸡蛋+染料合成（16 色），投掷命中按原版鸡蛋概率孵出 1~4 只对应品种的幼鸡 |
| **流体蛋** | 统一的水蛋/岩浆蛋/自定义流体蛋：右键像桶一样倒出流体源（不能装，可堆叠 64），可与储罐类模组交互 |

### 1.5 自然生成（数据包可配置）

- 主世界：燧石鸡 / 橡木原木鸡 / 沙子鸡（橡木原木鸡所有主世界群系都生成），另有树种鸡按群系生成：云杉（针叶林系）、白桦（白桦森林）、丛林（丛林系）、金合欢（热带草原系）、深色橡木（黑森林）、红树（红树林沼泽）、樱花（樱花树林）；下界：石英鸡 + 绯红菌柄鸡（绯红森林）/ 诡异菌柄鸡（诡异森林），灵魂沙峡谷另有灵魂沙鸡；末地：末影珍珠鸡
- 全部由数据包注册表 `chickens:spawn_rule` 控制（品种、权重、密度、维度、群系），详见 2.4
- 杂交链顶层（雪球、煤、铁、钻石、远古残骸等）**不在野生成池中**，只能靠杂交获得——这是科技树进度的主要来源

### 1.6 JEI 联动（装了 JEI 才生效）

- 「鸡杂交」分类：父母 → 全部加权结果（概率标在槽位下方），催化剂为繁殖箱
- 「鸡产出」分类：鸡 → 主产物 + 副产物（概率按权重），催化剂为鸡窝
- 物品鸡的熔炉/烟熏炉/营火烹饪配方（含全部品种变体）

### 1.7 配置文件

`config/chickens-common.toml`：

| 键 | 默认 | 说明 |
|---|---|---|
| `productionIntervalMultiplier` | 1.0 | 实体鸡产出间隔全局乘数 |
| `growthIntervalFactor` | 0.1 | 每点 Growth 的加速比例 |
| `breedingBoxBreedTime` | 600 | 繁殖箱配对耗时（tick） |
| `growerTime` | 600 | 培育箱成长耗时（tick） |
| `babyGrowthTime` | 6000 | 幼年鸡成长时间（tick，原版为 24000） |

### 1.8 精简版资源（`empty/`）

仓库另有一份**只保留 16 只染料鸡**的资源副本，位于 `empty/`（结构与 `src/main/resources` 相同）——用于发布精简版，或作为整合包 / 数据包的扩展起点。设施、工具、配方、流体蛋机制、自然生成注入等基础设施全部保留，品种只留染料鸡：

| 内容 | 完整版 | empty 版 |
|---|---|---|
| 品种（breed） | 71 | 16（16 色染料鸡） |
| 杂交规则（mutation） | 55 | 10（染料×染料） |
| 自然生成规则（spawn_rule） | 13 | 0（生成默认被否决；补上品种+规则即恢复） |
| 实体贴图 / 语言键 | 71 | 16 |

注意两点：`empty/` 只是资源副本，**不参与构建**（要发布 empty 版需复制回 `src/main/resources` 或配置构建变体）；代码里"无品种组件物品鸡"的默认品种是 `chickens:flint`，empty 版没有燧石鸡——放出这类鸡前先用鸡分析器 / 命令设置品种，或往 empty 版补回 `flint.json` 与对应贴图、语言键。

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
| `item` | 产物对象 | ✅ | 主产物（物品 id 或 `#标签`，见下方产物对象格式） |
| `count` | `[min, max]` | ✅ | 单次产出的数量区间 |
| `gain_multiplier` | double | 否，默认 `0.2` | 数量增益系数 |
| `byproducts` | 产物对象列表 | 否，默认空 | 副产物（可多个，按权重随机出；与主产物共用数量公式） |
| `loot` | ResourceLocation | 否 | 击杀掉落表；缺省掉落与原版鸡完全相同 |
| `texture` | ResourceLocation | 否 | 实体纹理位置（见 2.5） |
| `tier` | int | 否，默认 `0` | 等级标记 0~8（内置品种的分布见 1.1）：异品种杂交**未命中规则时优先产出 tier 较低的一方**（同级随机），也用于创造 Tab 分组排序 |
| `tags` | 字符串 / 字符串数组 | 否，默认空 | 品种所属标签（`#chickens:log_chicken` 形式）：杂交规则可用 `#标签` 引用整组品种、`$parent:标签` 返还实际参与的那只（见下方「品种标签」） |
| `particles` | 字符串 / 对象 | 否，缺省无粒子 | 环境粒子特效（简写或完整形态，见下方） |
| `interval` | int（tick） | 否，缺省 `600`（30 秒） | **鸡窝**产出间隔；只影响鸡窝（实体鸡节奏保持原版），仍受 Growth 加速 |

**粒子特效**（`particles`，客户端本地生成、零网络开销）：

```json
"particles": "minecraft:portal"                                                       // 简写 = 每 tick 2 颗（末影人同款）
"particles": { "type": "minecraft:flame", "chance": 0.3, "count": 1, "spread": 0.5 }  // 完整形态
```

- `type`：粒子注册表 id，**任意模组的粒子**都行（`minecraft:snowflake`、`mymod:my_particle`……）；引用未安装模组的粒子时静默跳过；
- `chance`：每 tick 触发概率（默认 `1.0`）；`count`：每次触发颗数（默认 `2`）；
- `spread`：水平散布半径（默认 `0.5`，与末影人一致；语义 = 体宽 × spread，调小让粒子更贴着身体）；
- 位置/速度公式与末影人一致（身体周围随机撒点、带轻微下坠）。
- 内置示例：`ender.json`（简写）、`lava.json`（完整形态）可直接参考。

**品种标签**（`tags`，杂交规则的分组引用；如内置的 `#chickens:log_chicken` 含 10 只原木/菌鸡、`#chickens:rock_chicken` 含 10 只岩石鸡）：

```json
"tags": "#chickens:log_chicken"                        // 单个标签
"tags": ["#chickens:log_chicken", "#mymod:hard_wood"]  // 或数组，声明多个
```

- 按官方命名规范写成 `#命名空间:路径`（**命名空间要写全**，省略会被当成 `minecraft:`）；
- 它只是品种自己的**分组声明**（不走数据包 tags 目录）：成员 = 所有在 JSON 里声明了该标签的品种；
- 杂交规则（见 2.2）里两种用法：
  - `parents` 里写 `"#chickens:log_chicken"` → 任一成员 × 对方都命中这条规则（不必逐个列父母）；
  - `results[].breed` 里写 `"$parent:log_chicken"`（后缀与标签同名，也可写全 `$parent:chickens:log_chicken`）→ **返还实际参与本次繁殖的那只**带该标签的父本（白桦原木鸡配的就返还白桦原木鸡）；两只都带 → 随机一只；都没带（规则写错）→ 记日志后随机一只兜底。

**产物对象格式**（`item` 与 `byproducts[]` 共用）：

```json
{
  "item": "minecraft:flint",     // 精确物品 id
  "item": "#c:ingots/silver",    // 或物品标签（矿词），两种写法二选一
  "weight": 10,
  "fluid": "minecraft:water"   // 可选：产出流体蛋时携带的流体 id（或 "#流体标签"）
}
```

- `item`：产出物品引用，两种写法：
  - **精确 id**（`"minecraft:flint"`）：**可以是任何模组的物品**；引用的物品不存在（模组没装）时跳过该份产出并记日志，不崩溃；
  - **物品标签 / 矿词**（`"#c:ingots/silver"`，`#` 开头）：**产出时从标签内已注册物品随机抽一个**——整合包里多个模组都提供同一物品（银锭、钢锭等）时用它；标签为空 / 没装提供方时跳过该份产出并记日志。JEI 产出页对标签固定显示标签内第一个物品。
- `weight`：相对权重。主产物有效权重 = `weight × 力量`，力量 10 时必出主产物；副产物按 `weight` 相对抽取。
- `fluid`：仅当产出物是 `chickens:fluid_egg` 时填写（例如内置的水鸡、熔岩鸡）。与 `item` 同款双形态：精确 id（`"minecraft:lava"`）或流体标签 / 矿词（`"#c:lava"`，产出时从标签内已注册流体随机抽一个）；解析总是落到具体流体，渲染颜色仍按具体流体查 `chickens:fluid_egg` 注册表。

**产出计算规则**（供调参参考）：

```
鸡窝：实际间隔 = (品种 interval 或内置默认 600) ÷ (1 + growth × growthIntervalFactor)
实体鸡：实际间隔 = 原版鸡下蛋节奏（5~10 分钟随机）× productionIntervalMultiplier ÷ (1 + growth × growthIntervalFactor)
实际数量 = 随机[count.min, count.max] × (1 + gain × gain_multiplier)
产物 item 为标签时：物品 = 从标签内已注册物品随机抽取（每次产出单独抽）
产物 fluid 为标签时：流体 = 从标签内已注册流体随机抽取（同上）
```

### 2.2 新增杂交规则（mutation）

```json
{
  "parents": ["chickens:stone", "#chickens:log_chicken"],
  "results": [
    { "breed": "chickens:coal", "weight": 0.8 },
    { "breed": "$parent:log_chicken", "weight": 0.2 }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `parents` | 恰好两个：品种 id（`"chickens:stone"`）或 `#品种标签`（`"#chickens:log_chicken"`，见 2.1「品种标签」）；顺序无关，繁殖箱里左右槽放哪只都行 |
| `results` | 加权结果表：`breed` 为品种 id 或 `$parent:标签`（返还实际参与的那只成员）；**同一父母对命中全部规则的所有结果合并**后按 weight 抽取 |

- `results` 里写回父母品种（`"chickens:stone"` 这种具体 id，`$parent:` 标记也行）就是「杂交失败回退」；
- 规则 id 随意（只要求唯一），建议 `父母A_父母B_to_结果` 命名；
- 杂交发生在**繁殖箱**里（两只成年鸡 + 种子）；实体鸡交配走同一规则表；
- 注意**规则重叠**：若多条规则命中同一父母对，结果会合并后按 weight 抽取（常见于两条规则用了同一个 `#标签` 父母）——模组自带配方已全部避开，你自己加配方时记得排查；
- JEI 杂交页对 `#标签` 父母与 `$parent:` 回退结果，会在**同一个槽里循环展示**标签的全部成员鸡。

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
- 内置规则 id：`overworld` / `overworld_taiga` / `overworld_birch_forest` / `overworld_jungle` / `overworld_savanna` / `overworld_dark_forest` / `overworld_mangrove_swamp` / `overworld_cherry_grove` / `nether` / `nether_soul_sand_valley` / `nether_crimson_forest` / `nether_warped_forest` / `end`（数据包写同名文件即可覆盖）。

**生成入口**：鸡要能进某群系的生成池，该群系还得有「入口」。入口分两类，都可在数据包里调整：

- **creature 通道（主世界风格）**：把群系加进标签 `#chickens:spawnable_biomes`（内置 = 主世界 + 下界 + 末地）——内置 biome modifier（官方 `neoforge:add_spawns`，weight 25）会给标签内所有群系的 creature 池加鸡：

```json
// data/<你的命名空间>/tags/worldgen/biome/spawnable_biomes.json
{ "values": ["<你的群系id>", "#<某个包含你群系的标签>"] }
```

- **monster 通道（下界/末地风格，每 tick 尝试、密度更高）**：写一个自己的 biome modifier JSON，用本模组的自定义类型 `chickens:add_spawns_to_category` 指定 `category`（内置的下界 = `#minecraft:is_nether` weight 100、末地 = `#minecraft:is_end` weight 15 就是这么注入的）：

```json
// data/<你的命名空间>/neoforge/biome_modifier/my_chicken.json
{
  "type": "chickens:add_spawns_to_category",
  "biomes": "<你的群系id / 列表 / #标签>",
  "category": "monster",
  "spawners": { "type": "chickens:resource_chicken", "weight": 15, "minCount": 1, "maxCount": 2 }
}
```

> 机制说明：官方 `neoforge:add_spawns` 的池类别固定取实体自身类别（resource_chicken 是 creature），够不着 monster 池；本模组为此注册了自定义类型 `chickens:add_spawns_to_category`（多一个可选 `category` 字段，缺省 = 官方行为），下界/末地的 monster 通道由它**叠加式注入**——不覆盖原版群系文件，与其他 mod 零冲突，自定义维度同样适用。有入口之后，实际生成还必须有命中的 spawn_rule（上一节）。

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
- 同理可覆盖 `fluid_egg`、`spawn_rule`；
- 关掉/调整内置生成入口 → 覆盖 `data/chickens/neoforge/biome_modifier/` 下同名 JSON（如 `add_resource_chicken_nether.json`），把 `"type"` 改成 `"neoforge:none"` 即禁用该条。

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
    { "breed": "mymod:dirt", "weight": 0.8 },
    { "breed": "chickens:flint", "weight": 0.2 }
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
- `BreedLookups` — 查询工具（按 id 解析品种、按力量抽产物、自然生成规则查询、按标签取品种成员等）。

品种标签走 `ChickenBreedBuilder.tag(TagKey<ChickenBreed>)`；杂交规则的两端引用分别是
`MutationRule.BreedRef`（品种 id 或 `#标签`）与 `MutationRule.ResultRef`（品种 id 或 `$parent:标签`），
都有 `of(...)` 工厂与 `breedId()` / `tag()` 访问器，codec 与 JSON 写法一致。

---

## 五、常见问题

| 现象 | 原因 |
|---|---|
| 鸡是原版白鸡纹理 | 纹理路径不对 / 注册表没加载到条目（`/datapack list` 检查） |
| 物品显示 `breed.chickens.xxx` 裸键 | 资源包缺语言文件 |
| 杂交不出新品种 | `parents` 品种 id 写错（看鸡分析器显示的实际品种 id）——此时按 tier 低者回退产出（见 1.2） |
| 杂交概率和预期不符 | 检查是否有**另一条规则命中同一父母对**（结果会合并抽取，见 2.2） |
| 流体蛋倒不出液体 | `fluid` 指向的流体在你的包里不存在 |
| 某群系不生成鸡 | 群系没有生成池入口（见 2.4）/ 没有命中的 spawn_rule / chance 过低 |
| 创造 Tab 顺序不对 | `tier` 没设置（同 tier 内按 id 排序；内置品种分布见 1.1） |
| 击杀没有掉落 | 品种 `loot` 字段指向了不存在的掉落表（缺省掉落与原版鸡相同） |
