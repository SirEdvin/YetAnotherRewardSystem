package site.siredvin.yars.data

import site.siredvin.broccolium.modules.data.api.TextRecord
import site.siredvin.yars.YarsCore

enum class ModText : TextRecord {
    CREATIVE_TAB,
    ;

    override val textID: String by lazy {
        "text.${YarsCore.MOD_ID}.${name.lowercase()}"
    }
}
