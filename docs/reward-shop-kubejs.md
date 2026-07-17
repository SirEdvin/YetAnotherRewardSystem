# Reward Shop KubeJS

Register trades in `kubejs/server_scripts`, then run `/reload` or restart the server. This event is unavailable to `startup_scripts`; trade IDs are persistent player-history keys.

```js
RewardShopEvents.trades(event => {
  // Unlimited fixed trade: one emerald buys one diamond.
  event.trade('fixed_diamond')
    .simple(-1, Item.of('minecraft:emerald'), Item.of('minecraft:diamond'))

  // Exactly four purchases at a fixed price.
  event.trade('four_diamonds')
    .simple(4, Item.of('minecraft:emerald'), Item.of('minecraft:diamond'))

  // Three purchases costing one, two, then four emeralds.
  event.trade('doubling_diamonds')
    .dynamic(3, index => Item.of('minecraft:emerald', 2 ** index), index => Item.of('minecraft:diamond'))

  // The first purchase costs an emerald; later purchases cost 64 iron.
  event.trade('basic_diamonds')
    .simple(1, Item.of('minecraft:emerald'), Item.of('minecraft:diamond'))
    .simple(-1, Item.of('minecraft:iron_ingot', 64), Item.of('minecraft:diamond'))
})
```

`simple` and `dynamic` take the payment first and the reward second. Use a positive whole number to limit a stage or `-1` for the final unlimited stage. `dynamic` receives the player's zero-based total completed-purchase count for that trade. IDs must remain stable because completed counts are stored in `player.persistentData.yars.reward_shop.v1.trades` under the trade ID. Missing, non-integer, or negative values resolve as zero.
