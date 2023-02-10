package com.vkas.easyconnect.ecui.ecmain

import android.app.AlertDialog
import android.app.Application
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.github.shadowsocks.database.Profile
import com.github.shadowsocks.database.ProfileManager
import com.github.shadowsocks.preference.DataStore
import com.google.gson.reflect.TypeToken
import com.vkas.easyconnect.R
import com.vkas.easyconnect.ecapp.App.Companion.mmkvEc
import com.vkas.easyconnect.ecbase.BaseViewModel
import com.vkas.easyconnect.ecbean.EcIpBean
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.KLog
import com.vkas.easyconnect.ecutils.MmkvUtils
import com.xuexiang.xui.utils.Utils
import com.xuexiang.xutil.XUtil
import com.xuexiang.xutil.net.JsonUtil

class MainViewModel (application: Application) : BaseViewModel(application){
    //初始化服务器数据
    val liveInitializeServerData: MutableLiveData<EcVpnBean> by lazy {
        MutableLiveData<EcVpnBean>()
    }
    //更新服务器数据(未连接)
    val liveNoUpdateServerData: MutableLiveData<EcVpnBean> by lazy {
        MutableLiveData<EcVpnBean>()
    }
    //更新服务器数据(已连接)
    val liveUpdateServerData: MutableLiveData<EcVpnBean> by lazy {
        MutableLiveData<EcVpnBean>()
    }

    //当前服务器
    var currentServerData: EcVpnBean = EcVpnBean()
    //断开后选中服务器
    var afterDisconnectionServerData: EcVpnBean = EcVpnBean()
    //跳转结果页
    val liveJumpResultsPage: MutableLiveData<Bundle> by lazy {
        MutableLiveData<Bundle>()
    }
    fun initializeServerData() {
        val bestData = EasyConnectUtils.getFastIpEc()
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                ProfileManager.updateProfile(setSkServerData(it, bestData))
            } else {
                val profile = Profile()
                ProfileManager.createProfile(setSkServerData(profile, bestData))
            }
        }
        DataStore.profileId = 1L
        currentServerData = bestData
        val serviceData = JsonUtil.toJson(currentServerData)
        MmkvUtils.set("currentServerData",serviceData)
        liveInitializeServerData.postValue(bestData)
    }

    fun updateSkServer(skServiceBean: EcVpnBean,isConnect:Boolean) {
        ProfileManager.getProfile(DataStore.profileId).let {
            if (it != null) {
                setSkServerData(it, skServiceBean)
                ProfileManager.updateProfile(it)
            } else {
                ProfileManager.createProfile(Profile())
            }
        }
        DataStore.profileId = 1L
        if(isConnect){
            afterDisconnectionServerData = skServiceBean
            liveUpdateServerData.postValue(skServiceBean)
        }else{
            currentServerData = skServiceBean
            val serviceData = JsonUtil.toJson(currentServerData)
            MmkvUtils.set("currentServerData",serviceData)
            liveNoUpdateServerData.postValue(skServiceBean)
        }
    }

    /**
     * 设置服务器数据
     */
    private fun setSkServerData(profile: Profile, bestData: EcVpnBean): Profile {
        profile.name = bestData.ec_country + "-" + bestData.ec_city
        profile.host = bestData.ec_ip.toString()
        profile.password = bestData.ec_pwd!!
        profile.method = bestData.ec_method!!
        profile.remotePort = bestData.ec_port!!
        return profile
    }
    /**
     * 跳转连接结果页
     */
    fun jumpConnectionResultsPage(isConnection: Boolean){
        val bundle = Bundle()
        val serviceData = mmkvEc.decodeString("currentServerData", "").toString()
        bundle.putBoolean(Constant.CONNECTION_EC_STATUS, isConnection)
        bundle.putString(Constant.SERVER_EC_INFORMATION, serviceData)
        liveJumpResultsPage.postValue(bundle)
    }

    /**
     * 解析是否是非法ip；中国大陆ip、伊朗ip
     */
    fun whetherParsingIsIllegalIp(): Boolean {
        val data = mmkvEc.decodeString(Constant.IP_INFORMATION)
        KLog.e("state","data=${data}===isNullOrEmpty=${Utils.isNullOrEmpty(data)}")
        return if (Utils.isNullOrEmpty(data)) {
            false
        } else {
            val ptIpBean: EcIpBean = JsonUtil.fromJson(
                mmkvEc.decodeString(Constant.IP_INFORMATION),
                object : TypeToken<EcIpBean?>() {}.type
            )
            return ptIpBean.country_code == "IR"
        }
    }

    /**
     * 是否显示不能使用弹框
     */
    fun whetherTheBulletBoxCannotBeUsed(context: AppCompatActivity) {
        val dialogVpn: AlertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.vpn))
            .setMessage(context.getString(R.string.cant_user_vpn))
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                XUtil.exitApp()
            }.create()
        dialogVpn.setCancelable(false)
        dialogVpn.show()
        dialogVpn.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialogVpn.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }
}