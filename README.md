# Chickens 模组扩展教程

> 本文档教你如何**不写一行 Java 代码**，用数据包（datapack）和资源包（resourcepack）给本模组新增鸡品种、杂交规则、流体蛋，或覆盖内置内容。
>
> 面向对象：整合包作者、服务器管理员，以及想给本模组做联动的模组作者。

---

## 一、核心思想：三个数据包注册表

本模组的几乎所有内容都由**数据包注册表**驱动（和原版 `tags`、`trim_pattern` 同一个机制）：

| 注册表 | 内容 | JSON 路径 |
|---|---|---|
| `chickens:breed` | 鸡品种（产物、间隔、属性） | `data/<命名空间>/chickens/breed/<品种id>.json` |
| `chickens:mutation` | 杂交规则（父母 → 加权结果） | `data/<命名空间>/chickens/mutation/<规则id>.json` |
| `chickens:fluid_egg` | 流体蛋定义（流体 + 颜色） | `data/<命名空间>/chickens/fluid_egg/<id>.json` |

这些注册表**自动同步到客户端**（JEI 也能直接读到），所以加条目只需：

1. 在你的数据包里写 JSON；
2. 在你的资源包里放纹理、语言文件；
3. 重启游戏。

下面逐项说明字段格式。

---

## 二、新增鸡品种（breed）

### 2.1 品种 JSON 格式

路径：`data/<命名空间>/chickens/breed/<品种id>.json`

以内置的燧石鸡为例：

```json
{
  "item": {
    "item": "minecraft:flint",
    "weight": 10
  },
  "interval": 600,
  "count": [1, 1],
  "gain_multiplier": 0.2,
  "tier": 1
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `item` | 产物对象 | ✅ | 主产物（见下方产物对象格式） |
| `interval` | int | ✅ | 产出基础间隔（tick，20 tick = 1 秒） |
| `count` | `[min, max]` | ✅ | 单次产出的数量区间 |
| `gain_multiplier` | double | 否，默认 `0.2` | 数量增益系数 |
| `byproducts` | 产物对象列表 | 否，默认空 | 副产物（可多个，按权重随机出） |
| `loot` | ResourceLocation | 否 | 击杀掉落表（可忽略，鸡默认不掉东西） |
| `texture` | ResourceLocation | 否 | 实体纹理位置（见 2.3） |
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
- `weight`：相对权重。抽取时主产物有效权重 = `weight × 力量`，力量 10 时必出主产物；副产物按 `weight` 相对抽取。
- `fluid`：仅当产出物是 `chickens:fluid_egg` 时填写，产出的流体蛋会携带该流体组件（例如内置的水鸡）。

### 2.2 产出计算规则（供调参参考）

```
实际间隔 = interval × 配置乘数 ÷ (1 + growth × 配置系数)
实际数量 = 随机[count.min, count.max] × (1 + gain × gain_multiplier)
```

三个配置项（`chickens-common.toml`）：`production_interval_multiplier`、`growth_interval_factor`、`baby_growth_time` 等。属性值（growth/gain/strength 均为 1~10）由繁殖遗传获得。

### 2.3 纹理

渲染器按以下顺序找纹理：

1. 品种 JSON 的 `texture` 字段（如 `"texture": "mymod:entity/chicken/dirt"`，**不含 .png 后缀**，可指向其他命名空间）；
2. 约定路径 `assets/chickens/textures/entity/chicken/<品种id>.png`（注意固定是 `chickens` 命名空间）；
3. 都没有 → 回退**原版鸡**纹理。

纹理就是原版鸡的 UV 展开（64×32，头身两层），直接拿内置纹理改色即可。

### 2.4 语言文件

实体显示名走语言键：

```json
// assets/chickens/lang/zh_cn.json（资源包里）
"breed.chickens.dirt": "泥土鸡"
```

物品鸡（抓在手里/背包里）的工具提示、JEI 里也显示这个名字。注意键的前缀固定是 `breed.chickens.`，后面跟品种 id 的 path 部分——即使品种来自其他命名空间（如 `mymod:dirt`），语言键也仍然是 `breed.chickens.dirt`。

### 2.5 自动获得的东西（无需额外配置）

新品种一旦注册：

- ✅ 创造 Tab「鸡」里自动出现对应物品鸡（按 tier 分组、组内按 id 排序）；
- ✅ JEI 物品列表、**鸡产出**分类、**熔炉/烟熏炉/营火烹饪**配方全部自动出现；
- ✅ 能被鸡捕手抓取、放入繁殖箱/培育箱/鸡窝、参与繁殖遗传。

### 2.6 让新鸡自然生成？

目前自然生成的品种（主世界燧石/原木/沙、下界石英/灵魂沙）是**硬编码在代码里**的，数据包无法配置生成。新鸡的获取途径：杂交、创造模式、`/summon` 或 `/give`：

```
/summon chickens:resource_chicken ~ ~ ~ {Breed:"<命名空间>:<品种id>"}
/give @s chickens:chicken[chickens:breed="<命名空间>:<品种id>"]
```

---

## 三、新增杂交规则（mutation）

路径：`data/<命名空间>/chickens/mutation/<规则id>.json`

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

注意点：

- `results` 里写回父母品种（如上例）就是「杂交失败回退」，内置配方统一用 0.9/0.1；
- 规则 id 随意（只要求唯一），建议 `父母A_父母B_to_结果` 命名；
- 杂交发生在**繁殖箱**（方块）里，放两只成年鸡 + 种子类物品即可。

---

## 四、新增流体蛋（fluid_egg）

路径：`data/<命名空间>/chickens/fluid_egg/<id>.json`

```json
{
  "fluid": "minecraft:water",
  "color": 4159204
}
```

| 字段 | 说明 |
|---|---|
| `fluid` | 流体 id。**可以注册任意模组的流体**——没有对应流体时该蛋显示灰色、倒不出东西 |
| `color` | 蛋的渲染颜色（RGB 整数，如 `0x3F76E4` = 4159204），物品图标与创造 Tab 条目都会实时染成该色 |

注册后自动：创造 Tab 多一个该颜色的流体蛋条目、JEI 列表出现该变体、蛋扔出/右键放置**向放置格倒出对应流体**（像水桶一样，只能倒出不能收起，可堆叠 64）。

---

## 五、覆盖 / 修改内置内容

数据包的优先级高于 mod 自带的 `src/main/resources/data`。**同路径写同名 JSON 即覆盖**：

- 想改燧石鸡的产出间隔 → 在你的数据包里写 `data/chickens/chickens/breed/flint.json`（完整内容，不能只写想改的字段）；
- 想删某条杂交配方 → 写同名 JSON 把 `results` 改成回退父母（注册表不支持删除内置条目，只能改内容）；
- 同理可覆盖 `fluid_egg`。

资源包同理：同名纹理、语言键直接替换。

---

## 六、完整示例：添加「泥土鸡」（零代码）

目标：新鸡产泥土，由燧石鸡 + 沙子鸡杂交出。

**① 数据包 `dirtpack/data/mymod/chickens/breed/dirt.json`**

```json
{
  "item": { "item": "minecraft:dirt", "weight": 10 },
  "interval": 600,
  "count": [1, 1],
  "gain_multiplier": 0.2,
  "tier": 1
}
```

**② 数据包 `dirtpack/data/mymod/chickens/mutation/flint_sand_to_dirt.json`**

```json
{
  "parents": ["chickens:flint", "chickens:sand"],
  "results": [
    { "breed": "mymod:dirt", "weight": 0.9 },
    { "breed": "chickens:flint", "weight": 0.1 }
  ]
}
```

**③ 资源包纹理**：`assets/chickens/textures/entity/chicken/dirt.png`（复制 `flint.png` 改成土色）。

**④ 资源包语言**：`assets/chickens/lang/zh_cn.json` 加一行 `"breed.chickens.dirt": "泥土鸡"`。

**⑤ 数据包 pack.mcmeta 打包**：数据包目录结构

```
dirtpack/
├── pack.mcmeta            {"pack": {"pack_format": 48, "description": "..."}}
└── data/mymod/chickens/breed/dirt.json
    data/mymod/chickens/mutation/flint_sand_to_dirt.json
