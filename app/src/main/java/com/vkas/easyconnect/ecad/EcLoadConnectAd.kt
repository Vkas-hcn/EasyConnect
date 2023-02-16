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
import com.vkas.easyconnect.ecbase.AdBase
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

object EcLoadConnectAd {
    private val adBase = AdBase.getConnectInstance()

    // 广告ID
    var idEc = ""


    /**
     * 加载首页插屏广告
     */
     fun loadConnectAdvertisementEc(context: Context, adData: EcAdBean) {
        val adRequest = AdRequest.Builder().build()
        idEc = takeSortedAdIDEc(adBase.adIndexEc, adData.ec_connect)
        KLog.d(
            logTagEc,
            "connect--插屏广告id=$idEc;权重=${adData.ec_connect.getOrNull(adBase.adIndexEc)?.ec_weight}"
        )

        InterstitialAd.load(
            context,
            idEc,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagEc, "connect---连接插屏加载失败=$it") }
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = null
                    if (adBase.adIndexEc < adData.ec_connect.size - 1) {
                        adBase.adIndexEc++
                        loadConnectAdvertisementEc(context, adData)
                    } else {
                        adBase.adIndexEc = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adBase.loadTimeEc = Date().time
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = interstitialAd
                    adBase.adIndexEc = 0
                    KLog.d(logTagEc, "connect---连接插屏加载成功")
                }
            })
    }

    /**
     * connect插屏广告回调
     */
    private fun connectScreenAdCallback() {
        (adBase.appAdDataEc as? InterstitialAd)?.fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagEc, "connect插屏广告点击")
                    recordNumberOfAdClickEc()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagEc, "关闭connect插屏广告=${App.isBackDataEc}")
                    LiveEventBus.get<Boolean>(Constant.PLUG_EC_ADVERTISEMENT_SHOW)
                        .post(App.isBackDataEc)

                    adBase.appAdDataEc = null
                    adBase.whetherToShowEc = false
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    // Called when ad fails to show.
                    KLog.d(logTagEc, "Ad failed to show fullscreen content.")
                    adBase.appAdDataEc = null
                    adBase.whetherToShowEc = false
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    KLog.e("TAG", "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataEc = null
                    recordNumberOfAdDisplaysEc()
                    // Called when ad is shown.
                    adBase.whetherToShowEc = true
                    KLog.d(logTagEc, "connect----show")
                }
            }
    }

    /**
     * 展示Connect广告
     */
    fun displayConnectAdvertisementEc(activity: AppCompatActivity): Boolean {
        if (adBase.appAdDataEc == null) {
            KLog.d(logTagEc, "connect--插屏广告加载中。。。")
            return false
        }

        if (adBase.whetherToShowEc || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagEc, "connect--前一个插屏广告展示中或者生命周期不对")
            return false
        }
        connectScreenAdCallback()
        activity.lifecycleScope.launch(Dispatchers.Main) {
            (adBase.appAdDataEc as InterstitialAd).show(activity)
        }
        return true
    }
}