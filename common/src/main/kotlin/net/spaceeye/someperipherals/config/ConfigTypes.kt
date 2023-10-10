package net.spaceeye.someperipherals.config

import kotlin.reflect.KProperty

open class BaseConfigDelegate <T : Any>(var it:T, var range: Pair<T, T>? = null, var description: String="", val do_show:Boolean = true) {
    private lateinit var delegateRegister: DelegateRegisterItem

    operator fun getValue(thisRef: Any?, property: KProperty<*>):T {
        return ConfigDelegateRegister.getResolved(delegateRegister.resolved_name)() as T
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        ConfigDelegateRegister.setResolved(delegateRegister.resolved_name, value)
    }

    operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): BaseConfigDelegate<T> {
        delegateRegister = DelegateRegisterItem(thisRef, property, description, range, do_show = do_show)
        ConfigDelegateRegister.newEntry(delegateRegister, it)
        return this
    }
}

class CInt   (it:Int,     description: String="", range: Pair<Int, Int>?       = null, do_show: Boolean=true): BaseConfigDelegate<Int>    (it, range, description, do_show)
class CLong  (it:Long,    description: String="", range: Pair<Long, Long>?     = null, do_show: Boolean=true): BaseConfigDelegate<Long>   (it, range, description, do_show)
class CDouble(it:Double,  description: String="", range: Pair<Double, Double>? = null, do_show: Boolean=true): BaseConfigDelegate<Double> (it, range, description, do_show)
class CBool  (it:Boolean, description: String="", do_show: Boolean=true): BaseConfigDelegate<Boolean>(it, null, description, do_show)
class CString(it:String,  description: String="", do_show: Boolean=true): BaseConfigDelegate<String> (it, null, description, do_show)