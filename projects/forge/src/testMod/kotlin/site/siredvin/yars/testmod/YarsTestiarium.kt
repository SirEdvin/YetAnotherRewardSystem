package site.siredvin.yars.testmod

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterGameTestsEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import site.siredvin.testiarium.Testiarium
import site.siredvin.yars.common.block.RewardShopBlock
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

object YarsTestiarium {
    private val blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, "yars_test")

    @JvmStatic
    @Suppress("DEPRECATION")
    fun registerHooks() {
        blocks.register("reward_shop") {
            val shopId = ResourceLocation("yars_test", "reward_shop")
            RewardShopBlock(BlockBehaviour.Properties.of().strength(2.5f), shopId)
        }
        blocks.register(KotlinModLoadingContext.get().getKEventBus())
        Testiarium.init()
        MinecraftForge.EVENT_BUS.addListener { event: ServerStartedEvent -> Testiarium.onServerStarted(event.server) }
        MinecraftForge.EVENT_BUS.addListener { _: ServerStoppingEvent -> Testiarium.onServerStopped() }
        KotlinModLoadingContext.get().getKEventBus().addListener { event: RegisterGameTestsEvent ->
            Testiarium.loadTests(event::register)
        }
    }
}
