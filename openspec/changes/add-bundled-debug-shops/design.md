## Context

See proposal.md for motivation and specs/bundled-debug-shops/spec.md for acceptance behavior. The target is Minecraft 1.20.1, with shared Kotlin code and Fabric/Forge adapters.

Existing production `RewardShopBlock` and `AutomaticRewardBoxBlock` already implement the necessary interactions. Concrete blocks currently come from KubeJS builders. Fabric's builder explicitly calls `registerAutomaticRewardBoxStorage`; Forge's `AutomaticRewardBoxItemHandlerMixin` recognizes the shared block-entity class. The automatic block entity implements `IOwnedBlockEntity`, and its owner and selection are persisted. `RewardShopTradeHistory` stores per-player, per-trade history in overworld SavedData; older automatic-box design documents describe obsolete block-local history and must not drive this implementation.

`RewardShopTradeRegistration` validates definitions and attachments before calling `RewardShopTrades.replace`, which publishes a complete snapshot and notifies listeners. Both loader copies of `RewardShopTradeEvent` currently create an empty registration and publish it after a successful event. Thus initializing built-ins only once would lose them on the next script reload.

## Goals / Non-Goals

**Goals:**
- Reuse real gameplay classes and transaction paths; debug content is data and registration, not a parallel shop engine.
- Keep native content and catalog definitions in shared production code, with minimal loader registration/lifecycle glue.
- Make history and inventory behavior observable with cheap vanilla payments and a concrete catalog.

**Non-Goals:**
- No new screen, runtime asset generation, configuration framework, reset command, crafting progression, or test-only shop implementations.
- Do not make KubeJS an optional dependency as part of this work; script-free operation means the declared normal dependency set without user-authored scripts.
- No Creative bypass of payment, ownership, or history. Creative-only acquisition must still allow realistic Survival testing after placement.

## Decisions

### Native registrations reuse shared classes

Add a small shared holder/factory for the two stable block IDs and their block items. Use Fabric native registry calls in `YarsFabric` and the existing Forge deferred registers in `YarsForge`, adding a deferred block-entity register for the native automatic block. Use a supplier for its type, matching the existing automatic block constructor, to avoid registration-order cycles. Add both items to the vanilla Functional Blocks Creative tab through each loader's native tab event; a dedicated tab for two fixtures is unnecessary.

Register Fabric storage for the native automatic block using the existing function. Reuse Forge's existing shared-class handler and verify real capability lookups rather than introducing another adapter. Verify player placement through the ordinary block item establishes ownership using the inherited Broccolium placement path; inspect that pinned implementation during apply and add only a narrowly scoped placement hookup if required. Do not allow ownerless `/setblock` fixtures to stand in for real placement tests.

Prefer native registration over bundled KubeJS scripts because the user explicitly requires mod-owned content. Keep any resource files outside `src/generated`; there is no need to hand-edit generated output.

### Seed each candidate catalog before scripts and publish once

Introduce one native catalog-population function taking `RewardShopTradeRegistration`. Keep the registration constructor empty for existing isolated callers/tests; explicitly seed production event registrations in both loader copies before script handlers run. Retain duplicate and attachment validation. A script using a bundled trade ID is an error, not an override, and the old snapshot remains intact if candidate validation fails.

The pinned KubeJS `ServerScriptManager` invokes `onServerReload` after loading scripts, including initial startup. Establish the native baseline there only when the catalog lacks its built-in identity, before posting the seeded candidate. Clear the transient catalog on the loader's server-stopped event so a second integrated world cannot inherit another world's scripted definitions. Every script reload candidate is seeded anew; do not seed inside low-level `RewardShopTrades.replace`, which remains a complete-snapshot operation used by isolated tests.

Approved reload-safety correction: pinned KubeJS skips `afterPosted` without listeners and can log handler exceptions while still returning PASS. Publish explicitly after posting rather than inside `afterPosted`, track the event exception callback, and check newly captured server-console errors to cover explicit error exits. Catch catalog validation errors at publication and log them without replacing the valid snapshot. Preserve normal KubeJS diagnostics; do not swallow the originating exception or report the reload as successful.

Keep history separate from catalog publication and never reset SavedData on load/reload. Existing listener notification provides automatic-box re-resolution. This avoids a second registry, a generic source-merging framework, or relaxed validation.

### Concrete catalog with shared and independent identities

All IDs below use namespace `yars`; the table lists the path. Use fresh copied stacks from native resolvers and preserve this declaration order in attachments. Shared entries attach to both shops; the last two attach only to the named shop. The native Kotlin builder already supports fixed and dynamic stages.

