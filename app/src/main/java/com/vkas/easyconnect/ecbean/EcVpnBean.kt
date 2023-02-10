package com.vkas.easyconnect.ecbean

data class EcVpnBean(
    var ec_city: String? = null,
    var ec_country: String? = null,
    var ec_ip: String? = null,
    var ec_method: String? = null,
    var ec_port: Int? = null,
    var ec_pwd: String? = null,
    var ec_check: Boolean? = false,
    var ec_best: Boolean? = false
)
