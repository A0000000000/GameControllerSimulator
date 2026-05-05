package cn.maoyanluo.gamecontrollersimulator

import android.bluetooth.BluetoothDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cn.maoyanluo.bluetooth_library.hid.HIDBluetoothManager
import cn.maoyanluo.coroutine_library.CoroutineManager

class MainViewModel(): ViewModel() {
    var selectDevice: BluetoothDevice? by mutableStateOf(null)
    var hidBluetoothManager: HIDBluetoothManager? = null
    var coroutineManager: CoroutineManager = CoroutineManager()

}