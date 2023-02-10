package com.vkas.easyconnect.ecutils

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build

object ActivityUtils {
    var activities: HashMap<Class<*>, Activity>? = LinkedHashMap()
    /**
     * 是否在前台
     */
    /**
     * 添加Activity
     *
     * @param activity
     */
    fun addActivity(activity: Activity, clz: Class<*>) {
        activities!![clz] = activity
    }

    /**
     * 判断一个Activity 是否存在
     *
     * @param clz
     * @return
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    fun <T : Activity?> isActivityExist(clz: Class<T>): Boolean {
        val res: Boolean
        val activity: Activity? = getActivity(clz)
        res = if (activity == null) {
            false
        } else {
            !(activity.isFinishing() || activity.isDestroyed)
        }
        return res
    }

    /**
     * 获得指定activity
     *
     * @param clazz Activity 的类对象
     * @return
     */
    fun <T : Activity?> getActivity(clazz: Class<T>): Activity? {
        return activities!![clazz]
    }

    /**
     * 移除activity,代替finish
     *
     * @param activity
     */
    @JvmStatic
    fun removeActivity(activity: Activity) {
        if (activities!!.containsValue(activity)) {
            activities!!.remove(activity.javaClass)
        }
    }
}