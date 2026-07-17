## Why

The mod-owned `yars:reward_shop` block forces every pack to use one fixed shop block and one global set of trades. Pack authors need to create distinct reward shops with KubeJS, control each block's presentation, and populate each shop independently without YARS shipping gameplay blocks of its own.

## What Changes

- Add a KubeJS block registry type for creating reward-shop blocks from `startup_scripts`, following KubeJS's normal block-builder workflow.
- Add shop identity to reward-shop blocks and route interaction to the trades registered for that specific shop.
- Change server-script trade registration so pack authors fill a named shop rather than one global reward-shop trade list.
- Document the startup-script block definition and server-script trade population lifecycle, including multiple shops.
- **BREAKING** Remove the built-in `yars:reward_shop` block and its mod-provided block assets, loot table, data generation, and creative-tab entry.
- **BREAKING** Replace the global trade registration contract with shop-scoped registration; existing scripts must create a block and target its shop ID.

## Capabilities

### New Capabilities
- `kubejs-reward-shop-blocks`: KubeJS creation of reward-shop blocks and shop-scoped server-script trade registration, lookup, interaction, and reload behavior.

### Modified Capabilities

None.

## Impact

- Affects core block setup, reward-shop trade storage and merchant lookup, KubeJS plugin registration, data generators, resources, documentation, and tests.
- Removes the mod's only registered block and the now-empty YARS creative tab.
- Requires migration of existing KubeJS scripts and removal or replacement of `yars:reward_shop` in existing worlds and packs.
