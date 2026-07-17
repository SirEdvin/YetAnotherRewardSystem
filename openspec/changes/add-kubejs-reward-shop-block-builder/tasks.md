## 1. KubeJS Block Registration

- [ ] 1.1 Implement a reward-shop `BlockBuilder` that creates `RewardShopBlock` instances with KubeJS-generated properties and uses the block resource location as the shop ID
- [ ] 1.2 Register the `yars:reward_shop` block type from the YARS KubeJS plugin for both supported loaders
- [ ] 1.3 Add focused tests or startup smoke coverage proving multiple custom reward-shop blocks register without YARS-owned block assets

## 2. Shop-Scoped Trade Model

- [ ] 2.1 Replace the global trade list with an immutable shop-ID-to-trades registry that supports lookup and whole-registry replacement
- [ ] 2.2 Extend `RewardShopEvents.trades` with nested shop callbacks that merge repeated shop declarations in event order while retaining existing staged trade builders
- [ ] 2.3 Add atomic registration validation for duplicate shop/trade pairs and missing or non-shop block targets, preserving the previous registry after failure
- [ ] 2.4 Remove the unscoped global trade API and pass the shop ID and block display name through `RewardShopMerchant` offer generation, screen opening, and stale transaction approval
- [ ] 2.5 Add unit tests for independent shop inventories, repeated shop merges, duplicate trade IDs across shops, duplicates within one shop, invalid targets, empty shops, atomic failure, and reload replacement

## 3. Shop-Scoped Player Progress

- [ ] 3.1 Implement direct-key storage at `yars.reward_shop.v2.shops[<shop-id>].trades[<trade-id>]` without interpreting identifier punctuation as NBT paths
- [ ] 3.2 Update offer resolution, transaction completion, and validation to read and increment shop-scoped counts while ignoring legacy global counts
- [ ] 3.3 Add tests for independent same-named trades, reconnect persistence, malformed values, and legacy-history isolation

## 4. Remove Built-In Block Content

- [ ] 4.1 Remove static `yars:reward_shop` registration, the YARS creative tab, and other code that assumes a built-in block
- [ ] 4.2 Remove built-in block models, blockstate, texture, loot, recipe, language, and data-provider definitions, regenerating loader outputs through Gradle rather than editing generated resources manually
- [ ] 4.3 Update existing GameTests to create and target custom reward-shop blocks instead of the removed built-in block

## 5. Documentation And Verification

- [ ] 5.1 Rewrite the KubeJS guide with complete `startup_scripts` block creation and `server_scripts` shop population examples, restart/reload rules, migration warnings, and the new history path
- [ ] 5.2 Run core unit tests and the timed multi-loader Gradle build
- [ ] 5.3 Run KubeJS development-server smoke tests on Forge and Fabric, confirming custom block registration, per-shop trades, reload behavior, and zero script errors in each server log
