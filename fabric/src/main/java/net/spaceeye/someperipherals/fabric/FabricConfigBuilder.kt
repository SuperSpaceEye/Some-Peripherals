package net.spaceeye.someperipherals.fabric

import net.minecraftforge.api.ModLoadingContext
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.config.ModConfig
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.config.ConfigBuilder
import net.spaceeye.someperipherals.config.ConfigValueGetSet
import java.lang.AssertionError

class FabricConfigBuilder: ConfigBuilder() {
    val BUILDER = ForgeConfigSpec.Builder()
    var SPEC: ForgeConfigSpec? = null

    override fun pushNamespace(namespace: String) { BUILDER.push(namespace) }
    override fun popNamespace() { BUILDER.pop() }
    override fun beginBuilding() {}
    override fun finishBuilding(type: String) {
        SPEC = BUILDER.build()

        val type = when(type) {
            "client" -> ModConfig.Type.CLIENT
            "server" -> ModConfig.Type.SERVER
            "common" -> ModConfig.Type.COMMON
            else -> throw AssertionError("Invalid config type $type")
        }

        ModLoadingContext.registerConfig(SomePeripherals.MOD_ID, type, SPEC, "some_peripherals-$type.toml")
    }

    override fun <T : Any> makeItem(name: String, defaultValue: T, description: String): ConfigValueGetSet {
        val newVal = BUILDER.comment(description).define(name, defaultValue)
        return ConfigValueGetSet(
            { newVal.get() },
            { newVal.set(it as T) }
        )
    }
}