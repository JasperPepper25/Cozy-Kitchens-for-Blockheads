# Cozy Kitchens for Blockheads — 0.2.1 test build

A Forge 1.20.1 add-on for Cooking for Blockheads, created for Rafe's customized ATM9 pack. Farmer's Delight cabinet artwork gives each wood its own panel, slat, lattice or hardware design. Original pale oak cabinetry, white ceramic sinks, gold or black faucets, quartz worktops, and coordinated enamel appliances complete the sets.

## Changes in 0.2.1

| Faucet finish | Sink sets |
| --- | --- |
| Gold | Spruce, Jungle, Dark Oak, Mangrove, Cherry, Crimson |
| Black | Oak, Birch, Acacia, Bamboo, Warped, Pale Oak |

Pale oak now has original framed side panels and a bordered wood-grain texture for cabinet tops and undersides. These use the existing pale oak door palette; its lattice door artwork is unchanged. The cooking table's cutting board continues to use the original plank material.

This patch changes textures and their model bindings. It retains all existing geometry, sink-basin clearance, quartz worktops, block IDs, recipes, inventories, kitchen connections and appliance designs. Existing placed blocks adopt the new appearance when the updated mod loads.

## Changes in 0.2.0

- Rebuilt the wood furniture around clean Farmer's Delight cabinet fronts: eleven credited species designs plus an original pale oak lattice design. This replaces the shared raised-frame fronts and common gold pulls.
- Kept the approved oven, refrigerator and toaster model assets exactly as in 0.1.0. The quartz countertop texture is also unchanged.
- Recessed the sink basin behind the cabinet doors, leaving one model pixel of clearance. Removed the slab over the bowl, moved the internal shelf below it, and fitted the native animated water mesh inside the new basin. The faucet uses the same white ceramic material as the sink.
- Rebuilt the catalog renderer with a per-pixel depth buffer, so hidden basin faces cannot be painted over the closed doors.
- Kept all block IDs, recipes, native block entities and inventory serialization. Replace the older JAR with this version; do not install both versions together.

## Included

Each of **oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, bamboo, crimson, warped, and pale oak** has a cooking table, kitchen counter, wall cabinet, sink, corner, and hanging corner. Pale oak is enabled when Vanilla Backport is installed. That is 72 wood furnishings with Backport, or 66 without it.

**Ivory, sage, blush, and charcoal** each have an oven, refrigerator, and toaster: 12 appliances, for **84 blocks in your pack**.

The JAR includes 33 unmodified Farmer's Delight cabinet textures under its MIT License, with attribution and the full license, plus three original pale oak textures: front, side and top/bottom. The remaining materials reference Minecraft textures at runtime. No Cozy Home, Cluttered, Cooking for Blockheads, or Minecraft artwork is bundled. Farmer's Delight is not a runtime dependency. See `THIRD_PARTY_NOTICES.md` for provenance. The preview catalog renders these exact model files; Minecraft lighting and resource packs can change their appearance.

## Install

1. Use Minecraft **1.20.1**, Forge **47.2.0 or newer in the 47.x line**, and Java 17.
2. Keep **Cooking for Blockheads 16.0.15** and **Balm 7.3.43 or a later 7.x version** installed. This build intentionally restricts CFB to 16.0.15 because its internal model and block entity APIs are used.
3. Keep **Vanilla Backport** and its **Platform** dependency installed to enable pale oak. The add-on references `minecraft:pale_oak_planks`, the ID provided by your Backport version.
4. Replace the old add-on with `cozy-kitchens-forge-1.20.1-0.2.1.jar` in `mods`. Install the same version on a multiplayer server and every client.
5. Open the **Cozy Kitchens for Blockheads** creative tab, or search JEI for `@cozykitchens`.

Try the test build in a copy of your world before using it in your main save. Exact full-pack validation, shaders, and visual approval still need your ATM9 installation.

## Survival crafting

