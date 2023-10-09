package net.spaceeye.someperipherals.config

import kotlin.reflect.KProperty

open class BaseConfigDelegate <T : Any>(var it:T, var range: Pair<T, T>? = null, var description: String="") {
    private lateinit var delegateRegister: DelegateRegisterItem

    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return ConfigDelegateRegister.getResolved(delegateRegister.resolved_name)() as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        ConfigDelegateRegister.setResolved(delegateRegister.resolved_name, value)
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseConfigDelegate<T> {
        delegateRegister = DelegateRegisterItem(thisRef, property, description, range)
        ConfigDelegateRegister.newEntry(delegateRegister, it)
        return this
    }
}

class CInt   (it:Int,     description: String="", range: Pair<Int, Int>?       = null): BaseConfigDelegate<Int>    (it, range, description)
class CLong  (it:Long,    description: String="", range: Pair<Long, Long>?     = null): BaseConfigDelegate<Long>   (it, range, description)
class CDouble(it:Double,  description: String="", range: Pair<Double, Double>? = null): BaseConfigDelegate<Double> (it, range, description)
class CBool  (it:Boolean, description: String=""): BaseConfigDelegate<Boolean>(it, null, description)
class CString(it:String,  description: String=""): BaseConfigDelegate<String> (it, null, description)