# Verification — Cozy Kitchens 0.2.1

Validated on October 18, 2026 in an isolated Minecraft 1.20.1 / Forge 47.2.0 development environment with Java 17. This is a test release for the user's full ATM9 installation.

## 0.2.1 patch scope

This patch changes faucet materials and adds original pale oak side and top/bottom artwork. Its production Java classes are byte-identical to 0.2.0, checked against the prior release JAR during packaging. Existing cabinet-front PNGs, appliance assets, geometry, blockstates, item models and crafting data remain unchanged. The six server tests below were run for 0.2.0; the client, resource and production-package checks were run again for 0.2.1.

## Tested dependencies

| Dependency | Version |
| --- | --- |
| Cooking for Blockheads | 16.0.15 |
| Balm | 7.3.43 |
| Vanilla Backport, when enabled | 1.1.7.10 |
| Platform, when Backport is enabled | 1.3.4 |
| Cozy Home, companion run | 3.0.4 Forge |
| Cluttered, companion run | 3.0.3 Forge |

## Automated checks

All six Forge GameTests passed for 0.2.0 with Backport, Cozy Home, and Cluttered loaded (84 registered variants). The automated fixtures use the actual native CFB block entities and kitchen graph. Their functional implementation is unchanged in this texture patch.

| Check | What was verified |
| --- | --- |
| Every registered variant | Valid native CFB entity type; native entity save/reload; correct survival block drop. Hanging corners intentionally have no entity. |
| Storage and upgrades | Counter/cabinet contents, custom names, preservation upgrade, and fridge contents/ice/preservation state survive serialization. |
| Mixed kitchen | Original and variant storage connects through both corner types; sink, fridge and oven providers are found; a kitchen smelting request reaches the oven. |
| Refrigerator pairs | Two blocks form a 54-slot fridge; removing either half restores the survivor's small model. |
| Oven operation | A variant oven consumes normal fuel and produces a baked potato. |
| Toaster completion | A saved active toaster with two ticks remaining resumes, ejects its result, clears its slots, and stops. |

The real Forge client was launched with Backport, Cozy Home, and Cluttered. It verified all **84** block meshes and item meshes, both counter/cabinet hinge directions, oven normal/active doors, and all six fridge door meshes per finish. It checked that animated wood doors contain each species' cabinet-front sprite, that appliances retain the correct enamel sprite, and that all twelve sinks select the fitted water mesh. Original CFB counter-door and sink-water models retain their original quads. The client reloaded its resource packs and repeated these checks successfully (`COZY_MODEL_QA_INITIAL_PASS` followed by `COZY_MODEL_QA_PASS`). No missing sprites were found by those checks.

The 0.2.1 client check additionally verifies the expected gold or black faucet sprite on every sink, plus the new pale oak side and top/bottom sprites on every pale oak furnishing, before and after the resource reload.

The resource verifier resolved **312** model JSON files against the actual bundled, Minecraft and Backport texture files, plus all 84 crafting recipes, blockstates, item models, and loot tables. Optional pale oak recipes and data remain gated on Backport.

## Model revision checks

- All twelve wood fronts use distinct texture files. Eleven use unmodified Farmer's Delight art; pale oak uses the included original SVG artwork. Source paths and SHA-256 hashes for the 33 third-party PNGs are recorded in `art/farmers-delight-provenance.json` and checked during packaging. The JAR includes the complete third-party MIT License and notices.
- For every sink, all five basin cuboids have no positive-volume intersection with the closed door. The basin's nearest face is one model pixel behind the back of the door.
- Sink water remains within the inner bowl at six fill fractions, including full and empty, after the native renderer's y=8 anchoring and height scaling. Neither the bowl nor water intersects the cabinet door. The shelf is below the bowl; the countertop has a real opening.
- The quartz countertop and white basin materials are unchanged. Only the three faucet cuboids per sink use the specified gold or black texture.
- Six faucet sets use gold: spruce, jungle, dark oak, mangrove, cherry and crimson. Six use black: oak, birch, acacia, bamboo, warped and pale oak.
- Pale oak uses three original 16×16 PNGs exported from the included SVGs. Side and top/bottom textures use the lattice front's palette and custom joinery/grain. All six pale oak furnishing models bind these custom side and end materials; cabinet doors are unchanged.
- All **96** appliance model, blockstate and item-model JSON files are byte-identical to the 0.1.0 release. Appliance geometry and material choices were preserved.
- The catalog now uses per-pixel depth testing. The sink preview is pixel-identical with its element order reversed, verifying that hidden faces cannot reappear because of element sorting. The revised full catalog and a large pale oak sink detail were visually inspected.

The production package is built with `clean build` without either test flag. The packager checks that no test classes, test structures or dependency JARs enter the distributable, that all 84 block entries and optional loot tables are present, and that the native block-entity access transformer field reference is reobfuscated.

## Limits

These are integration and model checks in a small mod set. They do not establish full ATM9 compatibility, a multiplayer GUI playthrough, every automation mod's behavior, shader/resource-pack appearance, or final in-world aesthetic approval. The full pack should be tried in a copy of a world.

The combined companion-mod run logged missing/obsolete resource references in Cozy Home and Cluttered. Passing here means the add-on checks passed; the combined log was not entirely free of companion-mod warnings/errors. No companion resources were patched by this add-on.

The catalog is an orthographic render of the generated models with their actual referenced textures, not an in-game screenshot.
