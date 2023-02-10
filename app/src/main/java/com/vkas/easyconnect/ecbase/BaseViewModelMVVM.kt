package com.vkas.easyconnect.ecbase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

open class BaseViewModelMVVM (application: Application) : AndroidViewModel(application), IBaseViewModelMVVM{
    override fun onAny(owner: LifecycleOwner?, event: Lifecycle.Event?) {}
    override fun onCreate() {}
    override fun onDestroy() {}
    override fun onStart() {}
    override fun onStop() {}
    override fun onResume() {}
    override fun onPause() {}
    override fun onCleared() {
        super.onCleared()
        //ViewModel销毁时会执行，同时取消所有异步任务

    }
}