package com.vkas.easyconnect.ecad

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
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
import com.xuexiang.xutil.net.JsonUtil
import java.util.*

object EcLoadOpenAd {
    private val adBase = AdBase.getOpenInstance()
    /**
     * 加载启动页广告
     */
    private fun loadStartupPageAdvertisementEc(context: Context, adData: EcAdBean) {
        if (adData.ec_open.getOrNull(adBase.adIndexEc)?.ec_type == "screen") {
            loadStartInsertAdEc(context, adData)
        } else {
            loadOpenAdvertisementEc(context, adData)
        }
    }

    /**
     * 加载开屏广告
     */
     fun loadOpenAdvertisementEc(context: Context, adData: EcAdBean) {
        KLog.e("loadOpenAdvertisementEc", "adData().ec_open=${JsonUtil.toJson(adData.ec_open)}")
        KLog.e(
            "loadOpenAdvertisementEc",
            "id=${JsonUtil.toJson(takeSortedAdIDEc(adBase.adIndexEc, adData.ec_open))}"
        )

        val id = takeSortedAdIDEc(adBase.adIndexEc, adData.ec_open)

        KLog.d(logTagEc, "open--开屏广告id=$id;权重=${adData.ec_open.getOrNull(adBase.adIndexEc)?.ec_weight}")
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            id,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    adBase.loadTimeEc = Date().time
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = ad

                    KLog.d(logTagEc, "open--开屏广告加载成功")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = null
                    if (adBase.adIndexEc < adData.ec_open.size - 1) {
                        adBase.adIndexEc++
                        loadStartupPageAdvertisementEc(context, adData)
                    } else {
                        adBase.adIndexEc = 0
                        if(!adBase.isFirstRotation){
                            AdBase.getOpenInstance().advertisementLoadingEc(context)
                            adBase.isFirstRotation =true
                        }
                    }
                    KLog.d(logTagEc, "open--开屏广告加载失败: " + loadAdError.message)
                }
            }
        )
    }


    /**
     * 开屏广告回调
     */
    private fun advertisingOpenCallbackEc() {
        if (adBase.appAdDataEc !is AppOpenAd) {
            return
        }
        (adBase.appAdDataEc as AppOpenAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                //取消全屏内容
                override fun onAdDismissedFullScreenContent() {
                    KLog.d(logTagEc, "open--关闭开屏内容")
                    adBase.whetherToShowEc = false
                    adBase.appAdDataEc = null
                    if (!App.whetherBackgroundEc) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
                }

                //全屏内容无法显示时调用
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    adBase.whetherToShowEc = false
                    adBase.appAdDataEc = null
                    KLog.d(logTagEc, "open--全屏内容无法显示时调用")
                }

                //显示全屏内容时调用
                override fun onAdShowedFullScreenContent() {
                    adBase.appAdDataEc = null
                    adBase.whetherToShowEc = true
                    recordNumberOfAdDisplaysEc()
                    adBase.adIndexEc = 0
                    KLog.d(logTagEc, "open---开屏广告展示")
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    KLog.d(logTagEc, "open---点击open广告")
                    recordNumberOfAdClickEc()
                }
            }
    }

    /**
     * 展示Open广告
     */
    fun displayOpenAdvertisementEc(activity: AppCompatActivity): Boolean {

        if (adBase.appAdDataEc == null) {
            KLog.d(logTagEc, "open---开屏广告加载中。。。")
            return false
        }
        if (adBase.whetherToShowEc || activity.lifecycle.currentState != Lifecycle.State.RESUMED) {
            KLog.d(logTagEc, "open---前一个开屏广告展示中或者生命周期不对")
            return false
        }
        if (adBase.appAdDataEc is AppOpenAd) {
            advertisingOpenCallbackEc()
            (adBase.appAdDataEc as AppOpenAd).show(activity)
        } else {
            startInsertScreenAdCallbackEc()
            (adBase.appAdDataEc as InterstitialAd).show(activity)
        }
        return true
    }

    /**
     * 加载启动页插屏广告
     */
     fun loadStartInsertAdEc(context: Context, adData: EcAdBean) {
        val adRequest = AdRequest.Builder().build()
        val id = takeSortedAdIDEc(adBase.adIndexEc, adData.ec_open)
        KLog.d(
            logTagEc,
            "open--插屏广告id=$id;权重=${adData.ec_open.getOrNull(adBase.adIndexEc)?.ec_weight}"
        )

        InterstitialAd.load(
            context,
            id,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { KLog.d(logTagEc, "open---连接插屏加载失败=$it") }
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = null
                    if (adBase.adIndexEc < adData.ec_open.size - 1) {
                        adBase.adIndexEc++
                        loadStartupPageAdvertisementEc(context, adData)
                    } else {
                        adBase.adIndexEc = 0
                    }
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    adBase.loadTimeEc = Date().time
                    adBase.isLoadingEc = false
                    adBase.appAdDataEc = interstitialAd
                    KLog.d(logTagEc, "open--启动页插屏加载完成")
                }
            })
    }

    /**
     * StartInsert插屏广告回调
     */
    private fun startInsertScreenAdCallbackEc() {
        if (adBase.appAdDataEc !is InterstitialAd) {
            return
        }
        (adBase.appAdDataEc as InterstitialAd).fullScreenContentCallback =
            object : FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    KLog.d(logTagEc, "open--插屏广告点击")
                    recordNumberOfAdClickEc()
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    KLog.d(logTagEc, "open--关闭StartInsert插屏广告${App.isBackDataEc}")
                    if (!App.whetherBackgroundEc) {
                        LiveEventBus.get<Boolean>(Constant.OPEN_CLOSE_JUMP)
                            .post(true)
                    }
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
                    adBase.adIndexEc = 0
                    KLog.d(logTagEc, "open----插屏show")
                }
            }
    }
}