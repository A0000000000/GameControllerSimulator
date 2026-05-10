package cn.maoyanluo.log_library

import android.util.Log

class LogUtils {

    companion object {

        fun i(tag: String, msg: String) = Log.i(tag, msg)
        fun d(tag: String, msg: String) = Log.d(tag, msg)
        fun w(tag: String, msg: String) = Log.w(tag, msg)
        fun e(tag: String, msg: String) = Log.e(tag, msg)

        fun i(tag: String, msg: String, t: Throwable) = Log.i(tag, msg, t)
        fun d(tag: String, msg: String, t: Throwable) = Log.d(tag, msg, t)
        fun w(tag: String, msg: String, t: Throwable) = Log.w(tag, msg, t)
        fun e(tag: String, msg: String, t: Throwable) = Log.e(tag, msg, t)

    }


}