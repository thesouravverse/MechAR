# Mech model goes here

Drop your mech as `mech.glb` in this folder. The app loads it from
`assets/models/mech.glb` at runtime.

## Where to get one for v1 (free, CC-licensed)

1. https://sketchfab.com/3d-models — filter by:
   - Format: glTF
   - License: CC-Attribution / CC0
   - Search: "mech robot"
2. Download `.glb` (single file, embeds textures).
3. Rename to `mech.glb`.
4. Drop it here. Commit. Push.

## Requirements

- Format: **`.glb`** (binary glTF). The `.gltf` + textures multi-file form
  works but is harder to ship.
- Try to keep < 10 MB so APK stays small.
- Y is up. Model's "feet" should be at y=0 so it sits on the floor.
- If your model is huge or tiny, the code in `MechARScene.kt` rescales it
  so its tallest dimension equals `MECH_SIZE.y` (~70 cm).

## What happens if this file is missing?

The app falls back to a red metallic cube placed on the floor — so the
build still runs, you just don't see a mech. Drop a `.glb` here and
the cube disappears.
