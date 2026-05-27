package cn.maoyanluo.ui_library

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

interface KeyEventHandler {
    var hardwareKeyEventHandler: ((KeyEvent) -> Boolean)?
}
@Composable
fun LockScreenOrientation(orientation: Int) {
    val context = LocalContext.current
    DisposableEffect(orientation) {
        val activity = context.findActivity()
        val original = activity?.requestedOrientation
        val window = activity?.window
        val decorView = window?.decorView
        val insetsController = if (window != null && decorView != null) {
            WindowInsetsControllerCompat(window, decorView)
        } else {
            null
        }
        val immersiveLandscape = orientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        activity?.requestedOrientation = orientation
        if (immersiveLandscape) {
            insetsController?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            activity?.requestedOrientation = original ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Context.findKeyEventHandler(): KeyEventHandler? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is KeyEventHandler) return ctx
        ctx = ctx.baseContext
    }
    return null
}