Craft the regular Cooking for Blockheads piece first. In a crafting grid, combine it with **one plank of the desired wood** to create that furnishing. You can also use another Cozy Kitchens wood version as the input to change its finish.

For appliances, combine the original appliance (or a Cozy Kitchens appliance of that kind) with **white, green, pink, or black dye** for ivory, sage, blush, or charcoal. Finish changes use a crafting grid. Right-click recoloring is disabled on the variants so the selected wood or enamel finish stays consistent.

Crafting operates on items. Empty storage and remove upgrades before breaking and refinishing a placed kitchen. Breaking uses the native CFB inventory and upgrade drop behavior.

## Kitchen behavior

The variants instantiate **the actual Cooking for Blockheads block entities**, so its inventories, menus, recipe discovery, fluids, oven fuel/energy, upgrades, automation capabilities, and configuration remain responsible for functionality. A variant counter has the same configured capacity as an original counter; ovens and refrigerators use the original upgrades. Toasters retain CFB's normal behavior.

Mix wood types and combine these pieces with original CFB kitchen blocks. Corners and hanging corners carry kitchen connections. Stack two matching refrigerators to make the large refrigerator. Removing either half returns the survivor to the small model. The lower refrigerator drives the pair's animated door finish, so use the same enamel finish for both halves.

CFB still drives door movement, visible stored items, oven tools, cooking-book display, sink water level, and toast. The add-on routes animated door and sink-water meshes to the matching finish and handles texture reloads. Cabinet fronts use Farmer's Delight's paired-panel artwork on the native CFB hinged door assembly.

The add-on supplies new functional kitchen blocks in its own tab. Farmer's Delight supplies the credited wood artwork, while the original coordinated appliance geometry takes inspiration from Cozy Home and Cluttered.

## Build from source

Install a Java 17 JDK and run:

```sh
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

The output is under `build/libs`. ForgeGradle downloads Minecraft and dependency files on the first build. Production resources are already included; Python is not required to build.

To regenerate the model and crafting resources with Python 3:

```sh
python tools/generate_resources.py
```

To re-export the original pale oak vector artwork, run `python tools/create_pale_cabinet.py` (requires Pillow). The editable front, side and top/bottom SVGs are in `art/`. The other cabinet PNGs are bundled, unchanged source artwork; their origin and hashes are recorded in `art/farmers-delight-provenance.json`.

The resource verifier and catalog renderer additionally need the vanilla client JAR and the reference Backport JAR at the paths shown in those scripts. They are development inputs, not redistributed dependencies. The catalog renderer uses Pillow and NumPy.

Isolated Forge tests (not included in the production JAR):

```sh
./gradlew -PwithGameTests runGameTestServer
./gradlew -PwithGameTests -PwithBackport runGameTestServer
```

The development-only client model check is available with `-PwithClientTests runClient`. It verifies block, inventory, and animated door meshes after resources load, then closes the client automatically. Add `-PwithBackport` to include pale oak.

To reproduce companion-mod checks, put the official `cozy_home-3.0.4-forge-1.20.1.jar` in `vendor/` and add `-PwithFurniture`. This additionally fetches Cluttered 3.0.3. Reference mod JARs are not included in the source archive.

After running tests, clean before building the production JAR, so test-only classes and structures are excluded:

```sh
./gradlew clean build
```

## Integration details

The add-on extends CFB's native block classes and merges its blocks into the native block entity types' supported-block sets during common setup. One Forge access transformer opens the `validBlocks` field; no mixins are included. Original blocks and additions already present in those sets are retained.

Client-only door and water routing wraps Balm's deferred model suppliers. It delegates to the original supplier on every resource reload and uses the original mesh for original CFB blocks. State, inventory serialization, capabilities, and recipe logic remain in CFB.

CFB and Balm are required external dependencies and retain their own licenses. Farmer's Delight artwork is redistributed under MIT with attribution. Cozy Home and Cluttered are artistic references and optional companion mods. This is an independent add-on, not an official release by their authors. Original add-on code, geometry and pale oak artwork are MIT licensed.
