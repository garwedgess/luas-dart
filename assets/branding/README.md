# Transit app icon

The launcher uses a flat sky-blue rail vehicle on navy, with green and red rails referencing the Luas lines. The production artwork uses solid fills and transparent cutouts; it has no gradients, textures or shadows.

- `transit-icon-concept.png`: original concept generated with the built-in image_gen tool.
- `transit-icon.svg`: editable flat vector source, redrawn from that concept.
- `transit-icon-preview.png`: 512 px preview rendered from the flat SVG.
- `app/src/main/res/drawable/ic_transit_launcher_foreground.xml`: Android vector foreground with adaptive-icon safe-zone spacing. Its transparent background also supplies the themed monochrome mask.
- `mipmap-anydpi` and `mipmap-anydpi-v26`: legacy and adaptive launcher resources, including round icons.

## Final generation prompt

```text
Use case: logo-brand
Asset type: production Android launcher icon for an existing Dublin Luas and DART transit forecast app.
Primary request: one distinctive, exceptionally clean FLAT app icon, simple enough to read at 48 pixels.
Scene/backdrop: uniform solid deep navy (#0C0F17) filling the entire square canvas, edge to edge. No outer rounded-square frame; Android supplies the launcher mask.
Subject: a single bold front-facing urban rail vehicle symbol that reads as both a tram and a commuter train. Sky blue (#88CCFF) main silhouette, a broad navy windscreen, two simple round headlights, and two short outward-splaying rails below; one rail accent green and the other rail accent red to subtly reference the two Luas lines. One minimal pantograph above the vehicle. Calm geometric proportions, generous negative space, strong recognizable silhouette.
Style/medium: genuinely flat vector-style graphic, perfectly solid color fills, sharp clean antialiased edges, restrained rounded corners. No textures, gradients, lighting, shine, bevels, long shadows, drop shadows, 3D, perspective, or decorative detail.
Composition/framing: square 1024 by 1024 production asset, centered symmetric blue vehicle with balanced colored rail accents. ALL meaningful artwork including pantograph and rails fits within the central 60 percent of the canvas, with 20 percent clean navy margin on every edge, so circular and squircle adaptive icon masks cannot crop the artwork. No tiny strokes or fussy details. One icon only, no sheet or mockup.
Text: no letters, words, labels, numbers, watermark, or logos of transit operators.
```
