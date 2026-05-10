package cn.maoyanluo.gamecontrollersimulator2

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import cn.maoyanluo.bluetooth_library.BluetoothSelectManager
import cn.maoyanluo.bluetooth_library.socket.BluetoothSocketClient
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.ui.theme.GameControllerSimulatorTheme
import cn.maoyanluo.socket_common_library.SocketClientCallback
import cn.maoyanluo.socket_common_library.SocketServerCallback
import java.util.UUID

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
@SuppressLint("MissingPermission")
fun SelectDevicePages(modifier: Modifier = Modifier, onDeviceSelect: ((device: BluetoothDevice) -> Unit)) {
    val ctx = LocalContext.current
    val bluetoothSelectManager = remember { BluetoothSelectManager(ctx) }
    var devicesList by remember {
        mutableStateOf(bluetoothSelectManager.getBondedDevice())
    }
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "设备列表",
            fontSize = 30.sp,
            modifier = Modifier.padding(0.dp, 10.dp)
        )
        LazyColumn(modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            for (device in devicesList) {
                item {
                    Text(text = device.name, fontSize = 20.sp, modifier = Modifier.padding(0.dp, 3.dp).clickable {
                        onDeviceSelect(device)
                    })
                }
            }
        }
    }

}
@Composable
fun MainContainer(modifier: Modifier = Modifier) {
    var hasPermission by remember { mutableStateOf(false) }
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
    val ctx = LocalContext.current
    SelectDevicePages(modifier) {
        val manager = BluetoothSelectManager(ctx)
        val uuid = "0000180D-0000-1000-8000-00805f9b34fb"
        val coroutineManager = CoroutineManager()
        coroutineManager.init()
        var client: BluetoothSocketClient? = null
        client = BluetoothSocketClient(manager.getAdapter(), it, UUID.fromString(uuid), object: SocketClientCallback {
            override fun onConnectSuccess() {
                client?.sendData("Hello from client".toByteArray())
            }

            override fun onConnectException(e: Exception) {

            }

            override fun onSendDataException(e: Exception, id: Int) {

            }

            override fun onDisconnect() {

            }

            override fun onDataReady(data: ByteArray) {
                Log.e("maoyanluo", "data = ${String(data)}")
            }

            override fun onDataRevException(e: Exception) {

            }

        }, coroutineManager)
        client?.connect()


    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameControllerSimulatorTheme {
        MainContainer()
    }
}
