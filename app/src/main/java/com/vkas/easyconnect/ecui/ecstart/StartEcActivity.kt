package com.vkas.easyconnect.ecui.ecstart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.BR
import com.vkas.easyconnect.BuildConfig
import com.vkas.easyconnect.R
import com.vkas.easyconnect.databinding.ActivityStartBinding
import com.vkas.easyconnect.ecad.*
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbase.AdBase
import com.vkas.easyconnect.ecbase.BaseActivity
import com.vkas.easyconnect.ecbase.BaseViewModel
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecui.ecmain.MainActivity
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.findFastestIP
import com.vkas.easyconnect.ecutils.EasyConnectUtils.isThresholdReached
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.MmkvUtils
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.*

class StartEcActivity : BaseActivity<ActivityStartBinding, BaseViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener {
    companion object {
        var isCurrentPage: Boolean = false
    }

    private var liveJumpHomePage = MutableLiveData<Boolean>()
    private var liveJumpHomePage2 = MutableLiveData<Boolean>()
    private var jobOpenAdsEc: Job? = null

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_start
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        isCurrentPage = intent.getBooleanExtra(Constant.RETURN_EC_CURRENT_PAGE, false)

    }

    override fun initToolbar() {
        super.initToolbar()
    }

    override fun initData() {
        super.initData()
        binding.pbStartEc.setProgressViewUpdateListener(this)
        binding.pbStartEc.setProgressDuration(10000)
        binding.pbStartEc.startProgressAnimation()
        liveEventBusEc()
        lifecycleScope.launch(Dispatchers.IO) {
            EasyConnectUtils.getIpInformation()
        }
        getFirebaseDataEc()
        jumpHomePageData()
    }

    private fun liveEventBusEc() {
        LiveEventBus
            .get(Constant.OPEN_CLOSE_JUMP, Boolean::class.java)
            .observeForever {
                KLog.d(logTagEc, "关闭开屏内容-接收==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataEc() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            lifecycleScope.launch {
//                val ips = listOf("192.168.0.1", "8.8.8.8", "114.114.114.114")
//                val fastestIP = findFastestIP(ips)
//                KLog.e("TAG", "Fastest IP: $fastestIP")
//                delay(1500)
//                MmkvUtils.set(
//                    Constant.ADVERTISING_EC_DATA,
//                    ResourceUtils.readStringFromAssert("elAdDataFireBase.json")
//                )
//            }
            return
        } else {
            preloadedAdvertisement()
            val auth = Firebase.remoteConfig
            auth.fetchAndActivate().addOnSuccessListener {
                MmkvUtils.set(Constant.PROFILE_EC_DATA, auth.getString("ec_ser"))
                MmkvUtils.set(Constant.PROFILE_EC_DATA_FAST, auth.getString("ec_smar"))
                MmkvUtils.set(Constant.AROUND_EC_FLOW_DATA, auth.getString("ecAroundFlow_Data"))
                MmkvUtils.set(Constant.ADVERTISING_EC_DATA, auth.getString("ec_ad"))

            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
    }

    private fun jumpHomePageData() {
        liveJumpHomePage2.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                KLog.e("TAG", "isBackDataEc==${App.isBackDataEc}")
                delay(300)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    jumpPage()
                }
            }
        })
        liveJumpHomePage.observe(this, {
            liveJumpHomePage2.postValue(true)
        })
    }

    /**
     * 跳转页面
     */
    private fun jumpPage() {
        // 不是后台切回来的跳转，是后台切回来的直接finish启动页
        if (!isCurrentPage) {
            val intent = Intent(this@StartEcActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
    }

    /**
     * 加载广告
     */
    private fun loadAdvertisement() {
        // 开屏
        AdBase.getOpenInstance().adIndexEc = 0
        AdBase.getOpenInstance().advertisementLoadingEc(this)
        rotationDisplayOpeningAdEc()
        // 首页原生
        AdBase.getHomeInstance().adIndexEc = 0
        AdBase.getHomeInstance().advertisementLoadingEc(this)
        // 结果页原生
        AdBase.getResultInstance().adIndexEc = 0
        AdBase.getResultInstance().advertisementLoadingEc(this)
        // 连接插屏
        AdBase.getConnectInstance().adIndexEc = 0
        AdBase.getConnectInstance().advertisementLoadingEc(this)
        // 服务器页插屏
        AdBase.getBackInstance().adIndexEc = 0
        AdBase.getBackInstance().advertisementLoadingEc(this)
    }

    /**
     * 轮训展示开屏广告
     */
    private fun rotationDisplayOpeningAdEc() {
        jobOpenAdsEc = lifecycleScope.launch {
            try {
                withTimeout(10000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = EcLoadOpenAd
                            .displayOpenAdvertisementEc(this@StartEcActivity)
                        if (showState) {
                            jobOpenAdsEc?.cancel()
                            jobOpenAdsEc = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.e("TimeoutCancellationException I'm sleeping $e")
                jumpPage()
            }
        }
    }

    /**
     * 预加载广告
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDayEc()
        if (isThresholdReached()) {
            KLog.d(logTagEc, "广告达到上线")
            lifecycleScope.launch {
                delay(2000L)
                liveJumpHomePage.postValue(true)
            }
        } else {
            loadAdvertisement()
        }
    }

    override fun onHorizontalProgressStart(view: View?) {
    }

    override fun onHorizontalProgressUpdate(view: View?, progress: Float) {
    }

    override fun onHorizontalProgressFinished(view: View?) {
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return keyCode == KeyEvent.KEYCODE_BACK
    }
}