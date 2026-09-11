## Why

Manual testing currently requires KubeJS-created blocks and scripted trade fixtures. Bundle two immediately usable debug shops with predictable example trades and original pixel-written textures so both shop implementations and their shared player history can be exercised in an ordinary installation.

## What Changes

- Ship `yars:debug_reward_shop` and `yars:debug_automatic_reward_box`, their block items, and the automatic block entity through native Fabric/Forge registration rather than KubeJS builders or the test mod.
- Make both available in Creative inventory and through `/give`, without recipes, world generation, or a debug configuration gate. Creative-only describes acquisition; placed shops retain normal interaction and ownership rules so Survival transactions can be tested.
- Include a native example catalog containing shared and shop-specific trade IDs, covering unlimited and limited purchases, staged progression, dynamic costs/results, two costs, same-item costs, and NBT-sensitive stacks.
- Preserve bundled trades alongside scripted trades through startup and reload without changing player progress or weakening duplicate-ID validation.
- Ship distinct, simple block/item models and 16×16 PNG textures authored by explicit pixel writing with a reproducible local generator; no image-generation service.
- Document exact trade behavior, acquisition commands, and a manual testing checklist including automation, ownership, persistence, reload, and optional Jade presentation.

## Capabilities

### New Capabilities

- `bundled-debug-shops`: Native debug shop registration, Creative acquisition, built-in example catalog lifecycle, pixel-authored presentation, and a reproducible manual testing fixture.

### Modified Capabilities

None. There are no published capability files under `openspec/specs/` at proposal time. Earlier unarchived changes state that YARS ships no concrete blocks/assets; this change intentionally supersedes that restriction for these two debug fixtures only, without changing KubeJS builder or transaction contracts.

## Impact

Shared block/trade code and production resources in `projects/core/`; loader entrypoints and trade-reload integrations in `projects/fabric/` and `projects/forge/`; existing unit tests and Testiarium coverage; README and a small texture-generation tool. No new runtime dependency, custom screen, survival progression, history-reset command, or change to existing shop identities is proposed.
