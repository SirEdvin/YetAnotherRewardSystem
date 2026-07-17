## Context

YARS currently registers one concrete `yars:reward_shop` block during mod initialization. Interacting with it opens a `RewardShopMerchant`, which reads one process-wide `RewardShopTrades` list populated by server-script registration. Trade history is keyed only by trade ID. The block's models, texture, loot table, translations, data providers, tests, and creative-tab icon all assume that built-in block exists.

KubeJS supports custom block registry types by registering a `BlockBuilder` subtype with `RegistryInfo.BLOCK`. The referenced GT True Steam cooling-coil integration uses this pattern: startup scripts create a typed block, the builder creates the specialized runtime block, and ordinary KubeJS builder methods remain available for pack-controlled properties and assets.

## Goals / Non-Goals

**Goals:**
- Make KubeJS startup scripts the only source of reward-shop blocks.
- Use each created block's resource location as a stable shop ID without requiring duplicate identity configuration.
- Let server scripts register independent trade sets for each shop ID and replace them safely on reload.
- Preserve the existing merchant transaction validation, staged trade behavior, and per-player limits while scoping history to both shop and trade.
- Remove all built-in YARS block registrations and block-specific generated or static resources.
- Support the same API and behavior on Fabric and Forge.

**Non-Goals:**
- Creating shops from server scripts, data packs, or configuration files.
- Adding built-in shops, trades, textures, models, recipes, or loot.
- Providing a custom shop screen, block entity, inventory, or economy system.
- Migrating placed `yars:reward_shop` blocks automatically to a pack-defined replacement.

## Decisions

### Register a `yars:reward_shop` KubeJS block type

The KubeJS plugin will register a reward-shop `BlockBuilder` subtype under the type ID `yars:reward_shop`. A startup script will use `event.create('shop_name', 'yars:reward_shop')` and may chain normal KubeJS block-builder methods for display name, hardness, model, texture, sound, and other presentation. `createObject()` will construct `RewardShopBlock` with the builder-created properties and the builder's block resource location.

The block resource location is also the shop ID. This avoids a second mutable identifier that could disagree with the registry key and naturally gives every registered shop a unique namespaced ID.

Alternative: retain a generic YARS block and configure its shop ID through block state or NBT. That still ships a mod block, complicates placement and synchronization, and does not use KubeJS's registry lifecycle.

### Scope server-script registration by shop ID

The existing `RewardShopEvents.trades` event will expose a nested callback API: `event.shop('kubejs:daily_rewards', shop => { shop.trade('daily_diamond')... })`. Repeated declarations of the same shop across callbacks or script files merge their trades in event execution order. Registration will produce an immutable map from shop resource location to its ordered immutable trade definitions. Trade IDs remain stable plain strings, need only be unique within one shop, and may be reused by other shops. A duplicate shop/trade pair fails registration with both identifiers in the error.

On server-script reload, registration is atomic: the complete map is replaced only after every declaration validates. A missing block ID or an ID that resolves to a non-reward-shop block fails the event registration with a clear script error, and any previously valid registry remains active. A valid reward-shop block with no currently registered entry opens an empty merchant interface.

Alternative: keep one global list and add an optional shop predicate to each trade. That makes duplicate handling, ordering, diagnostics, and reload semantics less explicit than a map whose structure matches the user-facing concept.

### Carry shop identity through merchant validation

`RewardShopMerchant` will receive the interacting block's shop ID and resolve offers only from that shop. It will use the interacted block's display name as the vanilla merchant-screen title, so standard KubeJS display-name customization controls both block and shop naming. Offer-to-trade tracking and stale-offer approval will use the shop's current definitions, so a trade from another shop cannot be accepted merely because it shares a trade ID or resolved stacks.

Alternative: resolve a shop once and copy unscoped offers into the merchant. Current transaction guards re-resolve definitions at completion, so retaining identity through that path is necessary for reload and stale-offer safety.

### Scope persistent history by shop and trade

Completed counts will use direct compound keys under `yars.reward_shop.v2.shops[<shop-id>].trades[<trade-id>]`. For example, the count for trade `daily_diamond` in shop `kubejs:daily_rewards` is stored at `yars.reward_shop.v2.shops["kubejs:daily_rewards"].trades["daily_diamond"]`. Code will access the full shop and trade strings as direct NBT keys rather than splitting dots, colons, or slashes into path components. This allows two shops to use the same trade ID without sharing limits or progression and gives integrations an exact bracket-access contract.

The existing global `yars.reward_shop.v1.trades.<trade-id>` data cannot be mapped reliably because it contains no shop identity. It will remain ignored rather than being assigned to an arbitrary new shop. Documentation will identify the new path as the integration contract.

Alternative: require globally unique trade IDs and retain the current history shape. That leaks an implementation restriction across otherwise independent shops and makes script composition unnecessarily fragile.

### Remove the built-in block and empty content hooks

Static registration, assets, loot, recipes, language entries, data-generator output, and tests tied to `yars:reward_shop` will be removed. The now-empty YARS creative tab will also be removed rather than inventing a non-block icon or collecting KubeJS-owned blocks indirectly.

The global `event.trade(...)` form is removed outright. No default shop, global fallback, or compatibility alias will be provided. Alternative: retain the built-in block or global trades temporarily. This directly conflicts with the requirement that the mod provide no blocks and would leave an ambiguous migration path.

## Risks / Trade-offs

- [Existing worlds contain `yars:reward_shop`] -> Treat removal as an explicit breaking migration and document that pack authors must replace placed blocks before updating if preservation matters.
- [Startup and server scripts have separate lifecycles] -> Resolve shop IDs after registries are available, fail registration atomically for invalid targets, and document that adding or removing blocks requires a full restart while trade edits support server reload.
- [KubeJS APIs differ subtly across loaders] -> Keep the builder and plugin in common code where supported and verify registration, startup loading, server reload, and interaction on both loader development servers.
- [History format changes] -> Version the new shop-scoped path and leave old data untouched; do not guess which custom shop should inherit global counts.
- [A reload occurs while a merchant menu is open] -> Re-resolve the shop/trade pair during transaction approval and reject stale offers before result transfer.

## Migration Plan

1. Pack authors add each reward-shop block in `startup_scripts` and restart so registry entries exist.
2. Pack authors update server scripts to register each trade under the intended block resource location.
3. Existing placed `yars:reward_shop` blocks are replaced before deploying the mod update if the world must retain a physical shop location.
4. Deploy the update; old global trade-history data remains inert and new purchases populate the shop-scoped version.
5. Rollback restores the built-in block and old scripts, but purchases made only in the new history version are not reflected by the old implementation.

## Open Questions

None. The nested callback API, merge order, validation behavior, title source, history path, and removal of the global API are fixed by this design.
