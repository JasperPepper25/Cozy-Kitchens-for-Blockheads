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

The client smoke check verifies block/item meshes, animated finishes, gold/black faucets, pale oak side/end sprites, native model fallbacks, and a resource reload:

```sh
./gradlew -PwithClientTests -PwithBackport runClient
```

The client check closes Minecraft automatically after success. Run it on a desktop with OpenGL support. Companion checks additionally use `-PwithFurniture` and the official Cozy Home 3.0.4 JAR in the ignored `vendor/` folder, as described in [the guide](docs/INSTALL.md).

Always run `clean build` without test flags before distributing a production JAR. Test flags add their sources to the main source set and must not be used for releases.

## Models and artwork

- Edit the model/recipe generator in `tools/generate_resources.py`, then regenerate committed resources.
- Pale oak's original editable SVGs live in `art/`; export them using `python3 tools/create_pale_cabinet.py` with Pillow installed.
- Keep the 33 credited Farmer's Delight PNGs unchanged and retain their MIT License and provenance. Document the source and license of any additional artwork.
- The exact-resource catalog renderer needs Pillow, NumPy, the Minecraft client JAR at `research/client.jar`, and the reference Backport JAR at the path in the script. These dependencies are not committed.
- Run `python3 tools/render_catalog.py` and copy `dist/Cozy-Kitchens-Catalog.png` to `docs/Cozy-Kitchens-Catalog.png` after a visual revision.
- `tools/verify_resources.py` uses those same reference JARs to check texture resolution, recipes, geometry clearance, and water bounds. Optional comparison JARs under `research/` enable preservation checks against earlier releases.

Preserve registry IDs and native CFB inventory serialization across updates. Add regression coverage for functional changes; appearance changes should include a regenerated catalog and client inspection. Describe actual test scope in `VERIFICATION.md` and each change in `CHANGELOG.md`.

## Reports and ideas

Use the repository's issue forms. For rendering problems, include screenshots, the mod and loader versions, resource packs/shaders, and a minimal reproduction. Remove personal information from logs before sharing them. Design requests should identify the affected wood or appliance set and the intended style.
