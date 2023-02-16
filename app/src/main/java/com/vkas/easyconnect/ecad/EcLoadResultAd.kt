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
import com.vkas.easyconnect.ecbase.AdBase
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getAdServerDataEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdClickEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdDisplaysEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.takeSortedAdIDEc
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.RoundCornerOutlineProvider
import java.util.*

object EcLoadResultAd {
    private val adBase = AdBase.getResultInstance()

    /**
     * 加载result原生广告
     */
    fun loadResultAdvertisementEc(context: Context, adData: EcAdBean) {
        val id = takeSortedAdIDEc(adBase.adIndexEc, adData.ec_result)
        KLog.d(
            logTagEc,
            "result---原生广告id=$id;权重=${adData.ec_result.getOrNull(adBase.adIndexEc)?.ec_weight}"
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
            adBase.appAdDataEc = it
        }
        homeNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                adBase.isLoadingEc = false
                adBase.appAdDataEc = null
                KLog.d(logTagEc, "result---加载result原生加载失败: $error")

                if (adBase.adIndexEc < adData.ec_result.size - 1) {
                    adBase.adIndexEc++
                    loadResultAdvertisementEc(context, adData)
                } else {
                    adBase.adIndexEc = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagEc, "result---加载result原生广告成功")
                adBase.loadTimeEc = Date().time
                adBase.isLoadingEc = false
                adBase.adIndexEc = 0
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
            adBase.appAdDataEc?.let { adData ->
                if (adData is NativeAd &&!adBase.whetherToShowEc && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        adData.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater
                        .inflate(R.layout.layout_result_native_ec, null) as NativeAdView
                    // 对应原生组件
                    setResultNativeComponent(adData, adView)
                    binding.ecAdFrame.apply {
                        removeAllViews()
                        addView(adView)
                    }
                    binding.resultAdEc = true
                    recordNumberOfAdDisplaysEc()
                    adBase.whetherToShowEc = true
                    App.nativeAdRefreshEc = false
                    adBase.appAdDataEc = null
                    KLog.d(logTagEc, "result--原生广告--展示")
                    //重新缓存
                    AdBase.getResultInstance().advertisementLoadingEc(activity)
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
        adView.mediaView?.clipToOutline=true
        adView.mediaView?.outlineProvider= RoundCornerOutlineProvider(8f)
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