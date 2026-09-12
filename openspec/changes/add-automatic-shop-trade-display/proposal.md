## Why

Players cannot identify an automatic shop's selected trade without opening its owner-only interface. A small in-world display above the shop will make its current payment requirements and result visible at a glance.

## What Changes

- Render the selected, currently resolvable trade above each automatic shop: first payment item, optional second payment item, and result, with numeric quantities and clear payment-to-result ordering.
- Add an owner-controlled button in the automatic shop UI to enable or disable the display for that shop. Persist the setting; default to enabled for new shops and existing saves without the setting.
- Synchronize server-resolved display stacks to nearby clients without requiring an open menu, including changes from selection, shared purchase progress, and script reloads.
- Hide the display when disabled or no selected trade resolves, without changing trading, inventory, or ownership behavior.
- Support both bundled and KubeJS-created automatic shops on Fabric and Forge, with client-only rendering and no new dependencies.

## Capabilities

### New Capabilities

- `automatic-shop-trade-display`: World-space selected-trade items and quantities, authoritative synchronization, and a persistent owner-controlled display toggle.

### Modified Capabilities

None. This additive presentation capability builds on the existing automatic-reward-box behavior documented in earlier changes; no main specs have yet been materialized under `openspec/specs/`.

## Impact

- Shared automatic shop block entity, menu, and screen; a shared client renderer and localized button labels.
- Fabric and Forge client renderer registration, including dynamically registered automatic shop block entity types.
- Minimal server-to-client display snapshot updates and narrowly scoped shared-history invalidation if needed to prevent stale staged-trade quantities.
- Shared unit tests and cross-loader client GameTests, plus dedicated-server compatibility checks.
- No KubeJS API changes, new dependencies, physical item entities, or changes to payment consumption, output storage, or trade history semantics.
