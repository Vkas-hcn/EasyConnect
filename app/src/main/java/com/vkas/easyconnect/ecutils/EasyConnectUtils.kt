package com.vkas.easyconnect.ecutils

import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.gson.reflect.TypeToken
import com.vkas.easyconnect.BuildConfig
import com.vkas.easyconnect.R
import com.vkas.easyconnect.ecapp.App.Companion.mmkvEc
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecbean.EcDetailBean
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.xuexiang.xui.utils.ResUtils.getString
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.resource.ResourceUtils
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object EasyConnectUtils {
    /**
     * 获取Fast ip
     */
    fun getFastIpEc2(): EcVpnBean {
        val ufVpnBean: MutableList<EcVpnBean> = getLocalServerData()
        var intersectionList = findFastAndOrdinaryIntersection(ufVpnBean)
        if (intersectionList.size <= 0) {
            intersectionList = ufVpnBean
        }

        intersectionList.shuffled().take(1).forEach {
            it.ec_best = true
            it.ec_country = getString(R.string.fast_service)
            return it
        }
        intersectionList[0].ec_best = true
        return intersectionList[0]
    }
    fun getFastIpEc(): EcVpnBean {
        val ufVpnBean: MutableList<EcVpnBean> = getLocalServerData()
        val intersectionList = findFastAndOrdinaryIntersection(ufVpnBean).takeIf { it.isNotEmpty() } ?: ufVpnBean
        return intersectionList.shuffled().first().apply {
            ec_best = true
            ec_country = getString(R.string.fast_service)
        }
    }
    /**
     * 获取本地服务器数据
     */
    fun getLocalServerData(): MutableList<EcVpnBean> {
        return if (Utils.isNullOrEmpty(mmkvEc.decodeString(Constant.PROFILE_EC_DATA))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert(Constant.VPN_LOCAL_FILE_NAME_EC),
                object : TypeToken<MutableList<EcVpnBean>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvEc.decodeString(Constant.PROFILE_EC_DATA),
                object : TypeToken<MutableList<EcVpnBean>?>() {}.type
            )
        }
    }

    /**
     * 获取本地Fast服务器数据
     */
    private fun getLocalFastServerData(): MutableList<String> {
        return if (Utils.isNullOrEmpty(mmkvEc.decodeString(Constant.PROFILE_EC_DATA_FAST))) {
            JsonUtil.fromJson(
                ResourceUtils.readStringFromAssert(Constant.FAST_LOCAL_FILE_NAME_EC),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        } else {
            JsonUtil.fromJson(
                mmkvEc.decodeString(Constant.PROFILE_EC_DATA_FAST),
                object : TypeToken<MutableList<String>?>() {}.type
            )
        }
    }
    /**
     *
     */

    /**
     * 找出fast与普通交集
     */
    private fun findFastAndOrdinaryIntersection(ufVpnBeans: MutableList<EcVpnBean>): MutableList<EcVpnBean> {
        val intersectionList: MutableList<EcVpnBean> = ArrayList()
        getLocalFastServerData().forEach { fast ->
            ufVpnBeans.forEach { skServiceBean ->
                if (fast == skServiceBean.ec_ip) {
                    intersectionList.add(skServiceBean)
                }
            }
        }
        return intersectionList
    }

    /**
     * 广告排序
     */
    private fun adSortingEc(elAdBean: EcAdBean): EcAdBean {
        val adBean: EcAdBean = EcAdBean()
        val elOpen = elAdBean.ec_open.sortedWith(compareByDescending { it.ec_weight })
        val elBack = elAdBean.ec_back.sortedWith(compareByDescending { it.ec_weight })

        val ufVpn = elAdBean.ec_vpn.sortedWith(compareByDescending { it.ec_weight })
        val elResult = elAdBean.ec_result.sortedWith(compareByDescending { it.ec_weight })
        val elConnect = elAdBean.ec_connect.sortedWith(compareByDescending { it.ec_weight })


        adBean.ec_open = elOpen.toMutableList()
        adBean.ec_back = elBack.toMutableList()

        adBean.ec_vpn = ufVpn.toMutableList()
        adBean.ec_result = elResult.toMutableList()
        adBean.ec_connect = elConnect.toMutableList()

        adBean.ec_show_num = elAdBean.ec_show_num
        adBean.ec_click_num = elAdBean.ec_click_num
        return adBean
    }

    /**
     * 取出排序后的广告ID
     */
    fun takeSortedAdIDEc(index: Int, elAdDetails: MutableList<EcDetailBean>): String {
        return elAdDetails.getOrNull(index)?.ec_id ?: ""
    }

    /**
     * 获取广告服务器数据
     */
    fun getAdServerDataEc(): EcAdBean {
        val serviceData: EcAdBean =
            if (Utils.isNullOrEmpty(mmkvEc.decodeString(Constant.ADVERTISING_EC_DATA))) {
                JsonUtil.fromJson(
                    ResourceUtils.readStringFromAssert(Constant.AD_LOCAL_FILE_NAME_EC),
                    object : TypeToken<
                            EcAdBean?>() {}.type
                )
            } else {
                JsonUtil.fromJson(
                    mmkvEc.decodeString(Constant.ADVERTISING_EC_DATA),
                    object : TypeToken<EcAdBean?>() {}.type
                )
            }
        return adSortingEc(serviceData)
    }

    /**
     * 是否达到阀值
     */
    fun isThresholdReached(): Boolean {
        val clicksCount = mmkvEc.decodeInt(Constant.CLICKS_EC_COUNT, 0)
        val showCount = mmkvEc.decodeInt(Constant.SHOW_EC_COUNT, 0)
        KLog.e("TAG", "clicksCount=${clicksCount}, showCount=${showCount}")
        KLog.e(
            "TAG",
            "ec_click_num=${getAdServerDataEc().ec_click_num}, getAdServerData().ec_show_num=${getAdServerDataEc().ec_show_num}"
        )
        if (clicksCount >= getAdServerDataEc().ec_click_num || showCount >= getAdServerDataEc().ec_show_num) {
            return true
        }
        return false
    }

    /**
     * 记录广告展示次数
     */
    fun recordNumberOfAdDisplaysEc() {
        var showCount = mmkvEc.decodeInt(Constant.SHOW_EC_COUNT, 0)
        showCount++
        MmkvUtils.set(Constant.SHOW_EC_COUNT, showCount)
    }

    /**
     * 记录广告点击次数
     */
    fun recordNumberOfAdClickEc() {
        var clicksCount = mmkvEc.decodeInt(Constant.CLICKS_EC_COUNT, 0)
        clicksCount++
        MmkvUtils.set(Constant.CLICKS_EC_COUNT, clicksCount)
    }

    /**
     * 通过国家获取国旗
     */
    fun getFlagThroughCountryEc(ec_country: String): Int {
        when (ec_country) {
            "Faster server" -> {
                return R.mipmap.ic_fast
            }
            "Japan" -> {
                return R.mipmap.ic_japan
            }
            "United Kingdom" -> {
                return R.mipmap.ic_unitedkingdom
            }
            "United States" -> {
                return R.mipmap.ic_usa
            }
            "Australia" -> {
                return R.mipmap.ic_australia
            }
            "Belgium" -> {
                return R.mipmap.ic_belgium
            }
            "Brazil" -> {
                return R.mipmap.ic_brazil
            }
            "Canada" -> {
                return R.mipmap.ic_canada
            }
            "France" -> {
                return R.mipmap.ic_france
            }
            "Germany" -> {
                return R.mipmap.ic_germany
            }
            "India" -> {
                return R.mipmap.ic_india
            }
            "Ireland" -> {
                return R.mipmap.ic_ireland
            }
            "Italy" -> {
                return R.mipmap.ic_italy
            }
            "SouthKorea" -> {
                return R.mipmap.ic_koreasouth
            }
            "Netherlands" -> {
                return R.mipmap.ic_netherlands
            }
            "Newzealand" -> {
                return R.mipmap.ic_newzealand
            }
            "Norway" -> {
                return R.mipmap.ic_norway
            }
            "Russianfederation" -> {
                return R.mipmap.ic_russianfederation
            }
            "Singapore" -> {
                return R.mipmap.ic_singapore
            }
            "Sweden" -> {
                return R.mipmap.ic_sweden
            }
            "Switzerland" -> {
                return R.mipmap.ic_switzerland
            }
        }

        return R.mipmap.ic_fast
    }

    fun getIpInformation() {
        val sb = StringBuffer()
        try {
            val url = URL("https://ip.seeip.org/geoip/")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            val code = conn.responseCode
            if (code == 200) {
                val `is` = conn.inputStream
                val b = ByteArray(1024)
                var len: Int
                while (`is`.read(b).also { len = it } != -1) {
                    sb.append(String(b, 0, len, Charset.forName("UTF-8")))
                }
                `is`.close()
                conn.disconnect()
                KLog.e("state", "sb==${sb.toString()}")
                MmkvUtils.set(Constant.IP_INFORMATION, sb.toString())
            } else {
                MmkvUtils.set(Constant.IP_INFORMATION, "")
                KLog.e("state", "code==${code.toString()}")
            }
        } catch (var1: Exception) {
            MmkvUtils.set(Constant.IP_INFORMATION, "")
            KLog.e("state", "Exception==${var1.message}")
        }
    }

//    /**
//     * 埋点
//     */
//    fun getBuriedPointEc(name: String) {
//        if (!BuildConfig.DEBUG) {
//            Firebase.analytics.logEvent(name, null)
//        } else {
//            KLog.d(logTagEc, "触发埋点----name=${name}")
//        }
//    }
//
//    /**
//     * 埋点
//     */
//    fun getBuriedPointUserTypeEc(name: String, value: String) {
//        if (!BuildConfig.DEBUG) {
//            Firebase.analytics.setUserProperty(name, value)
//        } else {
//            KLog.d(logTagEc, "触发埋点----name=${name}-----value=${value}")
//        }
//    }
//    /**
//     * 埋点连接时长
//     */
//    fun getBuriedPointConnectionTimeEc(name: String,time:Int) {
//        if (!BuildConfig.DEBUG) {
//            Firebase.analytics.logEvent(name, bundleOf("time" to time))
//        } else {
//            KLog.d(logTagEc, "触发埋点----name=${name}---time=${time}")
//        }
//    }
}