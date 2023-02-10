package com.vkas.easyconnect.ecui.ecservice

import android.app.AlertDialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.reflect.TypeToken
import com.jeremyliao.liveeventbus.LiveEventBus
import com.vkas.easyconnect.BR
import com.vkas.easyconnect.R
import com.vkas.easyconnect.databinding.ActivityServiceListEcBinding
import com.vkas.easyconnect.ecad.EcLoadBackAd
import com.vkas.easyconnect.ecapp.App
import com.vkas.easyconnect.ecbase.BaseActivity
import com.vkas.easyconnect.ecbean.EcVpnBean
import com.vkas.easyconnect.ecenevt.Constant
import com.vkas.easyconnect.ecenevt.Constant.logTagEc
import com.vkas.easyconnect.ecutils.EasyConnectUtils
import com.vkas.easyconnect.ecutils.KLog
import com.xuexiang.xutil.net.JsonUtil
import kotlinx.coroutines.Job

class ServiceListEcActivity : BaseActivity<ActivityServiceListEcBinding, ServiceListEcViewModel>() {
    private lateinit var selectAdapter: ServiceListEcAdapter
    private var ecServiceBeanList: MutableList<EcVpnBean> = ArrayList()
    private lateinit var adBean: EcVpnBean

    private var jobBackEc: Job? = null

    //选中服务器
    private lateinit var checkSkServiceBean: EcVpnBean
    private lateinit var checkSkServiceBeanClick: EcVpnBean

    // 是否连接
    private var whetherToConnect = false
    override fun initContentView(savedInstanceState: Bundle?): Int {
        return R.layout.activity_service_list_ec
    }

    override fun initVariableId(): Int {
        return BR._all
    }

    override fun initParam() {
        super.initParam()
        val bundle = intent.extras
        checkSkServiceBean = EcVpnBean()
        whetherToConnect = bundle?.getBoolean(Constant.WHETHER_EC_CONNECTED) == true
        checkSkServiceBean = JsonUtil.fromJson(
            bundle?.getString(Constant.CURRENT_EC_SERVICE),
            object : TypeToken<EcVpnBean?>() {}.type
        )
        checkSkServiceBeanClick = checkSkServiceBean
    }

    override fun initToolbar() {
        super.initToolbar()
        liveEventBusReceive()
        binding.selectTitleEc.tvTitle.text = getString(R.string.locations)
        binding.selectTitleEc.imgBack.setOnClickListener {
            returnToHomePage()
        }
    }

    override fun initData() {
        super.initData()
        initSelectRecyclerView()
        viewModel.getServerListData()
        EcLoadBackAd.getInstance().whetherToShowEc = false
    }

    override fun initViewObservable() {
        super.initViewObservable()
        getServerListData()
    }
    private fun liveEventBusReceive() {
        //插屏关闭后跳转
        LiveEventBus
            .get(Constant.PLUG_EC_BACK_AD_SHOW, Boolean::class.java)
            .observeForever {
                finish()
            }
    }
    private fun getServerListData() {
        viewModel.liveServerListData.observe(this, {
            echoServer(it)
        })
    }

    private fun initSelectRecyclerView() {
        selectAdapter = ServiceListEcAdapter(ecServiceBeanList)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        binding.recyclerSelect.layoutManager = layoutManager
        binding.recyclerSelect.adapter = selectAdapter
        selectAdapter.setOnItemClickListener { _, _, pos ->
            run {
                selectServer(pos)
            }
        }
    }


    /**
     * 选中服务器
     */
    private fun selectServer(position: Int) {
        if (ecServiceBeanList[position].ec_ip == checkSkServiceBeanClick.ec_ip && ecServiceBeanList[position].ec_best == checkSkServiceBeanClick.ec_best) {
            if (!whetherToConnect) {
                finish()
                LiveEventBus.get<EcVpnBean>(Constant.NOT_CONNECTED_EC_RETURN)
                    .post(checkSkServiceBean)
            }
            return
        }
        ecServiceBeanList.forEachIndexed { index, _ ->
            ecServiceBeanList[index].ec_check = position == index
            if (ecServiceBeanList[index].ec_check == true) {
                checkSkServiceBean = ecServiceBeanList[index]
            }
        }
        selectAdapter.notifyDataSetChanged()
        showDisconnectDialog()
    }

    /**
     * 回显服务器
     */
    private fun echoServer(it: MutableList<EcVpnBean>) {
        ecServiceBeanList = it
        ecServiceBeanList.forEachIndexed { index, _ ->
            if (checkSkServiceBeanClick.ec_best == true) {
                ecServiceBeanList[0].ec_check = true
            } else {
                ecServiceBeanList[index].ec_check =
                    ecServiceBeanList[index].ec_ip == checkSkServiceBeanClick.ec_ip
                ecServiceBeanList[0].ec_check = false
            }
        }
        KLog.e("TAG", "ecServiceBeanList=${JsonUtil.toJson(ecServiceBeanList)}")
        selectAdapter.setList(ecServiceBeanList)
    }

    /**
     * 返回主页
     */
    private fun returnToHomePage() {
        App.isAppOpenSameDayEc()
        if (EasyConnectUtils.isThresholdReached()) {
            KLog.d(logTagEc, "广告达到上线")
            finish()
            return
        }
        if(!EcLoadBackAd.getInstance().displayBackAdvertisementEc(this)){
            finish()
        }
    }

    /**
     * 是否断开连接
     */
    private fun showDisconnectDialog() {
        if (!whetherToConnect) {
            finish()
            LiveEventBus.get<EcVpnBean>(Constant.NOT_CONNECTED_EC_RETURN)
                .post(checkSkServiceBean)
            return
        }
        val dialog: AlertDialog? = AlertDialog.Builder(this)
            .setTitle("Are you sure to disconnect current server")
            //设置对话框的按钮
            .setNegativeButton("CANCEC") { dialog, _ ->
                dialog.dismiss()
                ecServiceBeanList.forEachIndexed { index, _ ->
                    ecServiceBeanList[index].ec_check =
                        (ecServiceBeanList[index].ec_ip == checkSkServiceBeanClick.ec_ip && ecServiceBeanList[index].ec_best == checkSkServiceBeanClick.ec_best)
                }
                selectAdapter.notifyDataSetChanged()
            }
            .setPositiveButton("DISCONNECT") { dialog, _ ->
                dialog.dismiss()
                finish()
                LiveEventBus.get<EcVpnBean>(Constant.CONNECTED_EC_RETURN)
                    .post(checkSkServiceBean)
            }.create()

        val params = dialog!!.window!!.attributes
        params.width = 200
        params.height = 200
        dialog.window!!.attributes = params
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.BLACK)
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.setTextColor(Color.BLACK)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            returnToHomePage()
        }
        return true
    }
}