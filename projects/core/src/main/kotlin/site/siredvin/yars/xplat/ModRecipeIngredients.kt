package site.siredvin.yars.xplat

interface ModRecipeIngredients {
    companion object {
        private var impl: ModRecipeIngredients? = null

        fun configure(impl: ModRecipeIngredients) {
            this.impl = impl
        }

        fun get(): ModRecipeIngredients = checkNotNull(impl) { "Initialize YARS recipe ingredients first" }
    }
}
