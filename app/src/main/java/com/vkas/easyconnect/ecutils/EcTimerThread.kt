package com.vkas.easyconnect.ecutils

import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.ecenevt.Constant
import kotlinx.coroutines.*
import java.text.DecimalFormat
object EcTimerThread {
    private val job = Job()
    private val timerThread = CoroutineScope(job)
    var skTime = 0
    var isStopThread =true
    /**
     * 发送定时器信息
     */
    fun sendTimerInformation() {
        timerThread.launch {
            while (isActive) {
                skTime++
                if (!isStopThread) {
//                    if(skTime==300){
//                        LiveEventBus.get<Boolean>(Constant.STOP_VPN_CONNECTION)
//                            .post(true)
//                    }else{
                        LiveEventBus.get<String>(Constant.TIMER_EC_DATA)
                            .post(formatTime(skTime))
//                    }
                }
                delay(1000)
            }
        }
    }
    /**
     * 开始计时
     */
    fun startTiming(){
        if(isStopThread){
            skTime = 0
        }
        isStopThread =false
    }
    /**
     * 结束计时
     */
    fun endTiming(){
        MmkvUtils.set(Constant.LAST_TIME,formatTime(skTime))
        MmkvUtils.set(Constant.LAST_TIME_SECOND,skTime)
        skTime = 0
        isStopThread =true
        LiveEventBus.get<String>(Constant.TIMER_EC_DATA)
            .post(formatTime(0))

    }
    /**
     * 设置时间格式
     */
    private fun formatTime(timerData:Int):String{
        val hh: String = DecimalFormat("00").format(timerData / 3600)
        val mm: String = DecimalFormat("00").format(timerData % 3600 / 60)
        val ss: String = DecimalFormat("00").format(timerData % 60)
        return "$hh:$mm:$ss"
    }
}