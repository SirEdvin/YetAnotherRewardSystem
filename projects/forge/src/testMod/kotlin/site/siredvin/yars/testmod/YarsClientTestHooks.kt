package site.siredvin.yars.testmod

import net.minecraft.client.CloudStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.ParticleStatus
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.tutorial.TutorialSteps
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.gametest.framework.GameTestInfo
import net.minecraft.gametest.framework.GameTestListener
import net.minecraft.gametest.framework.GameTestRegistry
import net.minecraft.gametest.framework.GameTestRunner
import net.minecraft.gametest.framework.GameTestTicker
import net.minecraft.gametest.framework.GlobalTestReporter
import net.minecraft.gametest.framework.MultipleTestTracker
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import net.minecraftforge.client.event.ScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.TickEvent
import org.slf4j.LoggerFactory
import site.siredvin.testiarium.api.Timeouts
import site.siredvin.yars.testmod.client.isRenderingStable
import kotlin.system.exitProcess

object YarsClientTestHooks {
    private const val LEVEL_NAME = "testiarium-client"
    private const val STARTUP_DELAY = 5 * Timeouts.SECOND
    private val log = LoggerFactory.getLogger(YarsClientTestHooks::class.java)
    private val enabled = System.getProperty("testiarium.client") != null
    private var loadedWorld = false
    private var tracker: MultipleTestTracker? = null
    private var startupDelay = STARTUP_DELAY
    private var finished = false

    @JvmStatic
    fun register() {
        MinecraftForge.EVENT_BUS.addListener(::onServerTick)
        MinecraftForge.EVENT_BUS.addListener(::onOpenScreen)
        MinecraftForge.EVENT_BUS.addListener(::onClientTick)
    }

    private fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase == TickEvent.Phase.START) Minecraft.getInstance().screen?.let(::openFromScreen)
    }

    private fun onOpenScreen(event: ScreenEvent.Opening) {
        if (openFromScreen(event.screen)) event.isCanceled = true
    }

    private fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.START) tickServer(event.server)
    }

    private fun openFromScreen(screen: Screen): Boolean {
        if (!enabled || loadedWorld || screen !is TitleScreen && screen !is AccessibilityOnboardingScreen) return false
        loadedWorld = true
        val minecraft = Minecraft.getInstance()
        minecraft.options.autoJump().set(false)
        minecraft.options.cloudStatus().set(CloudStatus.OFF)
        minecraft.options.particles().set(ParticleStatus.MINIMAL)
        minecraft.options.tutorialStep = TutorialSteps.NONE
        minecraft.options.pauseOnLostFocus = false
        minecraft.options.renderDistance().set(6)
        minecraft.options.gamma().set(1.0)
        minecraft.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0)
        minecraft.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0.0)
        if (minecraft.levelSource.levelExists(LEVEL_NAME)) {
            minecraft.createWorldOpenFlows().loadLevel(screen, LEVEL_NAME)
        } else {
            val rules = GameRules().apply {
                getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null)
                getRule(GameRules.RULE_DAYLIGHT).set(false, null)
                getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null)
            }
            minecraft.createWorldOpenFlows().createFreshLevel(
                LEVEL_NAME,
                LevelSettings("YARS Client Tests", GameType.CREATIVE, false, Difficulty.EASY, true, rules, WorldDataConfiguration.DEFAULT),
                WorldOptions(0L, false, false),
            ) { it.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions() }
        }
        return true
    }

    private fun tickServer(server: MinecraftServer) {
        if (!enabled || finished) return
        val tests = tracker ?: startTests(server) ?: return
        if (server.overworld().gameTime % 20L == 0L) log.info(tests.progressBar)
        if (!tests.isDone) return
        finished = true
        GlobalTestReporter.finish()
        val exitCode = when {
            tests.totalCount == 0 -> 1
            tests.hasFailedRequired() -> 2
            else -> 0
        }
        Minecraft.getInstance().execute {
            Minecraft.getInstance().apply {
                level?.disconnect()
                clearLevel()
                stop()
            }
            exitProcess(exitCode)
        }
    }

    private fun startTests(server: MinecraftServer): MultipleTestTracker? {
        if (server.overworld().players().isEmpty()) return null
        server.overworld().players().forEach {
            it.abilities.flying = true
            it.onUpdateAbilities()
            it.connection.teleport(0.0, -30.0, 0.0, 0.0f, 90.0f)
            it.inventory.clearContent()
        }
        if (!Minecraft.getInstance().isRenderingStable() || startupDelay-- >= 0) return null
        return MultipleTestTracker(
            GameTestRunner.runTestBatches(
                GameTestRunner.groupTestsIntoBatches(GameTestRegistry.getAllTestFunctions()),
                BlockPos(0, -60, 0),
                Rotation.NONE,
                server.overworld(),
                GameTestTicker.SINGLETON,
                1,
            ),
        ).also {
            it.addListener(object : GameTestListener {
                private fun cleanup() = server.playerList.players.forEach(ServerPlayer::closeContainer)
                override fun testPassed(test: GameTestInfo) = cleanup()
                override fun testFailed(test: GameTestInfo) = cleanup()
                override fun testStructureLoaded(test: GameTestInfo) = Unit
            })
            tracker = it
        }
    }
}
