package com.vkas.easyconnect.ecutils

import android.os.Handler
import android.os.Looper

class Debouncer(private val delayMillis: Long, private val action: () -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    fun debounce() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = Runnable { action() }.also { handler.postDelayed(it, delayMillis) }
    }
}




