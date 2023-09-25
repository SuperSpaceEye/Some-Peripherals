//package net.spaceeye.someperipherals.unused_for_now
//
//import java.lang.RuntimeException
//import kotlin.math.floor
//
//class IntNDimDDAIter(val ndim: Int, start: Array<Int>, stop: Array<Int>) {
//    private var start_p : Array<Int>
//    private var end_p   : Array<Int>
//    var cur             = Array<Int>(ndim){0}
//
//    private var d = Array<Int>(ndim){0}
//    private var s = Array<Int>(ndim){0}
//    private var c = Array<Int>(ndim){0}
//
//    private var ix: Int = 0
//
//    init {
//        if (start.size != ndim) {throw RuntimeException("Start dim != ndim")}
//        if (stop .size != ndim) {throw RuntimeException("Stop dim != ndim")}
//        start_p = start
//        end_p   = stop
//    }
//
//    fun start () {
//        ix = 0
//        for (i in 0 until ndim) {
//            cur[i]=start_p[i]; s[i]=0; d[i]=end_p[i]-start_p[i]
//            if (d[i]>0)     { s[i]= 1 }
//            if (d[i]<0)     { s[i]=-1; d[i]=-d[i] }
//            if (d[ix]<d[i]) { ix = i }
//        }
//        for (i in 0 until ndim) {c[i]=d[ix]}
//    }
//
//    fun start(fp0: Array<Double>) {
//        if (fp0.size != ndim) {throw RuntimeException("Array ndim mismatch")}
//        start()
//        for (i in 0 until ndim) {
//            if (s[i]<0) { c[i]=(d[ix]*(    fp0[i]-floor(fp0[i]))).toInt()}
//            if (s[i]>0) { c[i]=(d[ix]*(1.0-fp0[i]+floor(fp0[i]))).toInt()}
//        }
//    }
//
//    fun update(): Boolean {
//        for (i in 0 until ndim) {c[i]-=d[i];if(c[i]<=0){c[i]+=d[ix];cur[i]+=s[i]}}
//        return (cur[ix]!=end_p[ix]+s[ix])
//    }
//}