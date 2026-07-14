# Reward Shop KubeJS

Register trades in a server script. Trade IDs are persistent player-history keys.

```js
RewardShopEvents.trades(event => {
  event.trade('diamond', Item.of('minecraft:diamond'), Item.of('minecraft:emerald'))

  event.progression('limited_diamond')
    .stage(4, Item.of('minecraft:diamond'), Item.of('minecraft:emerald'))

  event.progression('doubling_diamond')
    .calculated(3, index => Item.of('minecraft:diamond'), index => Item.of('minecraft:emerald', 2 ** index))

  event.progression('iron_diamond')
    .stage(1, Item.of('minecraft:diamond'), Item.of('minecraft:emerald'))
    .unlimited(Item.of('minecraft:diamond'), Item.of('minecraft:iron_ingot', 64))
})
```

Completed counts are stored in `player.persistentData.yars.reward_shop.trades` under the trade ID. Missing or invalid values resolve as zero.
