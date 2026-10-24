"""生成 16 种染料蛋贴图：用染料贴图自身的调色板给鸡蛋上色。

思路（分位数配色转移）：
  1. 取 egg.png 的不透明像素，按亮度排序，得到蛋的亮度分布；
  2. 取 <颜色>_dye.png 的不透明像素，按亮度排序，得到染料的亮度分布；
  3. 蛋的每个亮度层级按「累计像素分位数」映射到染料对应分位数的像素颜色。
这样蛋的整体明暗分布与染料一致，色相/饱和度直接来自染料调色板（含其高光、阴影的色相偏移），
渲染时不再需要 dyed_color 乘色。

用法：
  python gen_dye_egg_textures.py <贴图目录> <输出目录>
  <贴图目录> 需包含 egg.png 与 16 个 <颜色>_dye.png（white/orange/magenta/...）。
"""
import os
import sys
from collections import Counter

from PIL import Image

COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
]


def luminance(rgb):
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]


def opaque_pixels(image):
    pixels = []
    for y in range(image.height):
        for x in range(image.width):
            p = image.getpixel((x, y))
            if p[3] > 0:
                pixels.append(p[:3])
    return pixels


def generate(egg_path, dye_path):
    egg = Image.open(egg_path).convert("RGBA")
    dye = Image.open(dye_path).convert("RGBA")
    egg_pixels = opaque_pixels(egg)
    dye_pixels = sorted(opaque_pixels(dye), key=luminance)
    if not egg_pixels or not dye_pixels:
        raise ValueError(f"空贴图: {egg_path} / {dye_path}")

    # 蛋的每个亮度层级 -> 累计像素分位数中点
    counts = Counter(egg_pixels)
    total = len(egg_pixels)
    cumulative = 0
    level_quantile = {}
    for level in sorted(counts, key=luminance):
        level_quantile[level] = (cumulative + counts[level] / 2) / total
        cumulative += counts[level]

    out = Image.new("RGBA", egg.size, (0, 0, 0, 0))
    last = len(dye_pixels) - 1
    for y in range(egg.height):
        for x in range(egg.width):
            p = egg.getpixel((x, y))
            if p[3] == 0:
                continue
            quantile = level_quantile[p[:3]]
            index = min(last, max(0, round(quantile * last)))
            out.putpixel((x, y), dye_pixels[index] + (255,))
    return out


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        return 1
    source_dir, output_dir = sys.argv[1], sys.argv[2]
    os.makedirs(output_dir, exist_ok=True)
    for color in COLORS:
        texture = generate(os.path.join(source_dir, "egg.png"),
                           os.path.join(source_dir, f"{color}_dye.png"))
        texture.save(os.path.join(output_dir, f"{color}.png"))
        print(f"{color}.png")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
