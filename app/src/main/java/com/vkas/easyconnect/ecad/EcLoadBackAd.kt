package com.vkas.easyconnect.ecad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getAdServerDataEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdClickEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdDisplaysEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.takeSortedAdIDEc
import com.vkas.easyconnect.ecutils.KLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class EcLoadBackAd {
    companion object {
        fun getInstance() = InstanceHelper.backLoadEc
    }

    object InstanceHelper {
        val backLoadEc = EcLoadBackAd()
    }
    var appAdDataEc: InterstitialAd? = null

    // 是否正在加载中
    var isLoadingEc = false

    //加载时间
    private var loadTimeEc: Long = Date().time

    // 是否展示
    var whetherToShowEc = false

    // openIndex
    var adIndexEc = 0

    /**
     * 广告加载前判断
     */
    fun advertisementLoadingEc(context: Context) {
        App.isAppOpenSameDayEc()
        if (EasyConnectUtils.isThresholdReached()) {
            KLog.d(logTagEc, "广告达到上线")
            return
        }
        KLog.d(logTagEc, "back--isLoading=${isLoadingEc}")

        if (isLoadingEc) {
            KLog.d(logTagEc, "back--广告加载中，不能再次加载")
            return
        }

        if(appAdDataEc == null){
            isLoadingEc = true
            loadBackAdvertisementEc(context,getAdServerDataEc())
        }
        if (appAdDataEc != null && !whetherAdExceedsOneHour(loadTimeEc)) {
            isLoadingEc = true
            appAdDataEc =null
            loadBackAdvertisementEc(context,getAdServerDataEc())
        }
    }

    /**
     * 广告是否超过过期（false:过期；true：未过期）
     */
    private fun whetherAdExceedsOneHour(loadTime: Long): Boolean {
        val dateDifference: Long = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour
    }


    /**
     * 加载首页插屏广告
     */
    private fun loadBackAdvertisementEc(context: Context, adData: EcAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDEc(adIndexEc, adData.ec_back)
        KLog.d(logTagEc, "back--插屏广告id=$id;权重=${adData.ec_back.getOrNull(adIndexEc)?.ec_weight}")

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let {
                        KLog.d(logTagEc, "back---连接插屏加载失败=$it") }
                    isLoadingEc = false
                    appAdDataEc = null
                    if (adIndexEc < adData.ec_back.size - 1) {
                        adIndexEc++
                        loadBackAdvertisementEc(context,adData)
                    }else{
                        adIndexEc = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    loadTimeEc = Date().time
                    isLoadingEc = false
                    appAdDataEc = interstitialAd
                    adIndexEc = 0
                    KLog.d(logTagEc, "back---返回插屏加载成功")
                }
            })
    }

    /**
     * back插屏广告回调
     */
    private fun backScreenAdCallback() {
        appAdDataEc?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagEc, "back插屏广告点击")
                    recordNumberOfAdClickEc()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagEc, "关闭back插屏广告${App.isBackDataEc}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_EC_BACK_AD_SHOW)
                        .post(App.isBackDataEc)
                    appAdDataEc = null
                    whetherToShowEc = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagEc, "Ad failed to show fullscreen content.")
                    appAdDataEc = null
                    whetherToShowEc = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    appAdDataEc = null
                    recordNumberOfAdDisplaysEc()
                    // Called when ad is shown.
                    whetherToShowEc = true
                    KLog.d(logTagEc, "back----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayBackAdvertisementEc(activity: AppCompatActivity): Boolean {
        if (appAdDataEc == null) {
            KLog.d(logTagEc, "back--插屏广告加载中。。。")
            return false
        }
        if (whetherToShowEc || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagEc, "back--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        backScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (appAdDataEc as InterstitialAd).show(activity)
        }
        return true
    }
}