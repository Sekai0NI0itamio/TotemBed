# TotemBed — Totem Stasis for Minecraft Fabric 1.21.1

Bind a **Totem of Undying** to a bed. When that totem saves you from death,
vanilla mechanics wake you up at your linked bed.

- **Minecraft:** Java Edition 1.21.1 · **Loader:** Fabric · **Java:** 21
- **Mod id:** `totembed` · **Enchantment:** `totembed:totem_stasis` (max I)

## How it works

1. Hold a normal Totem of Undying in your hand.
2. Right-click a valid bed (either half). Server-side only.
3. The held stack is bound: custom item data stores the bed's **head position**
   plus the **dimension id**, and the **Totem Stasis** enchantment (level I) is
   applied to that stack only. Feedback: enchantment-table sound, enchant
   particles, action-bar message with coordinates + dimension.
4. Later, when **that specific totem** activates (`LivingEntity.tryUseTotem`
   mixin observes vanilla activation without replacing it), vanilla survival
   effects happen first (health restore, status effects, totem consumption).
5. One server tick later (outside the damage callback), the mod validates the
   destination and runs the bed-return sequence:
   - Validate dimension id, world availability, build limits, chunk access.
   - Confirm a complete bed (head + foot) still exists.
   - Refuse sleep paths in dimensions where beds explode
     (`BedBlock.isBedWorking` — Nether/End) and use only the safe vanilla
     exit-position calculation there.
   - Teleport across dimensions if needed.
   - Call vanilla `PlayerEntity.trySleep(headPos)`, then vanilla
     `wakeUp(...)`, and position the player at vanilla's
     `BedBlock.findWakeUpPosition(...)` exit (orientation + surroundings aware).
   - Never leave the player sleeping; on any failure the totem survival
     stands and the return is skipped with a message + log line.

The visual impression: you pop a totem, then wake up at your bed.

## Binding rules

- Only a Totem of Undying in the interacting hand binds; anything else keeps
  vanilla bed behavior.
- Only bed blocks bind; other blocks do nothing.
- Head/foot clicks both resolve to the **head** position via facing math.
- Malformed/unidentifiable beds write nothing.
- One stack only — the rest of the inventory is untouched.
- No items consumed, bed unmodified. Server-authoritative, once.

## Failure policy

- Destroyed/replaced bed, bad data, missing dimension, unloaded chunk:
  totem still saves you; bed return is skipped with feedback.
- Nether/End: no sleep attempt (no explosion possible); safe vanilla exit
  positioning only, when available.
- Duplicate scheduling, recursive damage, second death during the sequence are
  guarded (in-flight set + 20-tick dedupe + alive checks).

## Multiplayer / dedicated server

- All logic is server-side (`UseBlockCallback` server branch, server tick
  scheduler, `ServerPlayerEntity` APIs). Clients are never trusted for
  coordinates and never required to approve the destination.
- The mod must be installed on the **server**. Clients need it only for the
  tooltip lines; gameplay works without it client-side.
- Disconnect/shutdown: queued work is keyed by player UUID and validated
  (`isRemoved`, `isDead`) before execution; nothing leaks across restarts.

## Enchantment + data

- Data-driven enchantment `data/totembed/enchantment/totem_stasis.json`
  (supported item: `minecraft:totem_of_undying`, slots: hand).
- Custom component `totembed:totem_stasis` (`TotemStasisData`: dimension +
  `BlockPos`, Mojang codec + packet codec) rides on the stack through
  inventories, chests, and normal stack operations.
- Tooltip shows `Bound: <dim> x, y, z`; enchantment name renders via vanilla.

## Validation (all in GitHub Actions; never local)

- `./gradlew build` — compile, remap, unit tests.
- `./gradlew test` — JUnit: data well-formedness, codec round-trip, bed-half
  math, tooltip policy, and data-file wiring (enchantment JSON, metadata,
  mixin config, translations).
- Dedicated-server boot (`./gradlew runServer` with agreed EULA): the `server`
  CI job boots a real 1.21.1 dedicated server with the mod and asserts it
  reaches `Done (...)!` with our init line and no mixin/entrypoint failures.
- `TotemBedGameTest` (`@GameTest`) compiles with the mod and runs in any
  standard GameTest harness; Loom 1.7.4 provides no `runGametest` task, so
  GameTest execution is currently a manual verification item (see below).

See `.github/workflows/build.yml` for the exact CI both jobs run in.

## Known limitations

- `trySleep` can reject (e.g. daytime, too far, occupied): the mod then falls
  back to vanilla `findWakeUpPosition` teleport rather than forcing night.
- Cross-dimension return requires the destination world to be loaded/available
  on the server; otherwise the return is skipped safely.
- If the vanilla exit search finds no safe spot, the player stays where the
  totem saved them (alive, awake) with a message.

## Outstanding manual verification

Not automated in CI (no interactive client available there):

- Exact wake orientation/exit choice with crowded bedrooms and obstructed beds.
- End-to-end bind → die → wake sequence on a live server, including
  cross-dimension returns and Nether/End beds.
- `TotemBedGameTest` GameTests under a GameTest harness (`runGametest` is not
  provided by Loom 1.7.4).
- Heavy-lag double-activation timing and multiplayer edge cases.

## Build from source

Requires Java 21. All validation runs in CI:

```bash
./gradlew build
./gradlew test
./gradlew runGametest
```

Do not publish releases or upload to Modrinth/CurseForge without permission.
