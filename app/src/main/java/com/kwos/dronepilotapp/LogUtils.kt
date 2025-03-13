package com.kwos.dronepilotapp

import android.util.Log

fun logDebug(tag: String, message: String) {
    if (LogConfig.DEBUG_MODE) {
        Log.d(tag, message)
    }
}

fun logError(tag: String, message: String, throwable: Throwable? = null) {
    if (LogConfig.DEBUG_MODE) {
        Log.e(tag, message, throwable)
    }
}

fun logWarning(tag: String, message: String) {
    if (LogConfig.DEBUG_MODE) {
        Log.w(tag, message)
    }
}
