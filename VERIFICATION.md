# Verification — Cozy Kitchens 0.3.0

Checked in Minecraft 1.20.1 / Forge 47.2.0 with Java 17, Cooking for Blockheads 16.0.15 and Balm 7.3.43. Both base (78 variants) and Vanilla Backport 1.1.7.10 + Platform 1.3.4 (84 variants) profiles are covered. This build revises rendering and model geometry; native kitchen state and registry IDs are retained.

## Client and integrated-world checks

The real Forge client checks every registered block/item mesh and the animated oven/fridge parts, including all twelve wood sets when Backport is enabled. Before and after an actual resource reload it verifies:

- No missing texture sprites and no baked quad UVs outside their stitched atlas sprite. This catches the former faucet/burner texture bleeding above the normal block bounds.
- The blue `0x3f76e4` water-color handler exists on every sink and matches the original CFB sink. Each sink also selects its fitted water model.
- The paired counter/cabinet renderer is registered on the native entity types. Both leaf sprites have their wood-specific finish.
- Both door hinges remain fixed and both free edges swing outward at 0°, 30°, 60° and 90°. These assertions cover all four facings and both native flipped states, for every counter/wall-cabinet variant.
- Appliance doors use the new ceramic sprites; all six fridge door meshes per finish load. Gold/black faucet assignments and custom pale oak side/end materials are retained.
- Original CFB counter-door and water models retain their original quads.

After `COZY_MODEL_QA_PASS`, the client creates a new disposable flat world. It places native and variant counters, wall cabinets, sinks, both corner types, ovens, a toaster and a stacked sage fridge. It verifies native opening animation and synced shelf contents, then captures six real client screenshots. `COZY_WORLD_QA_PASS` marks completion. The test closes Minecraft automatically. Test classes and the disposable world are excluded from the production JAR.

The [screenshots](docs/screenshots/) show closed/open cabinetry, gold/black faucets and blue water, rear corner panels, oven backs and the stacked refrigerator. They are unedited captures using night vision for consistent inspection lighting, with no shaders. The orthographic [catalog](docs/Cozy-Kitchens-Catalog.png) is a separate exact-resource model preview.

## Native kitchen integration tests

All six Forge GameTests pass with and without Backport:

| Check | Verified behavior |
| --- | --- |
| Every variant | Valid native entity, native save/reload and correct survival block drop; hanging corners remain connectors without entities. |
| Storage/upgrades | Counter/cabinet inventory, custom name and preservation state; fridge inventory, ice and preservation state survive serialization. |
| Mixed kitchen | Original and variant storage connects through both corner types; sink/fridge/oven providers are discovered and a kitchen smelting request reaches the oven. |
| Stacked fridge | The pair exposes 54 slots; removing either half restores the small survivor model. |
| Oven | Normal fuel cooks a potato into a baked potato. |
| Toaster | A saved active cycle resumes, ejects its result, clears its slots and stops. |

## Resource and geometry checks

`python3 tools/verify_resources.py` passes:

- **361 model JSON files** resolve against bundled, Minecraft and Backport resources, with explicit UVs inside 0..16. No positive-area overlap exists between outward faces in the same plane, including inventory meshes.
- All **24 corner boxes** cover all six outside surfaces completely. Walls end beneath their caps, removing the former overlapping countertop faces.
- **48 independent door leaves** use the correct half of each cabinet-front sprite. Sink basin cuboids and water never intersect the closed fronts; the basin retains one model pixel of door clearance. Water stays inside the bowl at six native fill fractions.
- Each faucet has five connected solid components: collar, riser, arm, downspout and lever. Gold/black materials match the agreed six/six wood assignments.
- All **36 accepted cabinet PNGs** match their 0.2.1 SHA-256 hashes, including pale oak front, side and top/bottom art. White quartz worktops and basin material are retained.
- Four uniform 16×16 ceramic finishes match the rounded mean RGB of the old finish textures. No appliance model references a terracotta texture. Every external fridge cap and stacking band uses the corresponding finish; internal shelves remain white.
- All **84 recipes, blockstates, item models and loot tables** are present and resolve; optional pale oak recipes/data remain gated on Backport.

The production build runs `clean build` without test flags. The release packager checks reobfuscation of the block-entity field access, artwork provenance/licensing, the expected 41 PNGs and 361 models, all block data, and exclusion of test classes, test structures and dependency JARs.

The GitHub workflow repeats production/resource checks, server tests and software-rendered client/world checks for both profiles, and uploads their logs/screenshots as evidence.

## Scope

These checks cover the add-on in a small mod set and an automated integrated-world fixture. They do not establish behavior across every ATM9 mod, multiplayer GUI sequence, automation integration, shader or resource pack. Cozy Home and Cluttered were tested with the earlier build; they were not included in these 0.3.0 runs. Existing block IDs and stored data remain compatible, but the user's complete pack remains the final environment for appearance review.