```

装进世界或游戏内 `/datapack` 启用 → 完成。繁殖箱放燧石鸡 + 沙子鸡即可杂交出泥土鸡，JEI 自动出现配方。

> 小提示：如果鸡的纹理显示成原版白鸡，检查注册表里品种是否加载成功（`/datapack list`）以及纹理路径是否正确。

---

## 七、给模组作者的 Java API

如果你的模组想**程序化**注册品种（datagen 写 JSON 随 jar 发布，效果等同手写）：

```java
import org.abstruck.chickens.api.ChickenBreedBuilder;
import org.abstruck.chickens.api.ChickenBreedProvider;

// 在 GatherDataEvent 里：
event.addProvider(new ChickenBreedProvider(event.getGenerator().getPackOutput(), "mymod")
        .addBreed(ResourceLocation.fromNamespaceAndPath("mymod", "dirt"),
                ChickenBreedBuilder.of(ResourceLocation.withDefaultNamespace("dirt"))
                        .interval(600)
                        .count(1, 1)
                        .tier(1)
                        .build()));
```

数据模型与注册表键（`org.abstruck.chickens.breed` 包）：

- `ChickenBreed` / `MutationRule` / `FluidEggEntry` — 三个注册表条目的 record；
- `ChickenRegistries.BREED` / `MUTATION` / `FLUID_EGG` — 注册表键（`RegistryAccess` 里查）；
- `BreedLookups` — 查询工具（按 id 解析品种、按力量抽产物等）。

---

## 八、常见问题

| 现象 | 原因 |
|---|---|
| 鸡是原版白鸡纹理 | 纹理路径不对 / 注册表没加载到条目（`/datapack list` 检查） |
| 物品显示 `breed.chickens.xxx` 裸键 | 资源包缺语言文件 |
| 杂交不出新品种 | `parents` 品种 id 写错（看鸡分析器显示的实际品种 id） |
| 流体蛋倒不出液体 | `fluid` 指向的流体在你的包里不存在 |
| 创造 Tab 顺序不对 | `tier` 没设置（同 tier 内按 id 排序；染料鸡固定 tier 0） |
