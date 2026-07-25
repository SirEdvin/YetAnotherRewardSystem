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
  event.trade('pack:daily_diamond')
    .simple(1, Item.of('minecraft:emerald'), Item.of('minecraft:diamond'))

  event.trade('pack:doubling_diamonds')
    .dynamic(3, index => Item.of('minecraft:emerald', 2 ** index), index => Item.of('minecraft:diamond'))

  event.attach('kubejs:daily_rewards', 'pack:daily_diamond')
  event.attach('kubejs:daily_rewards', 'pack:doubling_diamonds')
  event.attach('kubejs:veteran_rewards', 'pack:daily_diamond')
  event.attach('kubejs:automated_rewards', 'pack:daily_diamond')
})
```

`simple` and `dynamic` take payment first and reward second. A positive whole number limits a stage; `-1` makes the final stage unlimited. `dynamic` receives the player's zero-based total purchases for that namespaced trade. Define each trade once, then attach it to any number of interactive or automatic boxes; attachment order controls presentation order.

Trade-script changes apply on `/reload`. A successful reload atomically replaces all definitions and attachments. Invalid targets, duplicate definitions or attachments, and unknown attached trades reject the entire reload and leave the previous snapshot active. A valid box without attachments still opens an empty screen.

## Automatic Reward Boxes

`yars:automatic_reward_box` is also startup-script-only and uses its block ID as its shop ID. Using the block opens its trade list and six-slot internal storage together: select a trade on the left, supply payment through the first two storage slots, and recover stored results from the remaining four. The screen never performs a manual merchant trade.

The first two inventory slots hold payment and the remaining four hold results. Item automation may insert only exact item-and-NBT matches for either cost of the currently selected trade. It may extract only from result slots. Whenever committed insertion completes the selected costs, the box performs as many trades as its payments and result capacity allow.

Each box belongs to the player who placed it. Only that owner can open the UI or change its selection, while hoppers and loader item APIs retain normal access. Progress is shared server-wide by owner UUID and namespaced trade ID, so execution continues while the owner is offline and stages or limits are shared with interactive purchases. A reload that removes or detaches the selected trade stops processing but retains its ID and contents; restoring the same attachment resumes from shared history.

When Jade is installed, looking at an automatic reward box shows its selected trade ID in the block tooltip.

Adding or changing an automatic reward-box block requires a full restart. Its trades still update through `/reload`. YARS provides the block type and behavior only; the pack must create every block and provide its presentation or assets.

## Player History

Completed counts live in the overworld's `yars_reward_trade_history` SavedData, keyed by canonical player UUID and namespaced trade ID. The same data is used from every dimension and does not require the player to be online.

Missing, malformed, negative, and unreleased player-NBT or block-local legacy values count as zero.

## Migration

The previous `event.shop(...).trade(...)` API and shop-scoped player history are not migrated. Give every trade a stable namespaced ID, declare it globally, and attach it separately to each target box.
