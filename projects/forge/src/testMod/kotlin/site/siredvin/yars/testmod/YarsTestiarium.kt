package site.siredvin.yars.testmod

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterGameTestsEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import site.siredvin.testiarium.Testiarium
import site.siredvin.yars.common.block.AutomaticRewardBoxBlock
import site.siredvin.yars.common.block.AutomaticRewardBoxBlockEntity
import site.siredvin.yars.common.block.RewardShopBlock
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

object YarsTestiarium {
    private val blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, "yars_test")
    private val blockEntities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "yars_test")

    @JvmStatic
    @Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun registerHooks() {
        listOf("reward_shop", "fletching_rewards", "cartographer_rewards").forEach { path ->
            blocks.register(path) {
                val shopId = ResourceLocation("yars_test", path)
                RewardShopBlock(BlockBehaviour.Properties.of().strength(2.5f), shopId)
            }
        }
        val automaticId = ResourceLocation("yars_test", "automatic_reward_box")
        lateinit var type: BlockEntityType<*>
        val automaticBlock = blocks.register("automatic_reward_box") {
            AutomaticRewardBoxBlock(BlockBehaviour.Properties.of().strength(2.5f), automaticId) { type }
        }
        blockEntities.register("automatic_reward_box") {
            BlockEntityType.Builder.of({ pos, state -> AutomaticRewardBoxBlockEntity(type, pos, state, automaticId) }, automaticBlock.get()).build(null).also { type = it }
        }
        val bus = KotlinModLoadingContext.get().getKEventBus()
        blocks.register(bus)
        blockEntities.register(bus)
        Testiarium.init()
        MinecraftForge.EVENT_BUS.addListener { event: ServerStartedEvent -> Testiarium.onServerStarted(event.server) }
        MinecraftForge.EVENT_BUS.addListener { _: ServerStoppingEvent -> Testiarium.onServerStopped() }
        KotlinModLoadingContext.get().getKEventBus().addListener { event: RegisterGameTestsEvent ->
            Testiarium.loadTests(event::register)
        }
    }
}
