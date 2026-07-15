## Context

YARS currently contains only registration and loader/KubeJS integration scaffolding. This change introduces its first gameplay block and needs one shared implementation for Fabric and Forge. Pack authors need to define the shop exclusively through KubeJS; the default block has no offers.

## Goals / Non-Goals

**Goals:**
- Register a reward-shop block that opens Minecraft's wandering trader-style merchant screen.
- Provide a reload-safe KubeJS registration surface for named trade definitions.
- Resolve each offer from the interacting player's own completed-trade count.
- Store those counts in persistent player NBT in a stable, UI-readable YARS namespace.
- Support fixed, capped, and staged or calculated costs and result stacks.

**Non-Goals:**
- Custom shop screens, block inventories, currencies, or economy integration.
- Built-in trade definitions or an in-game trade editor.
- Sharing trade limits or progression between players.
- Defining a generic scripting API beyond reward-shop trades.

## Decisions

### Reuse the vanilla merchant menu

The block interaction will open the vanilla merchant menu configured with wandering trader UI metadata rather than introducing a menu, container, or screen. The merchant adapter will construct the offers for the player opening the block and validate/complete transactions server-side.

Alternative: a custom menu provides more presentation control, but duplicates vanilla trade synchronization and conflicts with the requested UI reuse.

### Keep declarative trade definitions separate from player state

KubeJS registration produces immutable shared trade definitions identified by an explicit, stable trade ID. Each definition contains a sequence of purchase stages; a stage selects offered stack and one or two payment stacks for a completed-purchase index. A stage can have a finite number of uses or remain the final unlimited stage. The player's NBT stores only completed counts keyed by the stable trade ID.

Alternative: persist full generated offers on the block or player. This makes script reloads and changed definitions difficult to reconcile and stores data that can be recalculated.

### Model price changes as stages

The KubeJS builder will support a concise fixed stage plus count-limited stages. Repeating or progressive price patterns will be represented by a callback or stage-generation helper that resolves the stack for the purchase index, with its output validated as a legal non-empty item stack and vanilla-compatible count. This supports doubling, additive increases, and arbitrary item changes without a separate expression language.

Alternative: provide only multipliers and increments. That cannot express the requested emerald-to-iron progression without additional special cases.

### Enforce limits at offer resolution and completion

An exhausted trade will not be offered to that player. On a successful transaction, the server increments that player's count for the trade ID and refreshes the player-specific offers. The transaction path must re-check the current count before recording it to prevent stale client offers from bypassing limits.

Vanilla calls `Merchant.notifyTrade` only after removing the result, so the shared implementation uses a narrow common mixin at the merchant result-slot removal and quick-move entry points. The guard approves only the exact currently resolved reward-shop offer before vanilla can transfer its result or consume payment. The post-trade callback consumes that approval, records the purchase, and refreshes offers. This requires no loader-specific bridge or access change.

Alternative: use vanilla offer max-uses as global state. Merchant offers are shared by the opened merchant and cannot represent different player histories.

### Persist under a versioned YARS player-NBT root

Counts will live under the YARS-owned persistent-data compound `yars.reward_shop.v1.trades.<trade-id>`, with integer values representing completed transactions. This path is the script/UI contract; absent, non-integer, and negative values resolve as zero.

Alternative: a capability/component abstraction. This needs loader-specific plumbing for a small integer map and is not required when the stated integration requirement is player NBT access.

### Use a provenance-reviewed texture asset

Select one block texture from the referenced repositories only after verifying its repository license permits redistribution in this mod. Include the original path, author, license, and any required notice in the repository's attribution documentation.

Alternative: copy an unverified texture now. That risks distributing an asset without a compatible license.

## Risks / Trade-offs

- [Vanilla merchant internals differ between loaders/mappings] -> Keep the merchant adapter behind the existing core/platform split and test both loader builds.
- [KubeJS callbacks can generate invalid or changing stacks] -> Validate registration output and reject invalid stacks before an offer reaches the menu.
- [Trade IDs renamed by a pack author reset visible history] -> Document IDs as persistent identifiers; changing an ID intentionally starts a new progression record.
- [Player NBT can be edited externally] -> Treat absent, malformed, or negative counts as zero and clamp persisted values to non-negative integers.
- [Texture provenance is uncertain] -> Do not ship an external asset until a compatible license and attribution are recorded.

## Migration Plan

This is a new block with no existing saved data or configured trade API. Existing worlds receive no behavior until a pack author registers trades; missing player data resolves to zero purchases. Removing a script trade leaves its player-NBT count inert and harmless. Rolling back removes access to the block and leaves the namespaced count data ignored.

## Open Questions

None. The selected texture provenance is recorded in `THIRD_PARTY_NOTICES.md`. Dynamic callbacks receive the player's zero-based total purchase index; their first output is validated during registration and their current output is validated during server-side offer resolution.
