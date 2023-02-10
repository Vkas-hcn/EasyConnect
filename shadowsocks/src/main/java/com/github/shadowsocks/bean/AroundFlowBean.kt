package com.github.shadowsocks.bean

import androidx.annotation.Keep
import java.io.Serializable

@Keep
data class AroundFlowBean(
    val around_flow_mode: String? = null,
    val black_list: List<String> = ArrayList(),
    val white_list: List<String> = ArrayList()
) : Serializable