| Trade path | Attachment | Payment → result | Progression |
| --- | --- | --- | --- |
| `debug/shared/basic` | Both | 1 cobblestone → 1 emerald | Unlimited |
| `debug/shared/two_costs` | Both | 1 emerald + 2 sticks → 4 torches | Unlimited |
| `debug/shared/limited` | Both | 1 emerald → 1 diamond | 3 purchases, then exhausted |
| `debug/shared/staged` | Both | 1 emerald → 1 iron ingot; then 2 emeralds → 1 gold ingot; then 3 emeralds → 1 diamond | First stage 2 purchases, second stage 2 purchases, final stage unlimited |
| `debug/shared/dynamic` | Both | (1 + n % 4) emeralds → (1 + n % 3) redstone | Unlimited; n is completed purchases for this player and trade before purchase |
| `debug/shared/same_item_costs` | Both | 2 cobblestone + 3 cobblestone → 1 copper ingot | Unlimited; tests combined payment handling |
| `debug/shared/token_source` | Both | 1 paper → 1 paper carrying string NBT `yars_debug_token: "reward"` | Unlimited |
| `debug/shared/token_exchange` | Both | 1 matching tagged paper → 1 amethyst shard | Unlimited; ordinary paper is insufficient |
| `debug/manual/limited` | Manual only | 1 dirt → 1 apple | 2 purchases, then exhausted |
| `debug/automatic/limited` | Automatic only | 1 dirt → 1 apple | 2 purchases, then exhausted; independent of the manual ID |

NBT token source/exchange keeps the test script-free and avoids requiring hand-written NBT commands. Give the tagged paper a readable hover name and use the same complete stack definition for the source output and exchange cost. Both manual and automatic paths use the existing matching semantics, not a new special-case matcher.

Shared limited/staged examples visibly prove cross-shop history. Identical shop-specific recipes with separate IDs make independence easy to observe. A fresh world is the supported repeatable reset; breaking/replacing shops is not a progress reset. A second player tests independent shared history and owner rejection. Explicitly document that full output buffers block automatic trading and that clearing them resumes it.

### Reproducible small pixel textures

Add a Python 3 standard-library script at a new `tools/generate_debug_shop_textures.py` path, writing deterministic PNGs using explicit palette-index/pixel operations and `struct`/`zlib` encoding. No Pillow install, network access, random noise, or image-generation service is needed. Commit the generator and produced assets, but do not run generation at mod startup or make Python a runtime dependency.

Use opaque 16×16 textures, a shared dark outlined base, and restrained highlights. The manual shop uses warm wood/brass tones and a coin/counter motif; the automatic shop uses cool metal/teal tones and a hopper/arrow motif. Put the identifying motif on side faces so no new facing state is necessary. Use simple cube models with distinct top, bottom, and side textures and block-parent item models. Target resources are under `projects/core/src/main/resources/assets/yars/{textures/block,models/block,models/item,blockstates}` plus the existing `lang/en_us.json`. English names: "Debug Reward Shop" and "Debug Automatic Reward Box".

Byte-for-byte regeneration and PNG header/dimension checks prove reproducibility, not visual quality. Inspect nearest-neighbor enlarged previews and real in-world/inventory renders on both loaders; include resource reload and missing-asset log checks. Keep temporary previews/screenshots outside the repository.

### Verify the bundled fixture, not only synthetic substitutes

Extend existing shared tests for catalog definitions, stages, limits, NBT stacks, shared/independent IDs, reload merging, failure atomicity, and valid dynamic counts at representative and boundary purchase indices. Keep existing isolated registry tests isolated.

Extend Testiarium client tests to obtain and place the actual native registry items, open both populated interfaces, perform manual and automatic purchases, verify ownership and shared progression, and use real loader storage/capability paths. Exercise hopper flows, insufficient payments, same-item combined cost, full-output blocking/resume, shift-click, and save/reopen behavior. Run both dedicated servers with no local smoke scripts to prove baseline registration and catalog startup, then run temporary scripted coexistence/reload smoke tests on both loaders, restoring local scripts afterward.

Document the table and a step-by-step tester guide in README or a linked focused document. Include `/give @s yars:debug_reward_shop`, `/give @s yars:debug_automatic_reward_box`, placement as a player, Survival-mode payment checks, fresh-world restart, and optional Jade checks only when installed. No claim that all acceptance is covered by compilation alone.

## Risks / Trade-offs

- [KubeJS and loader lifecycle callbacks can overwrite an already published catalog] → Inspect actual ordering before wiring, test first startup, repeated reload, and a second integrated world; assert scripted and native entries survive together.
- [An invalid script collides with native IDs] → Keep all-or-nothing validation and report the conflicting ID; no implicit override policy.
- [Native registration skips builder-provided ownership or storage setup] → Trace inherited placement and exercise real block items and loader lookups on both platforms.
- [Creative mode hides inventory/payment defects] → Use Creative for acquisition and Survival for transaction checks; do not add Creative bypasses.
- [Finite shared examples become exhausted while testing] → Keep stable IDs, document expected cross-shop effects, and use a fresh world for resets rather than deleting persistent data.
- [Earlier pending specs prohibit bundled blocks] → Treat this capability as the explicit debug-content exception and reconcile that wording when those older changes are archived; do not silently rewrite unrelated change artifacts here.

## Migration Plan

This is additive native content; existing KubeJS block IDs and player history remain unchanged. Ship both loader jars with the same resources and IDs. Existing worlds receive the native catalog on next server start/reload. No data migration is needed. Before downgrading to a version without these registrations, back up the world and remove native debug blocks/items; do not promise automatic recovery of unknown registry entries. Keep future fixture IDs stable because they key persistent history.
