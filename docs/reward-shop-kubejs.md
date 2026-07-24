# Reward Shop KubeJS

YARS provides the `yars:reward_shop` KubeJS block type, but no built-in blocks or assets. Create each shop in `kubejs/startup_scripts` and provide its normal KubeJS block assets or builder customization.

```js
// kubejs/startup_scripts/reward_shops.js
StartupEvents.registry('block', event => {
  event.create('daily_rewards', 'yars:reward_shop')
    .displayName('Daily Rewards')
    .hardness(2.5)
    .textureAll('minecraft:block/fletching_table_top')

  event.create('veteran_rewards', 'yars:reward_shop')
    .displayName('Veteran Rewards')
    .hardness(3)
    .textureAll('minecraft:block/cartography_table_top')

  event.create('automated_rewards', 'yars:automatic_reward_box')
    .displayName('Automated Rewards')
    .hardness(3)
    .textureAll('minecraft:block/copper_block')
})
```

Adding, removing, or changing a startup-script block requires a full game or server restart. The block ID, such as `kubejs:daily_rewards`, is also its shop ID.

Populate those blocks from `kubejs/server_scripts`:

```js
// kubejs/server_scripts/reward_shops.js
RewardShopEvents.trades(event => {
  event.shop('kubejs:daily_rewards', shop => {
    shop.trade('daily_diamond')
      .simple(1, Item.of('minecraft:emerald'), Item.of('minecraft:diamond'))

    shop.trade('doubling_diamonds')
      .dynamic(3, index => Item.of('minecraft:emerald', 2 ** index), index => Item.of('minecraft:diamond'))
  })

  event.shop('kubejs:veteran_rewards', shop => {
    // Trade IDs only need to be unique inside one shop.
    shop.trade('daily_diamond')
      .simple(-1, Item.of('minecraft:gold_ingot', 4), Item.of('minecraft:diamond'))
  })

  event.shop('kubejs:automated_rewards', shop => {
    shop.trade('automated_diamond')
      .simple(-1, Item.of('minecraft:emerald', 4), Item.of('minecraft:diamond'))
  })
})
```

`simple` and `dynamic` take payment first and reward second. A positive whole number limits a stage; `-1` makes the final stage unlimited. `dynamic` receives the player's zero-based total purchases for that trade in that shop. Repeated `event.shop` declarations merge in script execution order.

Trade-script changes apply on `/reload`. A successful reload replaces all shop registrations. An invalid shop target or duplicate trade ID within one shop rejects the entire reload and leaves the previous valid registrations active. A valid shop without trades still opens an empty merchant screen.

## Automatic Reward Boxes

`yars:automatic_reward_box` is also startup-script-only and uses its block ID as its shop ID. Using the block opens its trade list and six-slot internal storage together: select a trade on the left, supply payment through the first two storage slots, and recover stored results from the remaining four. The screen never performs a manual merchant trade.

The first two inventory slots hold payment and the remaining four hold results. Item automation may insert only exact item-and-NBT matches for either cost of the currently selected trade. It may extract only from result slots. Whenever committed insertion completes the selected costs, the box performs as many trades as its payments and result capacity allow.

Progress is local to each placed box and stored separately by full trade ID. Switching trades does not reset either trade's staged progress. A reload that removes or invalidates the selected trade stops processing but retains its ID, progress, and contents; restoring the same trade ID resumes it. Inputs that no longer match remain recoverable and are never deleted.

When Jade is installed, looking at an automatic reward box shows its selected trade ID in the block tooltip.

Adding or changing an automatic reward-box block requires a full restart. Its trades still update through `/reload`. YARS provides the block type and behavior only; the pack must create every block and provide its presentation or assets.

## Player History

Completed counts are direct compound keys at:

```text
player.persistentData.yars.reward_shop.v2.shops[<shop-id>].trades[<trade-id>]
```

For example, use bracket access for `shops['kubejs:daily_rewards'].trades['daily_diamond']`. Colons, dots, and slashes in IDs are key characters, not path separators. Missing, non-integer, negative, and legacy `v1` values count as zero.

## Migration

The built-in `yars:reward_shop` block and global `event.trade(...)` API were removed. Before updating an existing world, replace placed built-in blocks if their locations must be preserved. Add startup-script blocks, restart, then move every trade under its matching `event.shop(...)` callback. Old global purchase history remains untouched but is not assigned to custom shops.
