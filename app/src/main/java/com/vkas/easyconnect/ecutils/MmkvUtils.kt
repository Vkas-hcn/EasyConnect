package com.vkas.easyconnect.ecutils

import com.tencent.mmkv.MMKV

object MmkvUtils {
    private val TAG = MmkvUtils::class.java.simpleName

    fun putIntValue(name: String?, value: Int) {
        MMKV.defaultMMKV()
            .encode(name, value)
    }

    fun getIntValue(name: String?): Int {
        return MMKV.defaultMMKV()
            .decodeInt(name)
    }

    fun putStringValue(name: String, value: String) {
        KLog.e("MMkV====$name:$value")
        MMKV.defaultMMKV().encode(name, value)
    }


    fun getStringValue(name: String?): String? {
        return MMKV.defaultMMKV().decodeString(name, "")
    }

    fun putBooleanValue(name: String?, value: Boolean) {
        MMKV.defaultMMKV().encode(name, value)
    }

    fun getBooleanValue(name: String?): Boolean {
        return MMKV.defaultMMKV().decodeBool(name, false)
    }

    /**
     * 存储数据
     *
     * @param key
     * @param value
     */
    fun set(key: String?, value: Any?) {
        val kv = MMKV.mmkvWithID("EasyConnect", MMKV.MULTI_PROCESS_MODE)
        var result = false
        if (value is Int) {
            result = kv.encode(key, (value as Int?)!!)
        } else if (value is Long) {
            result = kv.encode(key, (value as Long?)!!)
        } else if (value is Float) {
            result = kv.encode(key, (value as Float?)!!)
        } else if (value is Double) {
            result = kv.encode(key, (value as Double?)!!)
        } else if (value is Boolean) {
            result = kv.encode(key, (value as Boolean?)!!)
        } else if (value is String) {
            result = kv.encode(key, value as String?)
        } else if (value is ByteArray) {
            result = kv.encode(key, value as ByteArray?)
        }
    }

    /**
     * 获取数据
     *
     * @param key
     * @param defValue
     * @param <T>
     * @return
    </T> */
    operator fun <T> get(key: String?, defValue: T): T? {
        val kv = MMKV.defaultMMKV()
        var result: Any? = null
        if (defValue is Int) {
            result = kv.decodeInt(key, (defValue as Int))
        } else if (defValue is Long) {
            result = kv.decodeLong(key, (defValue as Long))
        } else if (defValue is Float) {
            result = kv.decodeFloat(key, (defValue as Float))
        } else if (defValue is Double) {
            result = kv.decodeDouble(key, (defValue as Double))
        } else if (defValue is Boolean) {
            result = kv.decodeBool(key, (defValue as Boolean))
        } else if (defValue is String) {
            result = kv.decodeString(key, defValue as String)
        } else if (defValue is ByteArray) {
            result = kv.decodeBytes(key)
        }
        return result as T?
    }
}