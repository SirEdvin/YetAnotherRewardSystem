## Why

Players need a reward redemption point that pack authors can configure without hard-coding fixed shop offers. The first mod block should support progression-aware prices and per-player purchase limits while exposing each player's trade history to UI integrations.

## What Changes

- Add a reward-shop block that opens the vanilla wandering trader trade interface.
- Add a KubeJS API for registering reward-shop trades, including unlimited or per-player-limited purchases.
- Support trade costs and offered item stacks that vary according to a player's completed purchases of that trade.
- Persist per-player, per-trade purchase counts in player NBT for later UI use.
- Add a block texture from a compatible open texture source and include required attribution or license information.

## Capabilities

### New Capabilities
- `reward-shop-trades`: Configurable reward-shop trade registration, execution, dynamic progression, and per-player trade-history persistence.

### Modified Capabilities

None.

## Impact

- Adds block registration, interaction, menu/trading integration, and player-persistent data to the core and loader projects.
- Adds KubeJS event or builder bindings and script-facing item/cost progression definitions.
- Requires a selected, license-compatible open block texture and accompanying attribution where required.
