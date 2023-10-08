package net.spaceeye.someperipherals.config

import kotlin.reflect.KProperty

open class BaseConfigDelegate <T : Any>(var it:T, var description: String="") {
    private lateinit var delegateRegister: DelegateRegisterItem
    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return ConfigDelegateRegister.getResolved(delegateRegister.resolved_name)() as T
    }
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        ConfigDelegateRegister.setResolved(delegateRegister.resolved_name, value)
    }

    private fun T_to_str(): String {
        return when (it) {
            is Int -> "int"
            is Double -> "double"
            is Boolean -> "bool"
            is String -> "str"

            else -> throw RuntimeException("Unknown type")
        }
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseConfigDelegate<T> {
        delegateRegister = DelegateRegisterItem(thisRef, property, T_to_str(), description)
        ConfigDelegateRegister.newEntry(delegateRegister, it)
        return this
    }
}

class CInt   (it:Int,     description: String=""): BaseConfigDelegate<Int>    (it, description)
class CDouble(it:Double,  description: String=""): BaseConfigDelegate<Double> (it, description)
class CBool  (it:Boolean, description: String=""): BaseConfigDelegate<Boolean>(it, description)
class CString(it:String,  description: String=""): BaseConfigDelegate<String> (it, description)