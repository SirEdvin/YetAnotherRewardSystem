package site.siredvin.yars.testmod.client

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
import org.slf4j.LoggerFactory
import site.siredvin.testiarium.api.Timeouts
import kotlin.system.exitProcess

object ClientTestHooks {
    private const val LEVEL_NAME = "testiarium-client"
    private const val STARTUP_DELAY = 5 * Timeouts.SECOND
    private val log = LoggerFactory.getLogger(ClientTestHooks::class.java)
    private val enabled = System.getProperty("testiarium.client") != null
    private var loadedWorld = false
    private var tracker: MultipleTestTracker? = null
    private var startupDelay = STARTUP_DELAY
    private var finished = false
    private var reopenedFrom: MinecraftServer? = null

    @JvmStatic
    fun onOpenScreen(screen: Screen): Boolean {
        if (!enabled || loadedWorld || screen !is TitleScreen && screen !is AccessibilityOnboardingScreen) return false
        val minecraft = Minecraft.getInstance()
        // Fabric initializes its title screen before the first resource reload has populated renderers.
        if (minecraft.overlay != null) return false
        loadedWorld = true
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

    @JvmStatic
    fun onServerTick(server: MinecraftServer) {
        if (!enabled) return
        if (reopenedFrom != null && server !== reopenedFrom && server.overworld().players().isNotEmpty()) {
            if (startupDelay-- > 0) return
            val trades = site.siredvin.yars.common.rewardshop.RewardShopTrades
            val valid = trades.all(site.siredvin.yars.common.block.DebugShops.MANUAL_ID).size == 9 &&
                trades.all(site.siredvin.yars.common.block.DebugShops.AUTOMATIC_ID).size == 9 &&
                trades.find(net.minecraft.resources.ResourceLocation("yars_test", "previous_world_only")) == null
            log.info("YARS_SECOND_INTEGRATED_WORLD {}", if (valid) "PASS" else "FAIL")
            reopenedFrom = null
            Minecraft.getInstance().execute {
                Minecraft.getInstance().level?.disconnect()
                Minecraft.getInstance().clearLevel()
                Minecraft.getInstance().stop()
                exitProcess(if (valid) 0 else 2)
            }
            return
        }
        if (finished) return
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
        if (exitCode == 0) {
            site.siredvin.yars.common.rewardshop.DebugShopTrades.registration().apply {
                trade("yars_test:previous_world_only").simple(-1, net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL), net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND))
                attach(site.siredvin.yars.common.block.DebugShops.MANUAL_ID.toString(), "yars_test:previous_world_only")
                replaceTrades()
            }
            reopenedFrom = server
            startupDelay = STARTUP_DELAY
            Minecraft.getInstance().execute {
                Minecraft.getInstance().level?.disconnect()
                Minecraft.getInstance().clearLevel()
                Minecraft.getInstance().createWorldOpenFlows().loadLevel(TitleScreen(), LEVEL_NAME)
            }
            return
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
