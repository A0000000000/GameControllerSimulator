package cn.maoyanluo.gamecontrollersimulator2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiEffect
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiIntent
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiState
import cn.maoyanluo.gamecontrollersimulator2.pages.ConnectingPage
import cn.maoyanluo.gamecontrollersimulator2.pages.GamepadPage
import cn.maoyanluo.gamecontrollersimulator2.ui.theme.GameControllerSimulatorTheme
import cn.maoyanluo.ui_library.LockScreenOrientation
import cn.maoyanluo.ui_library.pages.SelectDevicePages

class MainActivity : ComponentActivity() {
    var hardwareKeyEventHandler: ((KeyEvent) -> Boolean)? = null

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

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handler = hardwareKeyEventHandler
        if (handler != null && handler.invoke(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun MainContainer(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    val uiState = viewModel.mainUiState
    val uiEffect = viewModel.mainUiEffect
    val ctx = LocalContext.current
    val pageModifier = if (uiState !is MainUiState.GamepadPage) {
        modifier.safeDrawingPadding()
    } else {
        Modifier.fillMaxSize()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onUiIntent(MainUiIntent.PermissionResultIntent(permissions.values.all { it }))
    }
    LockScreenOrientation(
        orientation = if (uiState !is MainUiState.GamepadPage) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    )
    when (uiState) {
        MainUiState.NoPermissionPage -> {
            fun requestPermission() {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
            LaunchedEffect(Unit) {
                requestPermission()
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_permission), fontSize = 50.sp, modifier = Modifier.clickable {
                    requestPermission()
                })
            }
        }
        is MainUiState.SelectPage -> {
            SelectDevicePages(pageModifier, uiState.data.devices, onBluetoothDeviceSelected = { viewModel.onUiIntent(MainUiIntent.OnDeviceSelectedIntent(it)) }) {
                viewModel.onUiIntent(MainUiIntent.OnDeviceListFlush)
            }
        }
        is MainUiState.ConnectingPage -> {
            BackHandler() {
                viewModel.onUiIntent(MainUiIntent.OnDisconnect)
            }
            ConnectingPage(
                modifier = pageModifier,
                uiData = uiState.data,
                onUiIntent = viewModel::onUiIntent
            )
        }
        MainUiState.GamepadPage -> {
            BackHandler() {
                viewModel.onUiIntent(MainUiIntent.OnBackFromGamepad)
            }
            GamepadPage(
                modifier = pageModifier,
                generator = viewModel.getGamepadEventGenerator()
            ) {
                viewModel.onUiIntent(MainUiIntent.OnGamepadEvent(it))
            }
        }
    }
    LaunchedEffect(Unit) {
        uiEffect.collect {
            when (it) {
                is MainUiEffect.RttResultEffect -> {
                    Toast.makeText(ctx, "类型: ${it.type}, RTT: ${it.diff}", Toast.LENGTH_SHORT).show()
                }

                is MainUiEffect.DeviceFlushEffect -> {
                    Toast.makeText(ctx, if (it.isEnd) "设备列表更新完成" else "设备列表开始更新", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameControllerSimulatorTheme {
        MainContainer()
    }
}
