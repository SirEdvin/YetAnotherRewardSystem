# Reward Shop KubeJS

Register trades in `kubejs/server_scripts`, then run `/reload` or restart the server. This event is unavailable to `startup_scripts`; trade IDs are persistent player-history keys.

```js
RewardShopEvents.trades(event => {
  event.trade('basic_diamonds')
    .simple(4, Item.of('minecraft:diamond'), Item.of('minecraft:emerald'))
    .dynamic(3, index => Item.of('minecraft:diamond'), index => Item.of('minecraft:emerald', 2 ** index))
    .dynamic(-1, index => Item.of('minecraft:diamond'), index => Item.of('minecraft:iron_ingot', 48 + index * 16))
})
```

Use a positive count to limit a stage or `-1` for the final unlimited stage. `dynamic` receives a one-based purchase index within that stage. Completed counts are stored in `player.persistentData.yars.reward_shop.trades` under the trade ID. Missing or invalid values resolve as zero.
