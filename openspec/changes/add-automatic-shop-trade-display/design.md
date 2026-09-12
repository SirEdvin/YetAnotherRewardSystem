## Context

See `proposal.md` for motivation and `specs/automatic-shop-trade-display/spec.md` for behavior. This is a cross-cutting client rendering, persistence, synchronization, and menu change on Minecraft 1.20.1.

- `AutomaticRewardBoxBlockEntity` persists inventory, owner UUID, and selected trade ID. Its `currentTrade()` resolves against server-only `RewardShopTradeHistory`. It has no display snapshot or block-entity update packet implementation today.
- `setChanged()` also invokes trade processing. Presentation changes must not accidentally become new transaction triggers.
- `RewardShopTrades` in `RewardShopTrade.kt` already provides weak reload listeners. Shared history increments in `RewardShopTradeHistory` currently have no notifications; both automatic and interactive purchases call that method.
- `AutomaticRewardBoxMenu` already synchronizes selection through a `DataSlot`; its screen subclasses vanilla `MerchantScreen` and places six storage slots across the middle.
- `YarsFabricClient` and `YarsForgeClient` register the custom screen but no block entity renderer. Bundled shops have native registered types; each loader's `integrations/kubejs/RewardShopBlockBuilder.kt` creates types via `BlockEntityInfo`.

## Goals / Non-Goals

**Goals:** Keep game rules server-owned, reuse native menu and block-entity networking, share the renderer across loaders, and keep display updates event-driven and independent of trade execution.

**Non-Goals:** New script APIs, configurable layouts/animation, manual-shop displays, public inventory inspection, real item entities, or changes to trade processing triggers and history semantics. No new rendering/network dependencies.

## Decisions

### 1. Render a small camera-facing trade row with exact counts

Add a shared client-only `AutomaticRewardBoxRenderer` using the native item renderer and font. Position a compact row above the block center with first cost, optional second cost, then result; use a small plus/arrow separator to distinguish roles. Render one item model per component, preserve stack metadata, and place a legible numeric count beside each, including 1. Use world depth/lighting rather than through-wall labels; tune spacing, scale, and above-block culling during client validation.

Numeric text satisfies the user's preferred quantity presentation and bounds rendering work to two or three item models. Rendering one copy per unit is rejected because it is harder to count and scales poorly. Animated bobbing/spinning is unnecessary; exact cosmetic offsets are implementation details, not additional scope.

### 2. Synchronize a minimal server-resolved snapshot

Keep a persisted boolean (proposed NBT key `ShowTradeDisplay`) on the block entity, defaulting true when absent. Maintain a transient snapshot of copied first cost, optional second cost, and result; no resolver or history calls run on the client. Empty/disabled/invalid state explicitly clears prior displayed stacks.

Use native block-entity initial update tags and update packets for tracking clients. Serialize only the display flag and resolved stacks, not the full inventory, owner UUID, history, or server script data. Keep disk serialization and client update decoding distinct: applying a partial display tag must not call the full inventory-loading path and erase unrelated state. Recompute on the server after load/level availability and before initial tracking serialization as necessary. Compare stack identity, metadata, counts, and visibility before sending updates.

An open-menu-only offer packet cannot serve non-owner observers. Replicating global trades/history would expose unnecessary data and require client script evaluation, so both are rejected.

### 3. Refresh on the actual state transitions

Refresh after selection changes, completion of the existing processing loop, and successful definition/attachment replacement. Add a narrow, server-instance-scoped weak-listener mechanism to `RewardShopTradeHistory`, modeled after the existing reload listener pattern, carrying owner UUID and trade ID. Loaded boxes subscribe/unsubscribe through their level/lifecycle and filter for their current owner and selection. A matching external history increment refreshes presentation only; it must not invoke transaction processing in another box.

For a box already processing a batch, coalesce its display refresh until the loop exits to avoid publishing its own intermediate stages. Guard callbacks against reentrancy and removed/client-side boxes. Re-register lifecycle listeners on reload/revival as needed, and do not retain worlds or unloaded boxes through static strong references. Disabled displays can skip resolution but must resolve fresh when enabled. Dynamic resolver failures produce an empty snapshot through the same validity semantics as `currentTrade()`.

Periodic ticks/world scans and refresh-on-inventory-only are rejected: the former adds idle work and repeated script calls, and the latter leaves external shared-progress changes stale. This change does not promise continuous reevaluation of scripts whose output changes for unrelated external reasons; refresh triggers remain selection, trade progress, reload, load, and visibility changes.

### 4. Use a native menu button with server authorization

Add a boolean `DataSlot` to `AutomaticRewardBoxMenu` and retain the server-side block entity reference. Add a localized button in `AutomaticRewardBoxScreen` that reports on/off state, supports normal focus/narration, and does not overlap inventory slots or merchant offer navigation. Use vanilla container-button transport and a dedicated button ID through the menu's button handler; reject unknown IDs and check the current menu, backing block entity, and `canOpen(player)` on the server before changing state.

The setter must mark persistence dirty using the base block-entity behavior rather than the transaction-triggering override, then refresh/broadcast display state. The screen reads the authoritative data slot instead of assuming optimistic success. A custom position-targeted packet is unnecessary and would add another validation surface.

### 5. Register every automatic-shop type at client lifecycle time

Share rendering implementation in `projects/core/.../client/`; keep Fabric and Forge registration APIs in their existing client entrypoints. Provide minimal access to each `AutomaticRewardBoxBlock`'s actual registered entity type and enumerate registered automatic-shop blocks at the appropriate post-registration client lifecycle, deduplicating types. This includes bundled, test-mod, and KubeJS-created blocks without hardcoding IDs or constructing block entities to discover types.

Verify pinned KubeJS type availability and renderer registration order on both loaders during implementation, including resource reload. If registration ordering requires a loader-specific lifecycle adjustment, retain the same type-enumeration approach rather than adding client classes to common builders. Dedicated-server classloading must never reach renderer or GUI classes.

## Risks / Trade-offs

- Shared progress notifications can recurse or retain worlds → scope listeners to the history instance, filter owner/trade, clean up lifecycle subscriptions, and refresh presentation only.
- Partial network NBT can corrupt inventory if routed through disk loading → explicitly separate snapshot serialization/decoding and test inventory preservation.
- A cosmetic toggle can execute a trade via `setChanged()` → bypass the processing override for presentation-only dirtiness and test zero history/inventory changes.
- Extra dynamic resolver calls can be costly or fail → event-driven snapshots, no per-frame resolution, coalesced local batches, and empty display on invalid results.
- Registration may miss KubeJS types or be overwritten on resource reload → exercise actual scripted shops and resource reload on both loaders, not only bundled types.
- Text/model overlap and above-block culling cannot be established by compilation → use real client visual checks with two and three components, multi-digit counts, metadata-bearing models, camera movement, and nearby shops.

## Migration Plan

No migration command is needed. Missing `ShowTradeDisplay` means enabled; saved false remains false. Display snapshots are transient and rebuilt from existing authoritative trade state. Older versions ignore the additive setting, so rollback preserves inventory, ownership, and selection but loses the display feature and may discard its setting on resave. Upgrade server and clients together; mixed-version networking is not a goal.

Implementation belongs on a new feature branch under the apply workflow. No production code is changed by this planning change.
