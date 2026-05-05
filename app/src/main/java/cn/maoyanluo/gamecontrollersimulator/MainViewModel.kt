package cn.maoyanluo.gamecontrollersimulator

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cn.maoyanluo.bluetooth_library.hid.HIDBluetoothCallback
import cn.maoyanluo.bluetooth_library.hid.HIDBluetoothManager
import cn.maoyanluo.bluetooth_library.hid.bean.HIDRegisterData
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator.pages.TAG
import cn.maoyanluo.hid_library.GameControllerHIDReportGenerator
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.launch

class MainViewModel(application: Application): AndroidViewModel(application) {
    var selectDevice: BluetoothDevice? by mutableStateOf(null)
    var registerResult by mutableStateOf(false)
    var connected by mutableStateOf(false)
    val coroutineManager = CoroutineManager()
    val generator = GameControllerHIDReportGenerator(coroutineManager)

    val hidBluetoothManager = HIDBluetoothManager(application, object : HIDBluetoothCallback {
        override fun initResult(result: Boolean) {
            LogUtils.i(TAG, "initResult = $result")
        }

        override fun onAppStatusChanged(
            pluggedDevice: BluetoothDevice?,
            registered: Boolean
        ) {
            LogUtils.i(TAG, "onAppStatusChanged = $registered")
            coroutineManager.getMainScope().launch {
                registerResult = registered
            }
        }

        override fun onConnectionStateChanged(
            device: BluetoothDevice?,
            state: Int
        ) {
            coroutineManager.getMainScope().launch {
                if (device == selectDevice) {
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            connected = true
                        }

                        BluetoothProfile.STATE_CONNECTING -> {

                        }

                        BluetoothProfile.STATE_DISCONNECTING -> {

                        }

                        BluetoothProfile.STATE_DISCONNECTED -> {
                            connected = false
                        }
                    }
                }
            }
        }

        override fun release() {
            coroutineManager.getMainScope().launch {
                registerResult = false
            }
        }

    }, coroutineManager)

    init {
        coroutineManager.init()
        addCloseable {
            coroutineManager.destroy()
        }
    }

    fun initHidBluetoothManager(hidRegisterData: HIDRegisterData) {
        LogUtils.i(TAG, "init hidBluetoothManager")
        hidBluetoothManager.init(hidRegisterData)
    }

    fun releaseHidBluetoothManager() {
        LogUtils.i(TAG, "dispose hidBluetoothManager")
        hidBluetoothManager.release()
    }

    fun connectTargetDevice() {
        selectDevice?.let {
            hidBluetoothManager.connect(it)
        }
    }

    fun disconnectTargetDevice() {
        hidBluetoothManager.disconnect()
        connected = false
    }

    fun startCollection() {
        generator.startCollection(hidBluetoothManager::sendReport)
    }

    fun stopCollection() {
        generator.stopCollection()
    }

    fun exitGamepad() {
        stopCollection()
        disconnectTargetDevice()
        releaseHidBluetoothManager()
        selectDevice = null
    }

}