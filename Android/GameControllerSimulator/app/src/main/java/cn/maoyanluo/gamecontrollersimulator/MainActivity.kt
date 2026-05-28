package cn.maoyanluo.gamecontrollersimulator

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.gamecontrollersimulator.pages.GameControllerPage
import cn.maoyanluo.gamecontrollersimulator.ui.theme.GameControllerSimulatorTheme
import cn.maoyanluo.ui_library.LockScreenOrientation
import cn.maoyanluo.ui_library.pages.SelectDevicePages

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameControllerSimulatorTheme {
                MainContainer(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun MainContainer(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    var hasPermission by remember { mutableStateOf(false) }
    val bluetoothManagerWrapper = remember {
        BluetoothManagerWrapper(ctx)
    }
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
            viewModel.exitGamepad()
        }
        LockScreenOrientation(
            orientation = if (viewModel.selectDevice == null) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        )
        if (viewModel.selectDevice == null) {
            SelectDevicePages(pageModifier, bluetoothManagerWrapper.getBondedDevice(), { viewModel.selectDevice = it })
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


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameControllerSimulatorTheme {
        MainContainer()
    }
}
