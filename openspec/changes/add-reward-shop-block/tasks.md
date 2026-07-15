## 1. Trade Model and Persistence

- [x] 1.1 Define shared reward-shop trade, stage, and resolved-offer models with stable trade IDs, one or two costs, results, limits, and purchase-index resolution.
- [x] 1.2 Implement validated per-player completed-trade count access in the documented YARS player-NBT namespace, treating missing or invalid counts as zero.
- [x] 1.3 Add focused tests for fixed, limited, staged, and purchase-index-dependent trade resolution plus per-player count isolation.

## 2. KubeJS Registration

- [x] 2.1 Add the loader KubeJS plugin bindings and a server-side reward-shop trade registration event or builder.
- [x] 2.2 Implement builder validation for unique stable IDs, non-empty legal item stacks, valid payment counts, and limits.
- [x] 2.3 Document KubeJS examples for an unlimited fixed diamond trade, a four-purchase trade, a doubling emerald price, and an emerald-to-iron staged price.

## 3. Reward-Shop Block and Merchant Integration

- [x] 3.1 Register the reward-shop block and block item through the existing platform registration pattern.
- [x] 3.2 Implement the shared merchant adapter and block interaction to open the vanilla wandering trader-style menu with the player's current offers.
- [x] 3.3 Implement server-side transaction revalidation, player count update, and offer refresh so stale or exhausted offers cannot be completed.
- [x] 3.4 Verify no loader-specific bridge or access change is required for public vanilla merchant APIs.

## 4. Assets and Verification

- [x] 4.1 Select a redistribution-compatible texture from a supplied open texture repository and record its source, author, license, and required attribution.
- [x] 4.2 Add the texture and block-state/item-model resources using the selected asset.
- [x] 4.3 Add an integration or game test covering opening an empty shop, completing a capped trade, and verifying persistence across player reload.
- [x] 4.4 Run the core, Fabric, and Forge build/test tasks and manually verify the wandering trader UI, staged costs, and NBT-readable history in a development world.
