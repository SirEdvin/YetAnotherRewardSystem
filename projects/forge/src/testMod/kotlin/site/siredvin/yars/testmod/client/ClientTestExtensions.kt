package site.siredvin.yars.testmod.client

import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.gametest.framework.GameTestAssertException
import net.minecraft.gametest.framework.GameTestSequence
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicBoolean

fun Minecraft.isRenderingStable(): Boolean = level != null && player != null

fun GameTestSequence.thenOnClient(task: Minecraft.() -> Unit): GameTestSequence {
    var future: CompletableFuture<Void>? = null
    thenExecute { future = Minecraft.getInstance().submit { task(Minecraft.getInstance()) } }
    thenWaitUntil { if (!future!!.isDone) throw GameTestAssertException("Client task has not completed") }
    thenExecute {
        try {
            future!!.get()
        } catch (error: ExecutionException) {
            throw error.cause ?: error
        }
    }
    return this
}

fun GameTestSequence.thenRenderIdle(ticks: Int = 20): GameTestSequence {
    var idleTicks = 0
    thenWaitUntil {
        if (Minecraft.getInstance().isRenderingStable()) {
            if (++idleTicks <= ticks) throw GameTestAssertException("Rendering has only been idle for $idleTicks ticks")
        } else {
            idleTicks = 0
            throw GameTestAssertException("Waiting for the client to finish rendering")
        }
    }
    return this
}

fun GameTestSequence.thenScreenshot(name: String, showGui: Boolean = false): GameTestSequence {
    val captured = AtomicBoolean()
    thenRenderIdle()
    thenOnClient { options.hideGui = !showGui }
    thenIdle(2)
    thenOnClient {
        val directory = File(System.getProperty("testiarium.screenshots", gameDirectory.absolutePath))
        Screenshot.grab(directory, "$name.png", mainRenderTarget) { captured.set(true) }
    }
    thenWaitUntil { if (!captured.get()) throw GameTestAssertException("Screenshot was not captured") }
    thenOnClient { options.hideGui = false }
    return this
}
