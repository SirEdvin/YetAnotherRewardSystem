package site.siredvin.yars.testmod

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import site.siredvin.yars.testmod.client.ClientTestHooks

object YarsFabricClientTestHooks : ClientModInitializer {
    override fun onInitializeClient() {
        ServerTickEvents.START_SERVER_TICK.register(ClientTestHooks::onServerTick)
        ScreenEvents.AFTER_INIT.register { _, screen, _, _ -> ClientTestHooks.onOpenScreen(screen) }
    }
}
