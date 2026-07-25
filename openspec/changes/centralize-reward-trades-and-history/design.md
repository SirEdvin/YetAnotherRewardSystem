## Context

The implemented reward-shop model registers trades inside a shop ID and records interactive purchases in player persistent NBT under that shop and trade. The unmerged `feat/automatic-reward-box` baseline adds KubeJS-created automatic boxes with internal payment/output storage, a selected trade, and block-local completion counts. Its change-driven transaction loop already works without an online player, but those local counts do not share progression with interactive shops.

The new model must let both box types attach the same trade and use one player's progression. Automatic boxes therefore need a durable owner UUID, while history must be available without loading that player's entity. The solution must remain loader-neutral and preserve the automatic box's existing inventory, selection UI, item automation, and transaction behavior.

## Goals / Non-Goals

**Goals:**
- Define globally reusable trades with namespaced IDs and attach them to any number of reward boxes.
- Share each player's count for a trade across every attachment and box type.
- Make history available to loaded automatic boxes while their owners are offline.
- Persist automatic-box ownership and permit only the owner to open its UI or change its selection.
- Keep registration replacement atomic and transactions server-authoritative on Fabric and Forge.

**Non-Goals:**
- Migrating the unreleased player-NBT or block-local history formats.
- Changing automatic-box slot layout, hopper/capability behavior, selection presentation, or change-driven processing.
- Adding ownership transfer, operator bypass, teams, shared boxes, remote access, or history cleanup.
- Adding a Broccolium abstraction when its existing `IOwnedBlockEntity` contract is sufficient.

## Decisions

### Separate global definitions from box attachments

`RewardShopEvents.trades` will build two immutable structures: a map of namespaced trade IDs to trade definitions and an ordered map of box block IDs to attached trade IDs. The KubeJS surface will declare a trade once with `event.trade('pack:daily_diamond')...` and attach it separately with `event.attach('kubejs:daily_shop', 'pack:daily_diamond')`. The same trade may be attached to both `yars:reward_shop` and `yars:automatic_reward_box` instances.

Trade IDs become `ResourceLocation` values and are globally unique during one registration cycle. Attachments preserve declaration order for merchant presentation. Registration rejects duplicate definitions, duplicate attachments, unknown trade IDs, missing block IDs, and blocks that are not reward-shop targets. The complete definitions-and-attachments snapshot replaces the previous snapshot only after all declarations validate.

Alternative: retain per-box definitions and copy the same builder into each box. Copies can drift and cannot provide one unambiguous history identity, so they do not satisfy shared progression.

### Store history in one server-wide SavedData record

A `SavedData` instance obtained from the overworld `DimensionDataStorage` will own all counts, making the data server-wide and dimension-independent. Its persisted shape will be versioned and keyed directly by canonical UUID and trade-ID strings, conceptually `players[<uuid>].trades[<namespace:path>] = <count>`. Missing, malformed, negative, or overflowed values resolve safely as zero or the supported integer maximum.

The history API will accept a server level, player UUID, and trade ID. Reads never require a `Player` entity. Successful increments saturate at `Int.MAX_VALUE` and mark the SavedData dirty. All access occurs on the logical server thread, matching merchant and block-entity transaction execution, so an additional locking or loader capability layer is unnecessary.

Alternative: read offline player files. That couples shared logic to player save internals, leaves automatic transactions dependent on entity persistence, and risks conflicting writes when a player is online.

### Make trade identity the progression boundary

History is keyed by player UUID and global trade ID only, not by box ID, dimension, position, or box type. An interactive purchase and an automatic purchase of the same attached trade therefore advance the same stage and limit. Attaching a trade elsewhere exposes its current shared state immediately.

Alternative: key counts by attachment. That reuses only definitions and preserves the current inability to share progression, contrary to the requested trade semantics.

### Pass player identity at each transaction boundary

Interactive shops capture the interacting player's UUID when constructing their merchant adapter, then resolve and increment SavedData with that UUID. They no longer configure or access loader-specific player persistent NBT.

Automatic boxes implement Broccolium's existing `IOwnedBlockEntity`. Broccolium's `BaseBlockEntityBlock.setPlacedBy` supplies the placing player; the block entity persists only that player's UUID and resolves the live `Player` object only when the interface requests it. A box without an owner, including one placed by a non-player mechanism or loaded with malformed ownership data, cannot open a UI, select a trade, or execute one.

Only a player whose UUID equals the stored owner UUID may open the automatic-box menu. Hoppers and loader item APIs retain their existing payment insertion and output extraction access because ownership controls the player UI, not automation.

Alternative: store or require a live player reference. References do not survive reload and recreate the offline-owner failure.

### Resolve automatic transactions from owner history

The automatic box keeps only inventory, selected namespaced trade ID, and owner UUID. On each existing processing trigger, it resolves the selected attached trade using the owner's count from SavedData, simulates complete output insertion, consumes exact costs, inserts the result, and increments the shared count once. It repeats until the next trade cannot execute.

Selection and processing both revalidate that the selected trade remains attached to that box. Invalid definitions, removed attachments, exhausted limits, missing ownership, insufficient costs, or full output stop without mutation. The existing reentrancy guard remains sufficient because execution is synchronous on the server thread.

Alternative: cache completion counts in the block entity and periodically reconcile them. Two sources of truth permit duplicate purchases and lost updates when interactive and automatic transactions interleave.

## Risks / Trade-offs

- [A globally shared trade ID makes script renames reset visible progression] -> Require namespaced stable IDs and document them as persisted identity.
- [A box owner is offline when a transaction executes] -> Keep all trade resolution inputs limited to UUID, SavedData count, and registered definition; do not invoke player-dependent callbacks.
- [A trade reload removes a selected trade or attachment] -> Retain the selected ID and inventory but stop processing until the same ID is valid and attached again.
- [Multiple loaded boxes for one owner process the same trade] -> Execute all reads and increments synchronously on the server thread and re-read history before every loop iteration.
- [Level history grows indefinitely] -> Retain records because no safe automatic deletion policy is known; add administrative cleanup only when a concrete requirement exists.
- [The automatic-box branch currently extends vanilla `BaseEntityBlock`] -> Change it to Broccolium's ownership-aware base block or invoke the same ownership contract explicitly, choosing the smaller implementation that preserves KubeJS block creation on both loaders.

## Migration Plan

1. Apply this change on top of `feat/automatic-reward-box` so its inventory, menu, loader bridges, and tests are available.
2. Replace shop-nested trade registration with global definitions and attachments, then update both merchant implementations to use namespaced identities.
3. Add server-wide SavedData and route interactive purchases through UUID-based reads and increments.
4. Add persisted automatic-box ownership and route selection and processing through the owner's shared history.
5. Update scripts, documentation, unit tests, and both loader GameTests together because the KubeJS API is intentionally breaking.
6. Ignore existing player-NBT and block-local counts. Rollback leaves the new SavedData file inert and restores old behavior without converting counts.

## Open Questions

None. The implementation baseline, ownership rule, registration shape, identity format, history scope, sharing semantics, and migration policy are fixed by this design.
