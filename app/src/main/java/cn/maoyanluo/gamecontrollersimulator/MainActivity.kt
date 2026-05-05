package cn.maoyanluo.gamecontrollersimulator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.gamecontrollersimulator.pages.GameControllerPage
import cn.maoyanluo.gamecontrollersimulator.pages.SelectDevicePages
import cn.maoyanluo.gamecontrollersimulator.ui.theme.GameControllerSimulatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameControllerSimulatorTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    MainContainer(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun MainContainer(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    DisposableEffect(Unit) {
        viewModel.coroutineManager.init()
        onDispose {
            viewModel.coroutineManager.destroy()
        }
    }
    var hasPermission by remember { mutableStateOf(false) }
    val pageModifier = if (viewModel.selectDevice == null) {
        modifier.safeDrawingPadding()
    } else {
        Modifier.fillMaxSize()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
       hasPermission = permissions.values.all { it }
    }
    fun requestPermission() {
        launcher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        )
    }
    LaunchedEffect(Unit) {
        requestPermission()
    }
    if (hasPermission) {
        BackHandler(enabled = viewModel.selectDevice != null) {
            viewModel.selectDevice = null
        }
        LockScreenOrientation(
            orientation = if (viewModel.selectDevice == null) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        )
        if (viewModel.selectDevice == null) {
            SelectDevicePages(pageModifier)
        } else {
            GameControllerPage(pageModifier)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.no_permission), fontSize = 50.sp, modifier = Modifier.clickable {
                requestPermission()
            })
        }
    }
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameControllerSimulatorTheme {
        MainContainer()
    }
}
