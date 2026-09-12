package site.siredvin.yars.testmod

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import site.siredvin.yars.testmod.client.ClientTestHooks

object YarsFabricClientTestHooks : ClientModInitializer {
    override fun onInitializeClient() {
        ServerTickEvents.START_SERVER_TICK.register(ClientTestHooks::onServerTick)
        ClientTickEvents.START_CLIENT_TICK.register { client -> client.screen?.let(ClientTestHooks::onOpenScreen) }
    }
}
