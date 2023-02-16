package com.vkas.easyconnect.ecad

import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.vkas.easyconnect.databinding.ActivityMainBinding
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbean.EcAdBean
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getAdServerDataEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdClickEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.takeSortedAdIDEc
import com.vkas.easyconnect.ecutils.KLog
import java.util.*
import com.vkas.easyconnect.R
import com.vkas.easyconnect.ecbase.AdBase
import com.vkas.easyconnect.ecutils.EasyConnectUtils.recordNumberOfAdDisplaysEc
import com.vkas.easyconnect.ecutils.RoundCornerOutlineProvider

object EcLoadHomeAd {
    private val adBase = AdBase.getHomeInstance()

    /**
     * 加载vpn原生广告
     */
     fun loadHomeAdvertisementEc(context: Context,adData: EcAdBean) {
        val id = takeSortedAdIDEc(adBase.adIndexEc, adData.ec_vpn)
        KLog.d(logTagEc, "home---原生广告id=$id;权重=${adData.ec_vpn.getOrNull(adBase.adIndexEc)?.ec_weight}")

        val vpnNativeAds = AdLoader.Builder(
            context.applicationContext,
            id
        )
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        vpnNativeAds.withNativeAdOptions(adOptions)
        vpnNativeAds.forNativeAd {
            adBase.appAdDataEc = it
        }
        vpnNativeAds.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                val error =
                    """
           domain: ${loadAdError.domain}, code: ${loadAdError.code}, message: ${loadAdError.message}
          """"
                adBase.isLoadingEc = false
                adBase.appAdDataEc = null
                KLog.d(logTagEc, "home---加载vpn原生加载失败: $error")

                if (adBase.adIndexEc < adData.ec_vpn.size - 1) {
                    adBase.adIndexEc++
                    loadHomeAdvertisementEc(context,adData)
                }else{
                    adBase.adIndexEc = 0
                }
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
                KLog.d(logTagEc, "home---加载vpn原生广告成功")
                adBase.loadTimeEc = Date().time
                adBase.isLoadingEc = false
                adBase.adIndexEc = 0
            }

            override fun onAdOpened() {
                super.onAdOpened()
                KLog.d(logTagEc, "home---点击vpn原生广告")
                recordNumberOfAdClickEc()
            }
        }).build().loadAd(AdRequest.Builder().build())
    }

    /**
     * 设置展示vpn原生广告
     */
    fun setDisplayHomeNativeAdEc(activity: AppCompatActivity, binding: ActivityMainBinding) {
        activity.runOnUiThread {
            adBase.appAdDataEc?.let { adData ->
                if (adData is NativeAd && !adBase.whetherToShowEc && activity.lifecycle.currentState == Lifecycle.State.RESUMED) {
                    if (activity.isDestroyed || activity.isFinishing || activity.isChangingConfigurations) {
                        adData.destroy()
                        return@let
                    }
                    val adView = activity.layoutInflater.inflate(R.layout.layout_main_native_ec, null) as NativeAdView
                    // 对应原生组件
                    setCorrespondingNativeComponentEc(adData, adView)
                    binding.ecAdFrame.apply {
                        removeAllViews()
                        addView(adView)
                    }
                    binding.vpnAdEc = true
                    recordNumberOfAdDisplaysEc()
                    adBase.whetherToShowEc = true
                    App.nativeAdRefreshEc = false
                    adBase.appAdDataEc = null
                    KLog.d(logTagEc, "home--原生广告--展示")
                    //重新缓存
                    AdBase.getHomeInstance().advertisementLoadingEc(activity)
                }
            }
        }
    }

    private fun setCorrespondingNativeComponentEc(nativeAd: NativeAd, adView: NativeAdView) {
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