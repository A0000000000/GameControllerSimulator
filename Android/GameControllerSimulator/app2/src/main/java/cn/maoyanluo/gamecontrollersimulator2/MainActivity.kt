package cn.maoyanluo.gamecontrollersimulator2

import android.Manifest
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.bluetooth_library.BluetoothSelectManager
import cn.maoyanluo.gamecontrollersimulator2.ui.theme.GameControllerSimulatorTheme
import cn.maoyanluo.ui_library.LockScreenOrientation
import cn.maoyanluo.ui_library.pages.SelectDevicePages

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
    val uiState = viewModel.mainUiState
    val pageModifier = if (uiState !is MainUiState.GamepadPage) {
        modifier.safeDrawingPadding()
    } else {
        Modifier.fillMaxSize()
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.setPermission(permissions.values.all { it })
    }
    LockScreenOrientation(
        orientation = if (uiState !is MainUiState.GamepadPage) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    )
    when (uiState) {
        MainUiState.NoPermissionPage -> {
            fun requestPermission() {
                launcher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT
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
        MainUiState.SelectPage -> {
            val ctx = LocalContext.current
            val bluetoothSelectManager = remember { BluetoothSelectManager(ctx) }
            SelectDevicePages(pageModifier, bluetoothSelectManager::getBondedDevice) {
                viewModel.setCurrentDevice(it)
            }
        }
        is MainUiState.ConnectingPage -> {
            BackHandler() {
                viewModel.disconnect()
            }
        }
        is MainUiState.GamepadPage -> {
            BackHandler() {
                viewModel.exitGamepadPage()
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
