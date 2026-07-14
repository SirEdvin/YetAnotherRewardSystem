package site.siredvin.yars.fabric

import site.siredvin.broccolium.modules.platform.FabricInnerBasePlatform
import site.siredvin.yars.YarsCore

object YarsFabricPlatform : FabricInnerBasePlatform() {
    override val modID: String
        get() = YarsCore.MOD_ID
}
