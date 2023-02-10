package com.vkas.easyconnect.ecbase

import androidx.lifecycle.MutableLiveData

class StateLiveData <T> : MutableLiveData<T>() {
    enum class StateEnum {
        Idle, Loading, Success, Error, NoData, NoMoreData, NoNet
    }
    // 封装的枚举,用MutableLiveData可以被view观察;
    var stateEnumMutableLiveData = MutableLiveData<StateEnum>()
    fun postValueAndSuccess(t: T) {
        super.postValue(t)
        postSuccess()
    }

    fun postIdle() {
        stateEnumMutableLiveData.postValue(StateEnum.Idle)
    }

    fun postLoading() {
        stateEnumMutableLiveData.postValue(StateEnum.Loading)
    }

    fun postNoData() {
        stateEnumMutableLiveData.postValue(StateEnum.NoData)
    }

    fun postNoMoreData() {
        stateEnumMutableLiveData.postValue(StateEnum.NoMoreData)
    }

    fun postNoNet() {
        stateEnumMutableLiveData.postValue(StateEnum.NoNet)
    }

    fun postSuccess() {
        stateEnumMutableLiveData.postValue(StateEnum.Success)
    }

    fun postError() {
        stateEnumMutableLiveData.postValue(StateEnum.Error)
    }

    fun changeState(stateEnum: StateEnum) {
        stateEnumMutableLiveData.postValue(stateEnum)
    }
}