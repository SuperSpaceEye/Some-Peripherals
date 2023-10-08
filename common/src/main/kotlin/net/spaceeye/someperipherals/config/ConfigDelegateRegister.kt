package net.spaceeye.someperipherals.config

import net.spaceeye.someperipherals.PlatformUtils
import net.spaceeye.someperipherals.SomePeripheralsConfig
import kotlin.reflect.KProperty
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

var DelegateRegisterItemCount = 0
data class DelegateRegisterItem(
    val parentReg: Any?,
    val property: KProperty<*>,
    val val_type: String,
    val description: String,
    var resolved_name:String="") {
    val counter = DelegateRegisterItemCount
    init { DelegateRegisterItemCount++ }

    override fun hashCode(): Int {
        return counter
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}
abstract class ConfigSubDirectory

object ConfigDelegateRegister {
    private var default_parameters = hashMapOf<DelegateRegisterItem, Any>()
    private var registers = hashMapOf<KProperty<*>, DelegateRegisterItem>()

    private var resolved_get = hashMapOf<String, () -> Any>()
    private var resolved_set = hashMapOf<String, (Any) -> Unit>()

    fun newEntry(entry: DelegateRegisterItem, it: Any) {
        default_parameters[entry] = it
        registers[entry.property] = entry
    }

    fun getResolved(resolved_name: String): () -> Any = resolved_get[resolved_name]!!
    fun setResolved(resolved_name: String, it: Any) = resolved_set[resolved_name]!!(it)

    private fun getEntry(it: KProperty<*>): DelegateRegisterItem? = registers[it]
    private fun resolveEntry(it: DelegateRegisterItem) {
        val default_parameter = default_parameters[it]!!

        val getSet = SomePeripheralsConfig.server_config_holder.makeItem(
            it.property.name, default_parameter, it.description)

        resolved_get[it.resolved_name] = getSet.get
        resolved_set[it.resolved_name] = getSet.set
    }

    private fun reflectResolveConfigPaths(cls: Any, str_path: String = "", name: String) {
        SomePeripheralsConfig.server_config_holder.pushNamespace(name)

        val resolve_later = mutableListOf<Any>()
        for (item in cls::class.declaredMemberProperties) {
            if (item.visibility != KVisibility.PUBLIC) {continue}
            val entry = getEntry(item)

            if (entry == null) { item.getter.call(cls)?.let { if (it is ConfigSubDirectory) resolve_later.add(it) }; continue }

            entry.resolved_name = str_path + "." + item.name
            resolveEntry(entry)
        }

        for (item in resolve_later) {
            reflectResolveConfigPaths(item, str_path + "." + item::class.simpleName, item::class.simpleName!!)
        }

        SomePeripheralsConfig.server_config_holder.popNamespace()
    }

    fun initConfig() {
        SomePeripheralsConfig.server_config_holder = PlatformUtils.getConfig()
        SomePeripheralsConfig.server_config_holder.beginBuilding()
        reflectResolveConfigPaths(SomePeripheralsConfig.SERVER, "SomePeripheralsConfig", "SomePeripheralsConfig")
        SomePeripheralsConfig.server_config_holder.finishBuilding("server")

        SomePeripheralsConfig.client_config_holder = PlatformUtils.getConfig()
        SomePeripheralsConfig.client_config_holder.beginBuilding()
        reflectResolveConfigPaths(SomePeripheralsConfig.CLIENT, "SomePeripheralsConfig", "SomePeripheralsConfig")
        SomePeripheralsConfig.client_config_holder.finishBuilding("client")

        SomePeripheralsConfig.common_config_holder = PlatformUtils.getConfig()
        SomePeripheralsConfig.common_config_holder.beginBuilding()
        reflectResolveConfigPaths(SomePeripheralsConfig.COMMON, "SomePeripheralsConfig", "SomePeripheralsConfig")
        SomePeripheralsConfig.common_config_holder.finishBuilding("common")

        default_parameters.clear()
        registers.clear()
    }
}