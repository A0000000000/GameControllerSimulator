package cn.maoyanluo.gamecontrollersimulator2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import cn.maoyanluo.ui_library.KeyEventHandler
import cn.maoyanluo.ui_library.LockScreenOrientation
import cn.maoyanluo.ui_library.pages.SelectDevicePages

class MainActivity : ComponentActivity(), KeyEventHandler {
    override var hardwareKeyEventHandler: ((KeyEvent) -> Boolean)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameControllerSimulatorTheme {
                MainContainer(Modifier.fillMaxSize())
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean = hardwareKeyEventHandler?.invoke(event) == true || super.dispatchKeyEvent(event)

}

@Composable
fun MainContainer(modifier: Modifier = Modifier) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.mainUiState.collectAsState()
    val uiEffect = viewModel.mainUiEffect
    val ctx by rememberUpdatedState(LocalContext.current)
    val pageModifier = if (uiState is MainUiState.GamepadPage) {
        modifier.fillMaxSize()
    } else {
        modifier.safeDrawingPadding()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.onUiIntent(MainUiIntent.PermissionResultIntent(permissions.values.all { it }))
    }
    LockScreenOrientation(
        orientation = if (uiState !is MainUiState.GamepadPage) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    )
    when (val state = uiState) {
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
                Text(
                    text = stringResource(R.string.no_permission),
                    fontSize = 50.sp,
                    modifier = Modifier.clickable {
                        requestPermission()
                    })
            }
        }

        is MainUiState.SelectPage -> {
            SelectDevicePages(
                pageModifier,
                state.data.devices,
                onBluetoothDeviceSelected = {
                    viewModel.onUiIntent(
                        MainUiIntent.OnDeviceSelectedIntent(it)
                    )
                }) {
                viewModel.onUiIntent(MainUiIntent.OnDeviceListFlush)
            }
        }

        is MainUiState.ConnectingPage -> {
            BackHandler {
                viewModel.onUiIntent(MainUiIntent.OnDisconnect)
            }
            ConnectingPage(
                modifier = pageModifier,
                uiData = state.data,
                onUiIntent = viewModel::onUiIntent
            )
        }

        MainUiState.GamepadPage -> {
            BackHandler {
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
                    Toast.makeText(
                        ctx,
                        String.format(ctx.getString(R.string.rtt_result), it.type, it.diff),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                is MainUiEffect.DeviceFlushEffect -> {
                    Toast.makeText(
                        ctx,
                        ctx.getString(if (it.isEnd) R.string.device_list_flush_end else R.string.device_list_flush),
                        Toast.LENGTH_SHORT
                    ).show()
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
