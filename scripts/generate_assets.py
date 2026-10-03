import os
import json
from PIL import Image

BASE_ASSETS = "src/main/resources/assets/digitalexchange"
BASE_DATA = "src/main/resources/data/digitalexchange"

TEX_DIR = os.path.join(BASE_ASSETS, "textures", "block")
BLOCKSTATES = os.path.join(BASE_ASSETS, "blockstates")
MODELS_BLOCK = os.path.join(BASE_ASSETS, "models", "block")
MODELS_ITEM = os.path.join(BASE_ASSETS, "models", "item")
LANG_DIR = os.path.join(BASE_ASSETS, "lang")
RECIPE_DIR = os.path.join(BASE_DATA, "recipe")

for p in [TEX_DIR, BLOCKSTATES, MODELS_BLOCK, MODELS_ITEM, LANG_DIR, RECIPE_DIR]:
    os.makedirs(p, exist_ok=True)

# 1. PIXEL TEXTURE (16x16 exchange_core.png)
img = Image.new("RGBA", (16, 16), (25, 28, 35, 255))
pixels = img.load()

for i in range(16):
    pixels[i, 0] = pixels[0, i] = pixels[i, 15] = pixels[15, i] = (15, 17, 22, 255)
    pixels[i, 1] = pixels[1, i] = pixels[i, 14] = pixels[14, i] = (45, 52, 65, 255)

circuit_cyan = (0, 235, 255, 255)
bright_glow = (200, 255, 255, 255)
dark_blue = (10, 80, 130, 255)

for x in range(5, 11):
    for y in range(5, 11):
        pixels[x, y] = dark_blue

for x in range(6, 10):
    for y in range(6, 10):
        pixels[x, y] = circuit_cyan

pixels[7, 7] = pixels[8, 7] = pixels[7, 8] = pixels[8, 8] = bright_glow

pixels[7, 2] = pixels[8, 2] = pixels[7, 3] = pixels[8, 3] = pixels[7, 4] = pixels[8, 4] = circuit_cyan
pixels[7, 11] = pixels[8, 11] = pixels[7, 12] = pixels[8, 12] = pixels[7, 13] = pixels[8, 13] = circuit_cyan
pixels[2, 7] = pixels[2, 8] = pixels[3, 7] = pixels[3, 8] = pixels[4, 7] = pixels[4, 8] = circuit_cyan
pixels[11, 7] = pixels[11, 8] = pixels[12, 7] = pixels[12, 8] = pixels[13, 7] = pixels[13, 8] = circuit_cyan

img.save(os.path.join(TEX_DIR, "exchange_core.png"))

# 2. BLOCKSTATES & MODELS
with open(os.path.join(BLOCKSTATES, "exchange_core.json"), "w") as f:
    json.dump({"variants": {"": {"model": "digitalexchange:block/exchange_core"}}}, f, indent=2)

with open(os.path.join(MODELS_BLOCK, "exchange_core.json"), "w") as f:
    json.dump({
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "digitalexchange:block/exchange_core"}
    }, f, indent=2)

with open(os.path.join(MODELS_ITEM, "exchange_core.json"), "w") as f:
    json.dump({"parent": "digitalexchange:block/exchange_core"}, f, indent=2)

# 3. LOCALIZATION
with open(os.path.join(LANG_DIR, "en_us.json"), "w") as f:
    json.dump({
        "block.digitalexchange.exchange_core": "Digital Exchange Core",
        "item.digitalexchange.exchange_core": "Digital Exchange Core"
    }, f, indent=2)

# 4. AE2 CRAFTING RECIPE
recipe = {
    "type": "minecraft:crafting_shaped",
    "pattern": [
        "CPC",
        "FDF",
        "CSC"
    ],
    "key": {
        "C": {"item": "minecraft:iron_block"},
        "P": {"item": "ae2:calculation_processor"},
        "F": {"item": "ae2:fluix_crystal"},
        "D": {"item": "minecraft:diamond_block"},
        "S": {"item": "ae2:storage_bus"}
    },
    "result": {
        "id": "digitalexchange:exchange_core",
        "count": 1
    }
}
with open(os.path.join(RECIPE_DIR, "exchange_core.json"), "w") as f:
    json.dump(recipe, f, indent=2)

print("Assets and recipes generated successfully.")
