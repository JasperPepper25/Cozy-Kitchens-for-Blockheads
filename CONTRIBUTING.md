# Development

Use Java 17 and the committed Gradle 8.8 wrapper. Required development dependencies are fetched from their official Maven sources by ForgeGradle. Do not commit Minecraft, CFB, Balm, companion-mod JARs, local saves, logs, or dependency caches.

## Build and checks

```sh
./gradlew clean build
python3 tools/package_release.py
```

The packager checks production resources, artwork hashes and licensing, reobfuscation, and exclusion of test classes. It writes the JAR, source ZIP, installation guide and verification summary to the ignored `dist/` directory. The workflow uploads this directory as a build artifact.

The development-only Forge tests exercise native block entities, survival drops, storage/upgrades, kitchen connections, refrigerator pairs, oven cooking, and toaster completion:

```sh
./gradlew -PwithGameTests runGameTestServer
./gradlew -PwithGameTests -PwithBackport runGameTestServer
```

The client check verifies block/item meshes, baked UV bounds, blue water tint, paired-door pivots and renderer registration, materials and native fallbacks before and after a resource reload. It then creates a disposable world, opens native and variant cabinets, checks synced contents and saves six screenshots:

```sh
./gradlew -PwithClientTests -PwithBackport runClient
```

The client check closes Minecraft automatically after success; screenshots are in `run/screenshots/`. It uses night vision for inspection lighting. Linux CI runs it through `xvfb-run` and software OpenGL. Run it on a desktop with OpenGL support. Companion checks additionally use `-PwithFurniture` and the official Cozy Home 3.0.4 JAR in the ignored `vendor/` folder, as described in [the guide](docs/INSTALL.md).

Always run `clean build` without test flags before distributing a production JAR. Test flags add their sources to the main source set and must not be used for releases.

## Models and artwork

- Edit the model/recipe generator in `tools/generate_resources.py`, then regenerate committed resources.
- Pale oak's original editable SVGs live in `art/`; export them using `python3 tools/create_pale_cabinet.py` with Pillow installed.
- Ceramic finishes have editable SVG swatches in `art/`; export with `python3 tools/create_ceramic_textures.py`. Keep their palette matched to the previous finishes.
- Keep the 33 credited Farmer's Delight PNGs unchanged and retain their MIT License and provenance. Document the source and license of any additional artwork.
- Install Pillow and NumPy, then run `python3 tools/prepare_reference_assets.py` for checksum-verified Minecraft/Backport development inputs. These JARs are not committed.
- Run `python3 tools/render_catalog.py` and copy `dist/Cozy-Kitchens-Catalog.png` to `docs/Cozy-Kitchens-Catalog.png` after a visual revision.
- `tools/verify_resources.py` uses those same reference JARs to check texture resolution, recipes, geometry clearance, water bounds, enclosed corners, overlapping outward faces, UV bounds, ceramic color matching and all 36 accepted cabinet texture hashes (`art/approved-cabinet-textures.json`).

Preserve registry IDs and native CFB inventory serialization across updates. Add regression coverage for functional changes; appearance changes should include a regenerated catalog and client inspection. Describe actual test scope in `VERIFICATION.md` and each change in `CHANGELOG.md`.

## Reports and ideas

Use the repository's issue forms. For rendering problems, include screenshots, the mod and loader versions, resource packs/shaders, and a minimal reproduction. Remove personal information from logs before sharing them. Design requests should identify the affected wood or appliance set and the intended style.
