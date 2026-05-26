package cn.maoyanluo.gamecontrollersimulator

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.bluetooth_library.BluetoothManagerWrapper
import cn.maoyanluo.gamecontrollersimulator.pages.GameControllerPage
import cn.maoyanluo.gamecontrollersimulator.ui.theme.GameControllerSimulatorTheme
import cn.maoyanluo.ui_library.LockScreenOrientation

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
            SelectDevicePages(pageModifier, bluetoothManagerWrapper::getBondedDevice) {
                viewModel.selectDevice = it
            }
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
@SuppressLint("MissingPermission")
fun SelectDevicePages(modifier: Modifier = Modifier, getBoundsDevices: (() -> List<BluetoothDevice>), onBluetoothDeviceSelected: ((BluetoothDevice) -> Unit)) {
    val ctx = LocalContext.current
    var devicesList by remember {
        mutableStateOf(getBoundsDevices())
    }
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(cn.maoyanluo.ui_library.R.string.pair_devices),
            fontSize = 30.sp,
            modifier = Modifier.padding(0.dp, 10.dp).clickable {
                Toast.makeText(ctx, cn.maoyanluo.ui_library.R.string.start_update, Toast.LENGTH_SHORT).show()
                devicesList = getBoundsDevices()
                Toast.makeText(ctx, cn.maoyanluo.ui_library.R.string.update_finish, Toast.LENGTH_SHORT).show()
            }
        )
        LazyColumn(modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            for (device in devicesList) {
                item {
                    Text(text = device.name ?: device.address ?: "", fontSize = 20.sp, modifier = Modifier.padding(0.dp, 3.dp).clickable {
                        onBluetoothDeviceSelected(device)
                    })
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
