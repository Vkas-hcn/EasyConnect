package com.vkas.easyconnect.ecui.ecresult

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.BR
import com.vkas.easyconnect.R
import com.vkas.easyconnect.databinding.ActivityResultEcBinding
import com.vkas.easyconnect.ecad.EcLoadResultAd
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecapp.App.Companion.mmkvEc
import com.vkas.easyconnect.ecbase.AdBase
import com.vkas.easyconnect.ecbase.BaseActivity
import com.vkas.easyconnect.ecbase.BaseViewModel
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ResultEcActivity : BaseActivity<ActivityResultEcBinding, BaseViewModel>()  {
    private var isConnectionEc: Boolean = false

    //当前服务器
    private lateinit var currentServerBeanEc: EcVpnBean
    private var jobResultEc: Job? = null
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_result_ec
    }

    override fun initVariableId(): Int {
        return BR._all
    }
    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        isConnectionEc = bundle?.getBoolean(Constant.CONNECTION_EC_STATUS) == true
        currentServerBeanEc = JsonUtil.fromJson(
            bundle?.getString(Constant.SERVER_EC_INFORMATION),
            object : TypeToken<EcVpnBean?>() {}.type
        )
    }

    override fun initToolbar() {
        super.initToolbar()
        displayTimer()
        liveEventBusReceive()
        binding.presenter = EcClick()

        binding.resultTitle.imgBack.setOnClickListener {
            finish()
        }
    }
    fun liveEventBusReceive(){
//        LiveEventBus
//            .get(Constant.STOP_VPN_CONNECTION, Boolean::class.java)
//            .observeForever {
//                isConnectionEc = true
//                binding.resultTitle.tvTitle.text = getString(R.string.vpn_disconnect)
//                binding.tvConnected.text = getString(R.string.disconnection_succeed)
//                binding.linConnect.setBackgroundResource(R.mipmap.bg_result)
//            }
    }
    override fun initData() {
        super.initData()
        if (isConnectionEc) {
            binding.tvConnected.text = getString(R.string.connecteds)
            binding.viewTop.setBackgroundResource(R.drawable.bg_result_connect)
            binding.imgConnectState.setImageResource(R.mipmap.ic_result_connect)
            binding.txtTimerEc.setTextColor(getColor(R.color.tv_result_time_connect))

        } else {
            binding.tvConnected.text = getString(R.string.disconnecteds)
            binding.viewTop.setBackgroundResource(R.drawable.bg_result_disconnect)
            binding.imgConnectState.setImageResource(R.mipmap.ic_result_dis)
            binding.txtTimerEc.setTextColor(getColor(R.color.tv_time_dis))

            binding.txtTimerEc.text = mmkvEc.decodeString(Constant.LAST_TIME, "").toString()
        }
        binding.imgCountry.setImageResource(EasyConnectUtils.getFlagThroughCountryEc(currentServerBeanEc.ec_country.toString()))
        binding.txtCountry.text = currentServerBeanEc.ec_country.toString()
        AdBase.getResultInstance().whetherToShowEc =false
        initResultAds()
    }

    inner class EcClick {

    }

    private fun initResultAds() {
        jobResultEc= lifecycleScope.launch {
            while (isActive) {
                EcLoadResultAd.setDisplayResultNativeAd(this@ResultEcActivity,binding)
                if (AdBase.getResultInstance().whetherToShowEc) {
                    jobResultEc?.cancel()
                    jobResultEc = null
                }
                delay(1000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if(lifecycle.currentState != Lifecycle.State.RESUMED){return@launch}
            if(App.nativeAdRefreshEc){
                AdBase.getResultInstance().whetherToShowEc =false
                if(AdBase.getResultInstance().appAdDataEc !=null){
                    EcLoadResultAd.setDisplayResultNativeAd(this@ResultEcActivity,binding)
                }else{
                    AdBase.getResultInstance().advertisementLoadingEc(this@ResultEcActivity)
                    initResultAds()
                }
            }
        }
    }
    /**
     * 显示计时器
     */
    private fun displayTimer() {
        LiveEventBus
            .get(Constant.TIMER_EC_DATA, String::class.java)
            .observeForever {
                if (isConnectionEc) {
                    binding.txtTimerEc.text = it
                } else {
                    binding.txtTimerEc.text = mmkvEc.decodeString(Constant.LAST_TIME, "").toString()
                }
            }
    }
}