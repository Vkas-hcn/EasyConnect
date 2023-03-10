package com.vkas.easyconnect.ecui.ecstart

import android.content.Intent
import android.os.Bundle
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
import com.vkas.easyconnect.ecbase.BaseActivity
import com.vkas.easyconnect.ecbase.BaseViewModel
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecui.ecmain.MainActivity
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.isThresholdReached
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.MmkvUtils
import com.xuexiang.xui.widget.progress.HorizontalProgressView
import kotlinx.coroutines.*

class StartEcActivity : BaseActivity<ActivityStartBinding, BaseViewModel>(),
    HorizontalProgressView.HorizontalProgressUpdateListener  {
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
        binding.pbStartEc.setProgressDuration(2000)
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
                KLog.d(logTagEc, "??????????????????-??????==${this.lifecycle.currentState}")
                if (this.lifecycle.currentState == Lifecycle.State.STARTED) {
                    jumpPage()
                }
            }
    }

    private fun getFirebaseDataEc() {
        if (BuildConfig.DEBUG) {
            preloadedAdvertisement()
//            lifecycleScope.launch {
//                delay(1500)
//                MmkvUtils.set(Constant.ADVERTISING_EC_DATA, ResourceUtils.readStringFromAssert("elAdDataFireBase.json"))
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
     * ????????????
     */
    private fun jumpPage() {
        // ????????????????????????????????????????????????????????????finish?????????
        if (!isCurrentPage) {
            val intent = Intent(this@StartEcActivity, MainActivity::class.java)
            startActivity(intent)
        }
        finish()

    }
    /**
     * ????????????
     */
    private fun loadAdvertisement() {
        // ??????
        EcLoadOpenAd.getInstance().adIndexEc = 0
        EcLoadOpenAd.getInstance().advertisementLoadingEc(this)
        rotationDisplayOpeningAdEc()
        // ????????????
        EcLoadHomeAd.getInstance().adIndexEc = 0
        EcLoadHomeAd.getInstance().advertisementLoadingEc(this)
        // ???????????????
        EcLoadResultAd.getInstance().adIndexEc = 0
        EcLoadResultAd.getInstance().advertisementLoadingEc(this)
        // ????????????
        EcLoadConnectAd.getInstance().adIndexEc = 0
        EcLoadConnectAd.getInstance().advertisementLoadingEc(this)
        // ??????????????????
        EcLoadBackAd.getInstance().adIndexEc = 0
        EcLoadBackAd.getInstance().advertisementLoadingEc(this)
    }
    /**
     * ????????????????????????
     */
    private fun rotationDisplayOpeningAdEc() {
        jobOpenAdsEc = lifecycleScope.launch {
            try {
                withTimeout(8000L) {
                    delay(1000L)
                    while (isActive) {
                        val showState = EcLoadOpenAd.getInstance()
                            .displayOpenAdvertisementEc(this@StartEcActivity)
                        if (showState) {
                            jobOpenAdsEc?.cancel()
                            jobOpenAdsEc =null
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
     * ???????????????
     */
    private fun preloadedAdvertisement() {
        App.isAppOpenSameDayEc()
        if (isThresholdReached()) {
            KLog.d(logTagEc, "??????????????????")
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