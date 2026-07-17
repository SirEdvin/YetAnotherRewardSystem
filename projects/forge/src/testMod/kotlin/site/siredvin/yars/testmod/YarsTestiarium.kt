package site.siredvin.yars.testmod

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterGameTestsEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import site.siredvin.testiarium.Testiarium
import thedarkcolour.kotlinforforge.KotlinModLoadingContext

object YarsTestiarium {
    @JvmStatic
    @Suppress("DEPRECATION")
    fun registerHooks() {
        Testiarium.init()
        MinecraftForge.EVENT_BUS.addListener { event: ServerStartedEvent -> Testiarium.onServerStarted(event.server) }
        MinecraftForge.EVENT_BUS.addListener { _: ServerStoppingEvent -> Testiarium.onServerStopped() }
        KotlinModLoadingContext.get().getKEventBus().addListener { event: RegisterGameTestsEvent ->
            Testiarium.loadTests(event::register)
        }
    }
}
