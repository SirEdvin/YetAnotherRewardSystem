package site.siredvin.yars.common.setup

import site.siredvin.yars.common.block.RewardShopBlock
import site.siredvin.yars.xplat.ModPlatform

object Blocks {
    val REWARD_SHOP = ModPlatform.registerBlock("reward_shop", ::RewardShopBlock)

    fun doSomething() {}
}
