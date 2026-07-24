## Why

Reward shops currently require a player to place payment in the merchant screen and take each result manually. Packs need an automation-oriented variant where a player chooses the desired trade once and item transport can supply its costs and collect stored results.

## What Changes

- Add a KubeJS-only automatic reward-box block type that uses an existing reward-shop ID and its registered trades.
- Let a player open the box UI and select the one trade the box should perform without completing that trade from the UI.
- Add persistent block inventory and selection state: inserted matching costs are consumed, completed results are stored, and block-local purchase progress is retained for staged trades.
- Recheck the selected trade whenever the inventory receives an item and perform it repeatedly while costs and output capacity permit.
- Expose the inventory through standard item automation while restricting insertion to items accepted by the currently selected trade and keeping outputs available for extraction.
- Handle trade reloads, selection changes, block removal, save/load, and invalid dynamic trades without item loss or duplication.
- Add unit tests, cross-loader GameTests and KubeJS smoke coverage, and complete startup/server-script examples.

## Capabilities

### New Capabilities
- `automatic-reward-boxes`: KubeJS registration, trade selection UI, persistent inventory, automatic trade execution, item automation, lifecycle safety, and examples for automatic reward boxes.

### Modified Capabilities

None.

## Impact

- Affects shared block, block-entity, menu/networking, reward-shop resolution, KubeJS builder registration, persistence, item capability/storage integration, documentation, and tests.
- Requires small loader-specific adapters for Forge item capabilities and Fabric transfer/storage APIs while keeping trade behavior in core.
- Adds no built-in gameplay block or assets; packs remain responsible for registering and presenting every automatic reward box through KubeJS.
