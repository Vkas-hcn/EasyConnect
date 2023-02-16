package com.vkas.easyconnect.ecbase

import android.content.Context
import com.vkas.easyconnect.ecad.*
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.KLog
import java.util.*

class AdBase {
    companion object {
        fun getOpenInstance() = InstanceHelper.openLoadEc
        fun getHomeInstance() = InstanceHelper.homeLoadEc
        fun getResultInstance() = InstanceHelper.resultLoadEc
        fun getConnectInstance() = InstanceHelper.connectLoadEc
        fun getBackInstance() = InstanceHelper.backLoadEc
        private var idCounter = 0

    }

    val id = ++idCounter

    object InstanceHelper {
        val openLoadEc = AdBase()
        val homeLoadEc = AdBase()
        val resultLoadEc = AdBase()
        val connectLoadEc = AdBase()
        val backLoadEc = AdBase()
    }

    var appAdDataEc: Any? = null

    // 是否正在加载中
    var isLoadingEc = false

    //加载时间
    var loadTimeEc: Long = Date().time

    // 是否展示
    var whetherToShowEc = false

    // openIndex
    var adIndexEc = 0

    // 是否是第一遍轮训
    var isFirstRotation: Boolean = false

    /**
     * 广告加载前判断
     */
    fun advertisementLoadingEc(context: Context) {
        App.isAppOpenSameDayEc()
        if (EasyConnectUtils.isThresholdReached()) {
            KLog.d(Constant.logTagEc, "广告达到上线")
            return
        }
        if (isLoadingEc) {
            KLog.d(Constant.logTagEc, "${getInstanceName()}--广告加载中，不能再次加载")
            return
        }
        isFirstRotation = false
        if (appAdDataEc == null) {
            isLoadingEc = true
            KLog.d(Constant.logTagEc, "${getInstanceName()}--广告开始加载")
            loadStartupPageAdvertisementEc(context, EasyConnectUtils.getAdServerDataEc())
        }
        if (appAdDataEc != null && !whetherAdExceedsOneHour(loadTimeEc)) {
            isLoadingEc = true
            appAdDataEc = null
            KLog.d(Constant.logTagEc, "${getInstanceName()}--广告过期重新加载")
            loadStartupPageAdvertisementEc(context, EasyConnectUtils.getAdServerDataEc())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean =
        Date().time - loadTime < 60 * 60 * 1000

    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementEc(context: Context, adData: EcAdBean) {
        adLoaders[id]?.invoke(context, adData)
    }

    private val adLoaders = mapOf<Int, (Context, EcAdBean) -> Unit>(
        1 to { context, adData ->
            val adType = adData.ec_open.getOrNull(adIndexEc)?.ec_type
            if (adType == "screen") {
                EcLoadOpenAd.loadStartInsertAdEc(context, adData)
            } else {
                EcLoadOpenAd.loadOpenAdvertisementEc(context, adData)
            }
        },
        2 to { context, adData ->
            EcLoadHomeAd.loadHomeAdvertisementEc(context, adData)
        },
        3 to { context, adData ->
            EcLoadResultAd.loadResultAdvertisementEc(context, adData)
        },
        4 to { context, adData ->
            EcLoadConnectAd.loadConnectAdvertisementEc(context, adData)
        },
        5 to { context, adData ->
            EcLoadBackAd.loadBackAdvertisementEc(context, adData)
        }
    )

    /**
     * 获取实例名称
     */
    private fun getInstanceName(): String {
        when (id) {
            1 -> {
                return "open"
            }
            2 -> {
                return "home"
            }
            3 -> {
                return "result"
            }
            4 -> {
                return "connect"
            }
            5 -> {
                return "back"
            }
            else -> {
                return ""
            }
        }
    }
}