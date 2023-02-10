package com.vkas.easyconnect.ecbean

import androidx.annotation.Keep

@Keep
data class EcAdBean(
    var ec_open: MutableList<EcDetailBean> = ArrayList(),
    var ec_back: MutableList<EcDetailBean> = ArrayList(),
    var ec_vpn: MutableList<EcDetailBean> = ArrayList(),
    var ec_result: MutableList<EcDetailBean> = ArrayList(),
    var ec_connect: MutableList<EcDetailBean> = ArrayList(),

    var ec_click_num: Int = 0,
    var ec_show_num: Int = 0
)

@Keep
data class EcDetailBean(
    val ec_id: String,
    val ec_platform: String,
    val ec_type: String,
    val ec_weight: Int
)