package com.vkas.easyconnect.ecui.ecservice

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.easyconnect.R
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecutils.EasyConnectUtils.getFlagThroughCountryEc

class ServiceListEcAdapter (data: MutableList<EcVpnBean>?) :
    BaseQuickAdapter<EcVpnBean, BaseViewHolder>(
        R.layout.item_service,
        data
    ) {

    override fun convert(holder: BaseViewHolder, item: EcVpnBean) {
        if (item?.ec_best == true) {
            holder.setText(R.id.txt_country, Constant.FASTER_EC_SERVER)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryEc(Constant.FASTER_EC_SERVER)
            )
        } else {
            holder.setText(R.id.txt_country, item?.ec_country + "-" + item?.ec_city)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryEc(item?.ec_country.toString())
            )
        }

        if (item?.ec_check == true) {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_service_item_chek)
            holder.setTextColor(R.id.txt_country, context.resources.getColor(R.color.white))
        } else {
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_service_result)
            holder.setTextColor(R.id.txt_country, context.resources.getColor(R.color.tv_ff_333333))
        }
    }
}