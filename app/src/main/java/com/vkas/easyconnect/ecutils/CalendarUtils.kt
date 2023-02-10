package com.vkas.easyconnect.ecutils

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object CalendarUtils {
    val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

    private val dateThreadFormat: ThreadLocal<SimpleDateFormat?> =
        object : ThreadLocal<SimpleDateFormat?>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat("yyyy-MM-dd")
            }
        }

    //判断一个时间在另一个时间之后
    fun dateAfterDate(startTime: String?, endTime: String?): Boolean {
        val format = SimpleDateFormat("yyyy-MM-dd")
        try {
            val startDate: Date = format.parse(startTime)
            val endDate: Date = format.parse(endTime)
            val start: Long = startDate.getTime()
            val end: Long = endDate.getTime()
            if (end > start) {
                return true
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return false
        }
        return false
    }

    /**
     * @return 当前日期
     */
    fun formatDateNow(): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val date = Date()
        return simpleDateFormat.format(date)
    }
    /*
     * 将时间转换为时间戳
     */
    @Throws(ParseException::class)
    fun dateToStamp(s: String?): String? {
        val res: String
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val date: Date = simpleDateFormat.parse(s)
        val ts: Long = date.getTime()
        res = ts.toString()
        return res
    }

    /*
     * 将时间戳转换为时间
     */
    fun stampToDate(s: String): String? {
        val res: String
        @SuppressLint("SimpleDateFormat") val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val lt: String = s
        val date = Date(lt)
        res = simpleDateFormat.format(date)
        return res
    }

    /**
     * @return 当前详细日期
     */
    fun formatDetailedDateNow(isEnd: Boolean): String? {
        val simpleDateFormat: SimpleDateFormat
        if (isEnd) {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 23:59:59")
        } else {
            simpleDateFormat = SimpleDateFormat("yyyy-MM-dd 00:00:00")
        }
        val date = Date()
        return simpleDateFormat.format(date)
    }

    /**
     * 转2020-05-10样式
     *
     * @param time
     * @return
     */
    fun getTimeToDateFormat1(time: String?): String? {
        var cTime = ""
        val dateFormat = SimpleDateFormat("yyyy年MM月dd日")
        try {
            val date: Date = dateFormat.parseObject(time) as Date
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
            cTime = simpleDateFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return cTime
    }

    /**
     * 转年月日
     *
     * @param time
     * @return
     */
    fun getTimeToDateFormat2(time: String?): String? {
        var cTime = ""
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        try {
            val date: Date = dateFormat.parseObject(time) as Date
            val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
            cTime = simpleDateFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return cTime
    }
}