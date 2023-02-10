package com.vkas.easyconnect.ecui.ecservice

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.reflect.TypeToken
import com.vkas.easyconnect.ecapp.App.Companion.mmkvEc
import com.vkas.easyconnect.ecbase.BaseViewModel
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getFastIpEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getLocalServerData
import com.vkas.easyconnect.ecutils.KLog
import com.xuexiang.xui.utils.Utils.isNullOrEmpty
import com.xuexiang.xutil.net.JsonUtil

class ServiceListEcViewModel (application: Application) : BaseViewModel(application) {
    private lateinit var skServiceBean : EcVpnBean
    private lateinit var skServiceBeanList :MutableList<EcVpnBean>

    // 服务器列表数据
    val liveServerListData: MutableLiveData<MutableList<EcVpnBean>> by lazy {
        MutableLiveData<MutableList<EcVpnBean>>()
    }

    /**
     * 获取服务器列表
     */
    fun getServerListData(){
        skServiceBeanList = ArrayList()
        skServiceBean = EcVpnBean()
        skServiceBeanList = if (isNullOrEmpty(mmkvEc.decodeString(Constant.PROFILE_EC_DATA))) {
            KLog.e("TAG","skServiceBeanList--1--->")
            getLocalServerData()
        } else {
            KLog.e("TAG","skServiceBeanList--2--->")

            JsonUtil.fromJson(
                mmkvEc.decodeString(Constant.PROFILE_EC_DATA),
                object : TypeToken<MutableList<EcVpnBean>?>() {}.type
            )
        }
        skServiceBeanList.add(0, getFastIpEc())
        KLog.e("LOG","skServiceBeanList---->${JsonUtil.toJson(skServiceBeanList)}")

        liveServerListData.postValue(skServiceBeanList)
    }
}