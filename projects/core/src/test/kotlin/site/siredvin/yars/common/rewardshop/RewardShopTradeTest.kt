package site.siredvin.yars.common.rewardshop

import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class RewardShopTradeTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            Bootstrap.bootStrap()
        }
    }

    @Test
    fun `resolves fixed and capped stages`() {
        val trade = RewardShopTradeRegistration().trade("test_trade")
            .simple(2, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
            .simple(1, ItemStack(Items.IRON_INGOT, 4), ItemStack(Items.DIAMOND, 2))
            .build()

        assertEquals(Items.EMERALD, trade.resolve(0)!!.firstCost.item)
        assertEquals(Items.EMERALD, trade.resolve(1)!!.firstCost.item)
        assertEquals(4, trade.resolve(2)!!.firstCost.count)
        assertNull(trade.resolve(3))
    }

    @Test
    fun `dynamic stages receive one-based stage index`() {
        val trade = RewardShopTradeRegistration().trade("dynamic_trade")
            .dynamic(2, { index -> ItemStack(Items.EMERALD, index) }, { ItemStack(Items.DIAMOND) })
            .build()

        assertEquals(1, trade.resolve(0)!!.firstCost.count)
        assertEquals(2, trade.resolve(1)!!.firstCost.count)
    }

    @Test
    fun `rejects invalid fixed stacks during registration`() {
        assertThrows(IllegalArgumentException::class.java) {
            RewardShopTradeRegistration().trade("invalid_trade").simple(1, ItemStack.EMPTY, ItemStack(Items.DIAMOND))
        }
    }

    @Test
    fun `duplicate error identifies trade`() {
        val registration = RewardShopTradeRegistration()
        registration.trade("duplicate").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))
        registration.trade("duplicate").simple(1, ItemStack(Items.EMERALD), ItemStack(Items.DIAMOND))

        val error = assertThrows(IllegalArgumentException::class.java, registration::replaceTrades)
        assertEquals("Duplicate reward shop trade ID: duplicate", error.message)
    }
}
