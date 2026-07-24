## 1. Shared Block State And Transactions

- [x] 1.1 Implement the automatic reward-box block and block entity with two payment slots, output storage, selected trade ID, and direct-key per-trade completion counts
- [x] 1.2 Add save/load, player inventory access, container drops, malformed-count handling, and update synchronization without changing existing manual reward-shop state
- [x] 1.3 Implement shared exact-cost matching and atomic simulate-consume-insert-increment execution, including repeated performances, staged and limited trades, invalid resolvers, and reentrancy protection
- [x] 1.4 Recheck processing after committed insertion, selection, load, and successful trade-registry replacement while retaining invalid or removed selections and stored items
- [x] 1.5 Add focused unit tests for persistence, per-trade progress, one- and two-cost transactions, repeated and staged execution, full outputs, invalid trades, reload removal/restoration, and no-loss/no-duplication guarantees

## 2. Selection Interface

- [x] 2.1 Add a server-authoritative selection-only merchant/menu path that shows resolvable offers with the block display name and restores the current selection
- [x] 2.2 Validate offer selection by stable trade ID, persist valid selections, and prevent merchant payment or result transfer through the selection interface
- [x] 2.3 Add client GameTests for empty offers, selecting and replacing trades, stale selection rejection, and selection without granting or consuming items

## 3. KubeJS And Loader Integration

- [x] 3.1 Register `yars:automatic_reward_box` through the KubeJS block and block-entity lifecycle, using the created block ID as its shop ID and retaining normal builder customization
- [x] 3.2 Extend shop-target validation to accept automatic reward boxes while preserving current validation and behavior for manual reward shops
- [x] 3.3 Expose shared payment insertion and output extraction through Fabric transfer/storage APIs, including non-mutating simulation
- [x] 3.4 Expose the same shared operations through Forge item capabilities, including non-mutating simulation
- [x] 3.5 Add cross-loader GameTests for save/load, breaking drops, accepted and rejected insertion, output extraction, simulation, staged execution, and reload-safe behavior

## 4. Examples And Verification

- [x] 4.1 Add complete startup-script and server-script examples showing custom block presentation, trade registration, player selection, item input, and output extraction
- [x] 4.2 Document the two payment slots, block-local per-trade progress, selection/reload behavior, automation rules, recovery of unmatched inputs, and lack of built-in assets
- [x] 4.3 Run core unit tests and both Fabric and Forge client GameTests under `xvfb-run` with explicit timeouts
- [x] 4.4 Run Fabric and Forge KubeJS server smoke tests confirming registration, trade targeting, persistence/reload behavior, automation startup, and zero script errors
- [x] 4.5 Run the timed multi-loader Gradle build and inspect relevant failure logs before marking the change complete
