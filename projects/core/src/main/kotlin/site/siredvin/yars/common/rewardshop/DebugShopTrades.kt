package site.siredvin.yars.common.rewardshop

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import site.siredvin.yars.common.block.DebugShops

object DebugShopTrades {
    fun registration() = RewardShopTradeRegistration().also(::populate)

    fun populate(registration: RewardShopTradeRegistration) = with(registration) {
        val shared = listOf("basic", "two_costs", "limited", "staged", "dynamic", "same_item_costs", "token_source", "token_exchange")
        trade("yars:debug/shared/basic").simple(-1, ItemStack(Items.COBBLESTONE), ItemStack(Items.EMERALD))
        trade("yars:debug/shared/two_costs").simple(-1, ItemStack(Items.EMERALD), ItemStack(Items.TORCH, 4), ItemStack(Items.STICK, 2))
        trade("yars:debug/shared/limited").simple(3, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        trade("yars:debug/shared/staged")
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.IRON_INGOT))
            .simple(2, ItemStack(Items.EMERALD, 2), ItemStack(Items.GOLD_INGOT))
            .simple(-1, ItemStack(Items.EMERALD, 3), ItemStack(Items.DIAMOND))
        trade("yars:debug/shared/dynamic").dynamic(-1, { ItemStack(Items.EMERALD, 1 + it % 4) }, { ItemStack(Items.REDSTONE, 1 + it % 3) })
        trade("yars:debug/shared/same_item_costs").simple(-1, ItemStack(Items.COBBLESTONE, 2), ItemStack(Items.COPPER_INGOT), ItemStack(Items.COBBLESTONE, 3))
        trade("yars:debug/shared/token_source").simple(-1, ItemStack(Items.PAPER), token())
        trade("yars:debug/shared/token_exchange").simple(-1, token(), ItemStack(Items.AMETHYST_SHARD))
        shared.forEach { name ->
            attach(DebugShops.MANUAL_ID.toString(), "yars:debug/shared/$name")
            attach(DebugShops.AUTOMATIC_ID.toString(), "yars:debug/shared/$name")
        }
        trade("yars:debug/manual/limited").simple(2, ItemStack(Items.DIRT), ItemStack(Items.APPLE))
        trade("yars:debug/automatic/limited").simple(2, ItemStack(Items.DIRT), ItemStack(Items.APPLE))
        attach(DebugShops.MANUAL_ID.toString(), "yars:debug/manual/limited")
        attach(DebugShops.AUTOMATIC_ID.toString(), "yars:debug/automatic/limited")
    }

    fun token() = ItemStack(Items.PAPER).apply {
        orCreateTag.putString("yars_debug_token", "reward")
        setHoverName(Component.literal("Debug Reward Token"))
    }
}
