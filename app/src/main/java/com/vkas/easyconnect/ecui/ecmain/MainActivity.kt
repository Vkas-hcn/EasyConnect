package com.vkas.easyconnect.ecui.ecmain

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.RemoteException
import android.view.KeyEvent
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceDataStore
import com.github.shadowsocks.Core
import com.github.shadowsocks.aidl.IShadowsocksService
import com.github.shadowsocks.aidl.ShadowsocksConnection
import com.github.shadowsocks.bg.BaseService
import com.github.shadowsocks.preference.DataStore
import com.github.shadowsocks.preference.OnPreferenceDataStoreChangeListener
import com.github.shadowsocks.utils.Key
import com.github.shadowsocks.utils.StartService
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.BR
import com.vkas.easyconnect.R
import com.vkas.easyconnect.databinding.ActivityMainBinding
import com.vkas.easyconnect.ecad.EcLoadBackAd
import com.vkas.easyconnect.ecad.EcLoadConnectAd
import com.vkas.easyconnect.ecad.EcLoadHomeAd
import com.vkas.easyconnect.ecad.EcLoadResultAd
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecapp.App.Companion.mmkvEc
import com.vkas.easyconnect.ecbase.BaseActivity
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecui.ecresult.ResultEcActivity
import com.vkas.easyconnect.ecui.ecservice.ServiceListEcActivity
import com.vkas.easyconnect.ecui.ecweb.WebEcActivity
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getFlagThroughCountryEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.isThresholdReached
import com.vkas.easyconnect.ecutils.EcTimerThread
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.MmkvUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.net.JsonUtil
import com.xuexiang.xutil.net.JsonUtil.toJson
import com.xuexiang.xutil.net.NetworkUtils.isNetworkAvailable
import com.xuexiang.xutil.tip.ToastUtils
import kotlinx.coroutines.*

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(),
    ShadowsocksConnection.Callback,
    OnPreferenceDataStoreChangeListener {
    var state = BaseService.State.Idle

    //重复点击
    var repeatClick = false
    private var jobRepeatClick: Job? = null

    // 跳转结果页
    private var liveJumpResultsPage = MutableLiveData<Bundle>()
    private val connection = ShadowsocksConnection(true)

    // 是否返回刷新服务器
    var whetherRefreshServer = false
    private var jobNativeAdsEc: Job? = null
    private var jobStartEc: Job? = null

    //当前执行连接操作
    private var performConnectionOperations: Boolean = false

    //是否点击连接
    private var clickToConnect: Boolean = false

    companion object {
        var stateListener: ((BaseService.State) -> Unit)? = null
    }

    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_main
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
    }

    override fun initToolbar() {
        super.initToolbar()
        binding.presenter = EcClick()
        liveEventBusReceive()
        binding.mainTitle.imgBack.setImageResource(R.mipmap.ic_main_menu)
        binding.mainTitle.imgBack.setOnClickListener {
            binding.sidebarShowsEc =true
        }
    }

    private fun liveEventBusReceive() {
        LiveEventBus
            .get(Constant.TIMER_EC_DATA, String::class.java)
            .observeForever {
                binding.txtTimerEc.text = it
            }
        LiveEventBus
            .get(Constant.STOP_VPN_CONNECTION, Boolean::class.java)
            .observeForever {
                if (state.canStop) {
                    performConnectionOperations = false
                    Core.stopService()
                }
            }

        //更新服务器(未连接)
        LiveEventBus
            .get(Constant.NOT_CONNECTED_EC_RETURN, EcVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, false)
            }
        //更新服务器(已连接)
        LiveEventBus
            .get(Constant.CONNECTED_EC_RETURN, EcVpnBean::class.java)
            .observeForever {
                viewModel.updateSkServer(it, true)
            }
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_EC_ADVERTISEMENT_SHOW, Boolean::class.java)
            .observeForever {
                KLog.e("state", "插屏关闭接收=${it}")

                //重复点击
                jobRepeatClick = lifecycleScope.launch {
                    if (!repeatClick) {
                        KLog.e("state", "插屏关闭后跳转=${it}")
                        EcLoadConnectAd.getInstance().advertisementLoadingEc(this@MainActivity)
                        connectOrDisconnectEc(it)
                        repeatClick = true
                    }
                    delay(1000)
                    repeatClick = false
                }
            }
    }

    override fun initData() {
        super.initData()
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
        changeState(BaseService.State.Idle, animate = false)
        connection.connect(this, this)
        DataStore.publicStore.registerChangeListener(this)
        if (EcTimerThread.isStopThread) {
            viewModel.initializeServerData()
        } else {
            val serviceData = mmkvEc.decodeString("currentServerData", "").toString()
            val currentServerData: EcVpnBean = JsonUtil.fromJson(
                serviceData,
                object : TypeToken<EcVpnBean?>() {}.type
            )
            setFastInformation(currentServerData)
        }
        EcLoadHomeAd.getInstance().whetherToShowEc = false
        initHomeAd()
        showVpnGuide()
    }

    private fun initHomeAd() {
        jobNativeAdsEc = lifecycleScope.launch {
            while (isActive) {
                EcLoadHomeAd.getInstance().setDisplayHomeNativeAdEc(this@MainActivity, binding)
                if (EcLoadHomeAd.getInstance().whetherToShowEc) {
                    jobNativeAdsEc?.cancel()
                    jobNativeAdsEc = null
                }
                delay(1000L)
            }
        }
    }

    override fun initViewObservable() {
        super.initViewObservable()
        // 跳转结果页
        jumpResultsPageData()
        setServiceData()
    }

    private fun jumpResultsPageData() {
        liveJumpResultsPage.observe(this, {
            lifecycleScope.launch(Dispatchers.Main.immediate) {
                delay(300L)
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    startActivityForResult(ResultEcActivity::class.java, 0x11, it)
                }
            }
        })
        viewModel.liveJumpResultsPage.observe(this, {
            liveJumpResultsPage.postValue(it)
        })
    }

    private fun setServiceData() {
        viewModel.liveInitializeServerData.observe(this, {
            setFastInformation(it)
        })
        viewModel.liveUpdateServerData.observe(this, {
            whetherRefreshServer = true
            connect.launch(null)
        })
        viewModel.liveNoUpdateServerData.observe(this, {
            whetherRefreshServer = false
            setFastInformation(it)
            connect.launch(null)
        })
    }

    inner class EcClick {
        fun linkService() {
            if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                connect.launch(null)
            }
//            if (binding.vpnState == 0) {
//                UnLimitedUtils.getBuriedPointEc("unlimF_clickv")
//            }
        }
        fun linkServiceGuide(){
            if (binding.vpnState != 1 && binding.viewGuideMask.isVisible) {
                connect.launch(null)
            }
        }

        fun clickService() {
            if (binding.vpnState != 1 && !binding.viewGuideMask.isVisible) {
                jumpToServerList()
            }
        }

        fun openOrCloseMenu() {
            binding.sidebarShowsEc = binding.sidebarShowsEc != true
        }

        fun clickMain() {
            KLog.e("TAG", "binding.sidebarShowsEc===>${binding.sidebarShowsEc}")
            if (binding.sidebarShowsEc == true) {
                binding.sidebarShowsEc = false
            }
        }

        fun clickMainMenu() {

        }

        fun toContactUs() {
            val uri = Uri.parse("mailto:${Constant.MAILBOX_EC_ADDRESS}")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            runCatching {
                startActivity(intent)
            }.onFailure {
                ToastUtils.toast("Please set up a Mail account")
            }
        }

        fun toPrivacyPolicy() {
            startActivity(WebEcActivity::class.java)
        }

        fun toShare() {
            val intent = Intent()
            intent.action = Intent.ACTION_SEND
            intent.putExtra(
                Intent.EXTRA_TEXT,
                Constant.SHARE_EC_ADDRESS + this@MainActivity.packageName
            )
            intent.type = "text/plain"
            startActivity(intent)
        }
    }

    /**
     * 跳转服务器列表
     */
    fun jumpToServerList() {
        lifecycleScope.launch {
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            val bundle = Bundle()
            if (state.name == "Connected") {
                bundle.putBoolean(Constant.WHETHER_EC_CONNECTED, true)
            } else {
                bundle.putBoolean(Constant.WHETHER_EC_CONNECTED, false)
            }
            EcLoadBackAd.getInstance().advertisementLoadingEc(this@MainActivity)
            val serviceData = mmkvEc.decodeString("currentServerData", "").toString()
            bundle.putString(Constant.CURRENT_EC_SERVICE, serviceData)
            startActivity(ServiceListEcActivity::class.java, bundle)
        }
    }

    /**
     * 设置fast信息
     */
    private fun setFastInformation(elVpnBean: EcVpnBean) {
        if (elVpnBean.ec_best == true) {
            binding.txtCountry.text = Constant.FASTER_EC_SERVER
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(Constant.FASTER_EC_SERVER))
        } else {
            binding.txtCountry.text = elVpnBean.ec_country.toString()
            binding.imgCountry.setImageResource(getFlagThroughCountryEc(elVpnBean.ec_country.toString()))
        }
    }

    private val connect = registerForActivityResult(StartService()) {
        binding.homeGuideEc = false
        binding.viewGuideMask.visibility = View.GONE
        lifecycleScope.launch(Dispatchers.IO) {
            EasyConnectUtils.getIpInformation()
        }
        if (it) {
            ToastUtils.toast(R.string.no_permissions)
        } else {
//            EasyConnectUtils.getBuriedPointEc("unlimF_geta")
            if (isNetworkAvailable()) {
                startVpn()
            } else {
                ToastUtils.toast("Please check your network",3000)
            }
        }
    }

    /**
     * 启动VPN
     */
    private fun startVpn() {
        binding.vpnState = 1
        clickToConnect =true
        changeOfVpnStatus()
        jobStartEc = lifecycleScope.launch {
            App.isAppOpenSameDayEc()
            if (isThresholdReached() || Utils.isNullOrEmpty(EcLoadConnectAd.getInstance().idEc)) {
                KLog.d(logTagEc, "广告达到上线,或者无广告位")
                delay(1500)
                connectOrDisconnectEc(false)
                return@launch
            }
            EcLoadConnectAd.getInstance().advertisementLoadingEc(this@MainActivity)
            EcLoadResultAd.getInstance().advertisementLoadingEc(this@MainActivity)

            try {
                withTimeout(10000L) {
                    delay(1500L)
                    KLog.e(logTagEc, "jobStartEc?.isActive=${jobStartEc?.isActive}")
                    while (jobStartEc?.isActive == true) {
                        val showState =
                            EcLoadConnectAd.getInstance()
                                .displayConnectAdvertisementEc(this@MainActivity)
                        if (showState) {
                            jobStartEc?.cancel()
                            jobStartEc = null
                        }
                        delay(1000L)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                KLog.d(logTagEc, "connect---插屏超时")
                if (jobStartEc != null) {
                    connectOrDisconnectEc(false)
                }
            }
        }
    }


    /**
     * 连接或断开
     * 是否后台关闭（true：后台关闭；false：手动关闭）
     */
    private fun connectOrDisconnectEc(isBackgroundClosed: Boolean) {
        KLog.e("state", "连接或断开")
        if (viewModel.whetherParsingIsIllegalIp()) {
            viewModel.whetherTheBulletBoxCannotBeUsed(this@MainActivity)
            return
        }
        performConnectionOperations = if (state.canStop) {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(false)
            }
            Core.stopService()
            false
        } else {
            if (!isBackgroundClosed) {
                viewModel.jumpConnectionResultsPage(true)
            }
//            EasyConnectUtils.getBuriedPointEc("unlimF_sv")
            Core.startService()
            true
        }
    }

    private fun changeState(
        state: BaseService.State,
        animate: Boolean = true
    ) {
        this.state = state
        connectionStatusJudgment(state.name)
        stateListener?.invoke(state)
    }

    /**
     * 连接状态判断
     */
    private fun connectionStatusJudgment(state: String) {
        KLog.e("TAG", "connectionStatusJudgment=${state}")
        if (performConnectionOperations && state != "Connected") {
            //vpn连接失败
            KLog.d(logTagEc, "vpn连接失败")
//            EasyConnectUtils.getBuriedPointEc("unlimF_vF")
            ToastUtils.toast(getString(R.string.connected_failed),3000)
        }
        when (state) {
            "Connected" -> {
                // 连接成功
                connectionServerSuccessful()
            }
            "Stopped" -> {
                disconnectServerSuccessful()
            }
        }
    }

    /**
     * 连接服务器成功
     */
    private fun connectionServerSuccessful() {
        binding.vpnState = 2
        changeOfVpnStatus()
//        EasyConnectUtils.getBuriedPointEc("unlimF_vT")
    }

    /**
     * 断开服务器
     */
    private fun disconnectServerSuccessful() {
        KLog.e("TAG", "断开服务器")
        binding.vpnState = 0
        changeOfVpnStatus()
//        if (clickToConnect) {
//            EasyConnectUtils.getBuriedPointConnectionTimeEc(
//                "unlimF_cn",
//                mmkvEc.decodeInt(Constant.LAST_TIME_SECOND)
//            )
//        }
    }

    /**
     * vpn状态变化
     * 是否连接
     */
    private fun changeOfVpnStatus() {
        when (binding.vpnState) {
            0 -> {
                binding.imgConnectionStatus.text = getString(R.string.connect)
                binding.imgConnectionStatus.setBackgroundResource(R.drawable.bg_connect)
                binding.txtTimerEc.text = getString(R.string._00_00_00)
                binding.txtTimerEc.setTextColor(getColor(R.color.tv_time_main_dis))
                EcTimerThread.endTiming()
                binding.lavViewEc.pauseAnimation()
                binding.lavViewEc.visibility = View.GONE
            }
            1 -> {
                if(!performConnectionOperations){
                    binding.imgConnectionStatus.text = getString(R.string.connecting)
                }else{
                    binding.imgConnectionStatus.text = getString(R.string.disconnecting)
                }
                binding.imgState.visibility = View.GONE
                binding.lavViewEc.visibility = View.VISIBLE
                binding.lavViewEc.playAnimation()
            }
            2 -> {
                binding.imgConnectionStatus.text = getString(R.string.connected)
                binding.txtTimerEc.setTextColor(getColor(R.color.tv_time_connect))
                binding.imgConnectionStatus.setBackgroundResource(R.drawable.bg_disconnect)
                EcTimerThread.startTiming()
                binding.lavViewEc.pauseAnimation()
                binding.lavViewEc.visibility = View.GONE
            }
        }
    }
    private fun showVpnGuide() {
        lifecycleScope.launch {
            delay(300)
            if (state.name != "Connected") {
                binding.homeGuideEc = true
                binding.viewGuideMask.visibility = View.VISIBLE
                binding.lavViewGu.playAnimation()
            } else {
                binding.homeGuideEc = false
                binding.viewGuideMask.visibility = View.GONE
                binding.lavViewGu.pauseAnimation()
            }
        }
    }
    override fun stateChanged(state: BaseService.State, profileName: String?, msg: String?) {
        changeState(state)
    }

    override fun onServiceConnected(service: IShadowsocksService) {
        changeState(
            try {
                BaseService.State.values()[service.state]
            } catch (_: RemoteException) {
                BaseService.State.Idle
            }
        )
    }

    override fun onPreferenceDataStoreChanged(store: PreferenceDataStore, key: String) {
        when (key) {
            Key.serviceMode -> {
                connection.disconnect(this)
                connection.connect(this, this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connection.bandwidthTimeout = 500
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            if (lifecycle.currentState != Lifecycle.State.RESUMED) {
                return@launch
            }
            if (App.nativeAdRefreshEc) {
                EcLoadHomeAd.getInstance().whetherToShowEc = false
                if (EcLoadHomeAd.getInstance().appAdDataEc != null) {
                    KLog.d(logTagEc, "onResume------>1")
                    EcLoadHomeAd.getInstance().setDisplayHomeNativeAdEc(this@MainActivity, binding)
                } else {
                    binding.vpnAdEc = false
                    KLog.d(logTagEc, "onResume------>2")
                    EcLoadHomeAd.getInstance().advertisementLoadingEc(this@MainActivity)
                    initHomeAd()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        connection.bandwidthTimeout = 0
    }

    override fun onDestroy() {
        super.onDestroy()
        LiveEventBus
            .get(Constant.PLUG_EC_ADVERTISEMENT_SHOW, Boolean::class.java)
            .removeObserver {}
        DataStore.publicStore.unregisterChangeListener(this)
        connection.disconnect(this)
        jobStartEc?.cancel()
        jobStartEc = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0x11 && whetherRefreshServer) {
            setFastInformation(viewModel.afterDisconnectionServerData)
            val serviceData = toJson(viewModel.afterDisconnectionServerData)
            MmkvUtils.set("currentServerData", serviceData)
            viewModel.currentServerData = viewModel.afterDisconnectionServerData
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (binding.viewGuideMask.isVisible) {
                binding.homeGuideEc = false
                binding.viewGuideMask.visibility = View.GONE
                binding.lavViewGu.pauseAnimation()
            } else {
                finish()
            }
        }
        return true
    }
}