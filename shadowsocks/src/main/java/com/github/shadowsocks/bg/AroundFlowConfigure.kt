package com.github.shadowsocks.bg

import android.content.Context
import com.tencent.mmkv.MMKV
import android.net.VpnService
import com.github.shadowsocks.bean.AroundFlowBean
import com.github.shadowsocks.utils.JsonUtils

import com.google.gson.reflect.TypeToken

object AroundFlowConfigure {
    private val mmkv by lazy {
        //启用mmkv的多进程功能
        MMKV.mmkvWithID("EasyLink", MMKV.MULTI_PROCESS_MODE)
    }

    /**
     * @return 解析aroundFlow json文件
     */
    private fun getAroundFlowJsonData(): AroundFlowBean? {
        if(mmkv.decodeString("aroundFlowData").isNullOrEmpty()){
            return JsonUtils.fromJson(
                "{\n" +
                        "  \"around_flow_mode\": \"0\",\n" +
                        "  \"black_list\": [\n" +
                        "    \"com.UCMobile\"\n" +
                        "  ],\n" +
                        "  \"white_list\": [\n" +
                        "    \"com.android.chrome\",\n" +
                        "    \"com.microsoft.emmx\",\n" +
                        "    \"org.mozilla.firefox\",\n" +
                        "    \"com.opera.browser\",\n" +
                        "    \"com.google.android.googlequicksearchbox\",\n" +
                        "    \"mark.via.gp\",\n" +
                        "    \"com.UCMobile.intl\",\n" +
                        "    \"com.brave.browser\",\n" +
                        "    \"privacy.explorer.fast.safe.browser\"\n" +
                        "  ]\n" +
                        "}",
                object : TypeToken<AroundFlowBean?>() {}.type
            )
        }else{
            return JsonUtils.fromJson(
                mmkv.decodeString("aroundFlowData"),
                object : TypeToken<AroundFlowBean?>() {}.type
            )
        }

    }
    fun brand(context: Context, builder: VpnService.Builder, myPackageName: String) {
        val strategy = getAroundFlowJsonData()
        when (strategy?.around_flow_mode) {
            //黑名单绕流
            "1" -> {
                (listOf(myPackageName) + listGmsPackages() + (strategy.black_list))
                    .iterator()
                    .forEachRemaining {
                        runCatching { builder.addDisallowedApplication(it) }
                    }
            }
            //白名单扰流
            "2" -> {
                (listWhitePackages()+strategy.white_list )
                    .iterator()
                    .forEachRemaining {
                        runCatching { builder.addAllowedApplication(it) }
                    }
            }
            else -> {
                builder.addDisallowedApplication(myPackageName)
            }
        }
    }

    /**
     * 默认黑名单
     */
    private fun listGmsPackages(): List<String> {
        return listOf(
            "com.google.android.gms",
            "com.google.android.ext.services",
            "com.google.process.gservices",
            "com.android.vending",
            "com.google.android.gms.persistent",
            "com.google.android.cellbroadcastservice",
            "com.google.android.packageinstaller"
        )
    }

    /**
     * 默认白名单
     */
    private fun listWhitePackages(): List<String> {
        return listOf(
            "com.android.chrome",
            "com.microsoft.emmx",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.google.android.googlequicksearchbox",
            "mark.via.gp",
            "com.UCMobile.intl",
            "com.brave.browser",
            "privacy.explorer.fast.safe.browser",
        )
    }
}