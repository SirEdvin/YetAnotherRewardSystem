package site.siredvin.yars.testmod

import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import site.siredvin.yars.testmod.client.ClientTestHooks

object YarsClientTestHooks {
    @JvmStatic
    fun register() {
        MinecraftForge.EVENT_BUS.addListener(::onServerTick)

        MinecraftForge.EVENT_BUS.addListener(::onClientTick)
    }

    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) Minecraft.getInstance().screen?.let(ClientTestHooks::onOpenScreen)
    }

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.START) ClientTestHooks.onServerTick(event.server)
    }
}
