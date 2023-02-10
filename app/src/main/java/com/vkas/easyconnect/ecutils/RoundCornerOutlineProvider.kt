package com.vkas.easyconnect.ecutils

import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import com.xuexiang.xui.utils.DensityUtils

class RoundCornerOutlineProvider (private val cornerDp: Float) : ViewOutlineProvider() {
    override fun getOutline(view: View?, outline: Outline?) {
        val sView = view ?: return
        val sOutline = outline ?: return
        sOutline.setRoundRect(
            0,
            0,
            sView.width,
            sView.height,
            DensityUtils.dp2px(cornerDp).toFloat()
        )
    }
}