#!/usr/bin/env python3
"""Package only reviewed source and production output, excluding dependencies and test binaries."""
from pathlib import Path
import hashlib
import json
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
DIST.mkdir(exist_ok=True)
version = re.search(r"^version = '([^']+)'", (ROOT / 'build.gradle').read_text(), re.MULTILINE).group(1)
jar = ROOT / f'build/libs/cozy-kitchens-forge-1.20.1-{version}.jar'
assert jar.is_file(), 'Build the production JAR first'
with zipfile.ZipFile(jar) as z:
    names = z.namelist()
    assert z.testzip() is None
    assert 'META-INF/mods.toml' in names and 'META-INF/accesstransformer.cfg' in names
    assert 'LICENSE' in names
    assert 'THIRD_PARTY_NOTICES.md' in names and 'META-INF/licenses/FarmersDelight-MIT.txt' in names
    assert not any('/test/' in n or n.startswith('data/cozykitchens/structures') for n in names), 'Test code leaked into production'
    assert not any(n.startswith('assets/cozy_home/') or n.startswith('assets/cluttered/') for n in names)
    provenance = json.loads((ROOT / 'art/farmers-delight-provenance.json').read_text())
    expected_pngs = {f['bundled'].removeprefix('src/main/resources/') for f in provenance['files']}
    expected_pngs.update(f'assets/cozykitchens/textures/block/cabinet/pale_oak_cabinet_{part}.png'
                         for part in ['front', 'side', 'top'])
    expected_pngs.update(f'assets/cozykitchens/textures/block/ceramic/{finish}.png'
                        for finish in ['ivory', 'sage', 'blush', 'charcoal', 'dark'])
    assert {n for n in names if n.endswith('.png')} == expected_pngs
    for f in provenance['files']:
        assert hashlib.sha256(z.read(f['bundled'].removeprefix('src/main/resources/'))).hexdigest() == f['sha256']
    assert len([n for n in names if n.startswith('assets/cozykitchens/blockstates/') and n.endswith('.json')]) == 84
    assert len([n for n in names if n.startswith('data/cozykitchens/recipes/') and n.endswith('.json')]) == 84
    assert len([n for n in names if '/loot_tables/blocks/' in n and n.endswith('.json')]) == 84
    assert not any(n.startswith('data/cozykitchens/loot_tables/blocks/pale_oak_') for n in names)
    assert all(f'resourcepacks/pale_oak/data/cozykitchens/loot_tables/blocks/pale_oak_{k}.json' in names
               for k in ['counter','cabinet','cooking_table','sink','corner','hanging_corner'])
    core = z.read('dev/fineedge/cozykitchens/CozyKitchens.class')
    assert b'f_58915_' in core and b'validBlocks' not in core, 'Block entity field was not reobfuscated'
    assert 'dev/fineedge/cozykitchens/client/PairedDoorRenderer.class' in names
    assert len([n for n in names if n.startswith('assets/cozykitchens/models/') and n.endswith('.json')]) == 361

shutil.copy2(jar, DIST / jar.name)
shutil.copy2(ROOT / 'docs/INSTALL.md', DIST / 'Cozy-Kitchens-Install-and-Guide.md')

files = []
for folder in ['src', 'gradle', 'art', 'docs', '.github']:
    files.extend(p for p in (ROOT / folder).rglob('*') if p.is_file())
for name in ['build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','LICENSE','THIRD_PARTY_NOTICES.md','README.md','VERIFICATION.md','CHANGELOG.md','CONTRIBUTING.md','.gitignore','.gitattributes']:
    files.append(ROOT / name)
for name in ['generate_resources.py','generate_test_structure.py','create_pale_cabinet.py','create_ceramic_textures.py',
             'prepare_reference_assets.py','verify_resources.py','render_catalog.py','package_release.py']:
    files.append(ROOT / 'tools' / name)
source = DIST / f'Cozy-Kitchens-{version}-Source.zip'
with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        assert p.suffix not in ['.class','.jar'] or p.name == 'gradle-wrapper.jar'
        z.write(p, f'Cozy-Kitchens-{version}/' + p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(source) as z:
    assert z.testzip() is None
report = {'jar': jar.name, 'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
          'source': source.name, 'source_files': len(files), 'blocks': 84, 'wood_sets': 12, 'appliance_finishes': 4,
          'cabinet_pngs': 36, 'ceramic_pngs': 5, 'model_files': 361}
(DIST / 'build-verification.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
