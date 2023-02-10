package com.github.shadowsocks.utils

import com.google.gson.Gson
import com.google.gson.JsonParseException
import org.json.JSONException
import org.json.JSONObject
import java.lang.UnsupportedOperationException
import java.lang.reflect.Type

/**
 * <pre>
 * desc   :	json转化工具
</pre> *
 */
class JsonUtils private constructor() {
    companion object {
        /**
         * 把 JSON 字符串 转换为 单个指定类型的对象
         *
         * @param json
         * 包含了单个对象数据的JSON字符串
         * @param classOfT
         * 指定类型对象的Class
         * @return 指定类型对象
         */
        fun <T> fromJson(json: String?, classOfT: Class<T>?): T? {
            try {
                return Gson().fromJson(json, classOfT)
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 解析Json字符串
         * @param json Json字符串
         * @param typeOfT 泛型类
         * @param <T>
         * @return
        </T> */
        fun <T> fromJson(json: String?, typeOfT: Type?): T? {
            try {
                return Gson().fromJson(json, typeOfT)
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * 把 单个指定类型的对象 转换为 JSON 字符串
         * @param src
         * @return
         */
        fun toJson(src: Any?): String {
            return Gson().toJson(src)
        }

        /**
         * 把 单个指定类型的对象 转换为 JSONObject对象
         * @param src
         * @return
         */
        fun toJSONObject(src: Any?): JSONObject? {
            return toJSONObject(toJson(src))
        }

        /**
         * 把 JSON 字符串 转换为 JSONObject对象
         * @param json
         * @return
         */
        fun toJSONObject(json: String?): JSONObject? {
            var jsonObject: JSONObject? = null
            try {
                jsonObject = JSONObject(json)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            return jsonObject
        }
    }

    /**
     * Don't let anyone instantiate this class.
     */
    init {
        throw UnsupportedOperationException("u can't instantiate me...")
    }
}