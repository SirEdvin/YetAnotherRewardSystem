## Context

KubeJS startup scripts currently create `RewardShopBlock` instances whose registry IDs identify the shop. Server scripts register staged or dynamic trades for those IDs, and the block opens a vanilla merchant screen backed by per-player completion history. The block has no block entity or inventory, and transactions occur only through the merchant menu.

An automatic box must persist a selected trade, payment items, produced items, and completion counts at a world position. It must expose loader-specific item automation without moving trade resolution or inventory correctness out of shared code. YARS must continue to ship no concrete gameplay blocks or assets.

## Goals / Non-Goals

**Goals:**
- Register automatic reward boxes exclusively through a KubeJS startup-script block type on Fabric and Forge.
- Reuse shop-scoped `RewardShopTrade` definitions, including fixed, staged, limited, and dynamic trades.
- Provide a vanilla-style trade list where a player selects, rather than manually executes, one trade.
- Execute transactions atomically from persistent block inventory and retain block-local progress across saves and selection changes.
- Support standard item insertion and output extraction on both loaders without duplication or loss.
- Cover shared behavior with unit tests and gameplay/integration behavior on both loaders.

**Non-Goals:**
- Redstone controls, filtering beyond the selected trade, recipes, energy costs, ownership, access control, or sided configuration.
- Sharing per-player reward-shop history with an automatic box.
- A custom screen, remote access, unattended trade selection, or built-in blocks and assets.
- Migrating automation state between different block IDs or recovering state after a pack removes its custom block.

## Decisions

### Register a separate KubeJS block type with persistent state

The KubeJS plugin will register `yars:automatic_reward_box` as a block builder type. `event.create('automated_rewards', 'yars:automatic_reward_box')` will create the specialized block and its block-entity support, use `kubejs:automated_rewards` as the shop ID, and retain normal KubeJS block presentation methods. Trade registration validation will accept both manual reward shops and automatic reward boxes as shop targets.

The automatic box is a separate type rather than an option on `yars:reward_shop`: inventory and block-entity semantics materially differ, while separate types keep manual shops stateless and avoid conditional behavior throughout their transaction path.

### Use a selection-only vanilla merchant screen

Interacting with the box will open a server-authoritative merchant menu using the block display name and the currently resolvable offers for that box's shop ID. Selecting an offer records its stable trade ID in the block entity. Payment and result transfer through the merchant menu will be disabled; the screen exists only to inspect and select trades. The menu will mark the current selection when opened and reject selections that no longer correspond to a registered, resolvable trade.

Reusing the merchant screen keeps trade presentation consistent with manual shops and avoids a custom client screen. Attempting to adapt ordinary merchant completion was rejected because its payment slots are player-owned transient menu state rather than automation-accessible block inventory.

### Store inputs, outputs, selection, and per-trade counts in the block entity

Each box will persist two payment slots, a small output inventory, the selected trade ID, and completed purchase counts keyed by full trade ID. Counts are local to that placed box and remain when selection moves to another trade, allowing staged progression to resume when reselected. Missing, malformed, or negative persisted counts resolve as zero.

Two payment slots mirror the maximum cost shape already supported by `RewardShopTrade`; output slots are extract-only to automation. Inputs are never discarded on selection change, so players can recover mismatched or obsolete payment items through the UI or block removal. Breaking the block drops all inventory contents using normal container behavior.

Per-player history was rejected because automated insertion has no reliable player identity. A single global count per selected trade was rejected because changing away and back would reset or merge unrelated staged progress.

### Execute synchronously and atomically when relevant state changes

After successful insertion, selection, load completion, or trade-registry replacement, the server resolves the selected trade at its block-local completed count and loops while all of the following hold: required costs match and are available, the complete result fits in output storage, and the trade still resolves. Each iteration simulates output insertion first, consumes both costs only after capacity is confirmed, inserts the result, increments the count, and resolves the next stage.

Matching uses the same item-and-component semantics as merchant costs. Invalid dynamic resolvers, removed trades, exhausted limited trades, insufficient costs, and full outputs stop processing without consuming items. A reentrancy guard prevents inventory callbacks caused by the transaction from recursively starting another transaction.

A world tick loop was rejected because boxes only need work after discrete state changes. Change-driven execution is cheaper and deterministic while still draining all immediately possible trades.

### Keep inventory rules shared and bridge only loader APIs

Shared code owns slot validation, simulation, mutation, serialization, menu behavior, and transaction execution. Forge exposes that inventory through its item capability and Fabric through its transfer/storage API. External insertion targets payment slots and accepts only stacks matching either cost of the currently selected, currently resolvable trade; extraction targets output slots only. Player interaction permits recovering payment and output contents.

If the selected trade changes or reloads, already stored payments remain recoverable but new insertion follows the new resolved costs. Loader adapters delegate to the same shared methods so both platforms enforce identical acceptance and atomicity rules.

### Treat trade reload as invalidation, not destructive migration

The trade registry will notify loaded automatic boxes, or open/use/insertion paths will observe its generation, so a successful reload re-resolves selection and attempts processing. If the selected trade disappeared, became exhausted, or throws while resolving, the ID and inventory remain stored but no transaction occurs. Reintroducing a valid trade with the same ID resumes from the retained count. Open selection menus rebuild or reject stale selection server-side.

Retaining the ID avoids silently changing player intent or deleting progress. Automatically selecting a replacement was rejected because offer ordering is not stable identity.

## Risks / Trade-offs

- [KubeJS-created blocks need block-entity registration on both loaders] -> Follow the installed KubeJS block-entity builder/registration lifecycle and verify startup on both development servers before adding custom registration machinery.
- [A dynamic resolver can throw or change cost during reload] -> Resolve defensively for every iteration and leave inventory untouched when resolution fails.
- [Loader item APIs have different transaction models] -> Implement simulate/commit behavior in shared inventory operations and keep loader adapters thin.
- [Selection-only behavior relies on vanilla merchant packets] -> Validate every selected offer index and trade ID on the server and add client GameTests for selection without item transfer.
- [Large input quantities can execute many trades in one callback] -> The loop is naturally bounded by finite payment inventory and output capacity; add a fixed safety ceiling only if profiling or a reproducible resolver edge case demonstrates a need.
- [Changing selection can strand old payment items] -> Keep payment slots player-accessible and drop all contents when broken rather than deleting or auto-routing them.

## Migration Plan

1. Add the shared block, block entity, selection menu, persistence, and transaction logic without altering existing manual shops.
2. Register the new KubeJS type and loader inventory bridges, then verify startup and automation on Fabric and Forge.
3. Document startup-script block creation and reuse the existing server-script `event.shop(...)` trade registration for the automatic box ID.
4. Existing worlds are unaffected until a pack adds the new startup-script block. Rollback requires removing placed automatic boxes first because older versions will not know their registry entries or state.

## Open Questions

None. Selection is block-global, progress is block-local per trade, payment storage has two slots, outputs are automation-extractable, and no ownership or redstone behavior is included.
