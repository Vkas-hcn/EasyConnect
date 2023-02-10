package com.vkas.easyconnect.ecad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.easyconnect.R
import com.vkas.easyconnect.databinding.ActivityResultEcBinding
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getAdServerDataEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdClickEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdDisplaysEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.takeSortedAdIDEc
import com.vkas.easyconnect.ecutils.KLog
import java.util.*

class EcLoadResultAd {
    companion object {
        fun getInstance() = InstanceHelper.openLoadEc
    }

    object InstanceHelper {
        val openLoadEc = EcLoadResultAd()
    }

    var appAdDataEc: NativeAd? = null

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
        KLog.d(logTagEc, "result--isLoading=${isLoadingEc}")

        if (isLoadingEc) {
            KLog.d(logTagEc, "result--广告加载中，不能再次加载")
            return
        }
        if (appAdDataEc == null) {
            isLoadingEc = true
            loadResultAdvertisementEc(context, getAdServerDataEc())
        }
        if (appAdDataEc != null && !whetherAdExceedsOneHour(loadTimeEc)) {
            isLoadingEc = true
            appAdDataEc = null
            loadResultAdvertisementEc(context, getAdServerDataEc())
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
     * 加载result原生广告
     */
    private fun loadResultAdvertisementEc(context: Context, adData: EcAdBean) {
        val id = takeSortedAdIDEc(adIndexEc, adData.ec_result)
        KLog.d(
            logTagEc,
            "result---原生广告id=$id;权重=${adData.ec_result.getOrNull(adIndexEc)?.ec_weight}"
        )

        val homeNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOelions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_LEFT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        homeNativeAds.withNativeAdOptions(adOelions)
        homeNativeAds.forNativeAd {
            appAdDataEc = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                isLoadingEc = false
                appAdDataEc = null
                KLog.d(logTagEc, "result---加载result原生加载失败: $error")

                if (adIndexEc < adData.ec_result.size - 1) {
                    adIndexEc++
                    loadResultAdvertisementEc(context, adData)
                } else {
                    adIndexEc = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagEc, "result---加载result原生广告成功")
                loadTimeEc = Date().time
                isLoadingEc = false
                adIndexEc = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagEc, "result---点击result原生广告")
                recordNumberOfAdClickEc()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示home原生广告
     */
    fun setDisplayResultNativeAd(activity: AppCompatActivity, binding: ActivityResultEcBinding) {
        activity.runOnUiThread {
            appAdDataEc.let {
                if (it != null && !whetherToShowEc && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    val activityDestroyed: Boolean = activity.isDestroyed
                    if (activityDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        it.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_result_native_ec, null) as NativeAdView
                    // 对应原生组件
                    setResultNativeComponent(it, adView)
                    binding.ecAdFrame.removeAllViews()
                    binding.ecAdFrame.addView(adView)
                    binding.resultAdEc = true
                    recordNumberOfAdDisplaysEc()
                    whetherToShowEc = true
                    App.nativeAdRefreshEc = false
                    appAdDataEc = null
                    KLog.d(logTagEc, "result--原生广告--展示")
                    //重新缓存
                    advertisementLoadingEc(activity)
                }
            }

        }
    }

    private fun setResultNativeComponent(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        (adView.headlineView as TextView).text = nativeAd.headline
        nativeAd.mediaContent?.let {
            adView.mediaView?.apply { setImageScaleType(ImageView.ScaleType.CENTER_CROP) }
                ?.setMediaContent(it)
        }
        // These assets aren't guaranteed to be in every UnifiedNativeAd, so it's important to
        // check before trying to display them.
        if (nativeAd.body == null) {
            adView.bodyView?.visibility = View.INVISIBLE
        } else {
            adView.bodyView?.visibility = View.VISIBLE
            (adView.bodyView as TextView).text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView?.visibility = View.INVISIBLE
        } else {
            adView.callToActionView?.visibility = View.VISIBLE
            (adView.callToActionView as TextView).text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView?.visibility = View.GONE
        } else {
            (adView.iconView as ImageView).setImageDrawable(
                nativeAd.icon?.drawable
            )
            adView.iconView?.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)
    }
}