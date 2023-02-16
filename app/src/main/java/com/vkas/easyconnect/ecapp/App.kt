package com.vkas.easyconnect.ecapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.blankj.utilcode.util.ProcessUtils
import com.github.shadowsocks.Core
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tencent.mmkv.MMKV
import com.vkas.easyconnect.BuildConfig
import com.vkas.easyconnect.ecui.ecmain.MainActivity
import com.vkas.easyconnect.ecbase.AppManagerEcMVVM
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecui.ecstart.StartEcActivity
import com.vkas.easyconnect.ecutils.ActivityUtils
import com.vkas.easyconnect.ecutils.CalendarUtils
import com.vkas.easyconnect.ecutils.EcTimerThread.sendTimerInformation
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.MmkvUtils
import com.xuexiang.xui.XUI
import com.xuexiang.xutil.XUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class App : Application(), LifecycleObserver {
    private var flag = 0
    private var job_ec : Job? =null
    private var ad_activity_ec: Activity? = null
    private var top_activity_ec: Activity? = null
    companion object {
        // app当前是否在后台
        var isBackDataEc = false

        // 是否进入后台（三秒后）
        var whetherBackgroundEc = false
        // 原生广告刷新
        var nativeAdRefreshEc = false
        val mmkvEc by lazy {
            //启用mmkv的多进程功能
            MMKV.mmkvWithID("EasyConnect", MMKV.MULTI_PROCESS_MODE)
        }
        //当日日期
        var adDateEc = ""
        /**
         * 判断是否是当天打开
         */
        fun isAppOpenSameDayEc() {
            adDateEc = mmkvEc.decodeString(Constant.CURRENT_EC_DATE, "").toString()
            if (adDateEc == "") {
                MmkvUtils.set(Constant.CURRENT_EC_DATE, CalendarUtils.formatDateNow())
            } else {
                if (CalendarUtils.dateAfterDate(adDateEc, CalendarUtils.formatDateNow())) {
                    MmkvUtils.set(Constant.CURRENT_EC_DATE, CalendarUtils.formatDateNow())
                    MmkvUtils.set(Constant.CLICKS_EC_COUNT, 0)
                    MmkvUtils.set(Constant.SHOW_EC_COUNT, 0)
                }
            }
        }

    }
    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        initCrash()
        setActivityLifecycleEc(this)
        MobileAds.initialize(this) {}
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (ProcessUtils.isMainProcess()) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
            Firebase.initialize(this)
            FirebaseApp.initializeApp(this)
            XUI.init(this) //初始化UI框架
            XUtil.init(this)
            LiveEventBus
                .config()
                .lifecycleObserverAlwaysActive(true)
            //是否开启打印日志
            KLog.init(BuildConfig.DEBUG)
        }
        Core.init(this, MainActivity::class)
        sendTimerInformation()
        isAppOpenSameDayEc()
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        nativeAdRefreshEc =true
        job_ec?.cancel()
        job_ec = null
        //从后台切过来，跳转启动页
        if (whetherBackgroundEc && !isBackDataEc) {
            jumpGuidePage()
        }
    }
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStopState(){
        job_ec = GlobalScope.launch {
            whetherBackgroundEc = false
            delay(3000L)
            whetherBackgroundEc = true
            ad_activity_ec?.finish()
            ActivityUtils.getActivity(StartEcActivity::class.java)?.finish()
        }
    }
    /**
     * 跳转引导页
     */
    private fun jumpGuidePage(){
        whetherBackgroundEc = false
        val intent = Intent(top_activity_ec, StartEcActivity::class.java)
        intent.putExtra(Constant.RETURN_EC_CURRENT_PAGE, true)
        top_activity_ec?.startActivity(intent)
    }
    fun setActivityLifecycleEc(application: Application) {
        //注册监听每个activity的生命周期,便于堆栈式管理
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                AppManagerEcMVVM.get().addActivity(activity)
                if (activity !is AdActivity) {
                    top_activity_ec = activity
                } else {
                    ad_activity_ec = activity
                }
                KLog.v("Lifecycle", "onActivityCreated" + activity.javaClass.name)
            }

            override fun onActivityStarted(activity: Activity) {
                KLog.v("Lifecycle", "onActivityStarted" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_ec = activity
                } else {
                    ad_activity_ec = activity
                }
                flag++
                isBackDataEc = false
            }

            override fun onActivityResumed(activity: Activity) {
                KLog.v("Lifecycle", "onActivityResumed=" + activity.javaClass.name)
                if (activity !is AdActivity) {
                    top_activity_ec = activity
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activity is AdActivity) {
                    ad_activity_ec = activity
                } else {
                    top_activity_ec = activity
                }
                KLog.v("Lifecycle", "onActivityPaused=" + activity.javaClass.name)
            }

            override fun onActivityStopped(activity: Activity) {
                flag--
                if (flag == 0) {
                    isBackDataEc = true
                }
                KLog.v("Lifecycle", "onActivityStopped=" + activity.javaClass.name)
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
                KLog.v("Lifecycle", "onActivitySaveInstanceState=" + activity.javaClass.name)

            }

            override fun onActivityDestroyed(activity: Activity) {
                AppManagerEcMVVM.get().removeActivity(activity)
                KLog.v("Lifecycle", "onActivityDestroyed" + activity.javaClass.name)
                ad_activity_ec = null
                top_activity_ec = null
            }
        })
    }
}