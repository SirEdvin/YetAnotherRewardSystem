package site.siredvin.yars.data

import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import site.siredvin.broccolium.modules.data.ForgeGeneratorSink
import site.siredvin.yars.YarsCore

@Mod.EventBusSubscriber(modid = YarsCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ForgeDataGenerators {
    @SubscribeEvent
    fun genData(event: GatherDataEvent) {
        ModDataProviders.add(ForgeGeneratorSink(event.generator.getVanillaPack(true), event.existingFileHelper, event.lookupProvider))
    }
}
