package cn.maoyanluo.bluetooth_library

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import cn.maoyanluo.bluetooth_library.bean.HIDRegisterData
import cn.maoyanluo.bluetooth_library.utils.CoroutineExecutorWrapper
import cn.maoyanluo.log_library.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 这个用于处理向系统注册HID设备相关的逻辑
 */
@SuppressLint("MissingPermission")
class HIDBluetoothManager(private val ctx: Context, private val callback: HIDBluetoothCallback) {

    companion object {
        val TAG = HIDBluetoothManager::class.simpleName ?: "HIDBluetoothManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val executor = CoroutineExecutorWrapper(scope)

    private val bluetoothManager = ctx.getSystemService(BluetoothManager::class.java)
    private val adapter = bluetoothManager.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var device: BluetoothDevice? = null

    /**
     * 初始化注册设备，向系统报告
     */

    fun init(registerData: HIDRegisterData) {
        scope.launch {
            adapter.getProfileProxy(ctx, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    LogUtils.i(TAG, "onServiceConnected profile = $profile, proxy = $proxy")
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        scope.launch {
                            hidDevice = proxy as BluetoothHidDevice
                            val sdp = BluetoothHidDeviceAppSdpSettings(
                                registerData.name,
                                registerData.description,
                                registerData.provider,
                                BluetoothHidDevice.SUBCLASS1_NONE,
                                registerData.hidReporter
                            )
                            val qos = BluetoothHidDeviceAppQosSettings(
                                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                                800,
                                9,
                                0,
                                11250,
                                11250
                            )
                            withContext(Dispatchers.Main) {
                                val result = hidDevice?.registerApp(
                                    sdp,
                                    null,
                                    qos,
                                    executor,
                                    object : BluetoothHidDevice.Callback() {

                                        override fun onAppStatusChanged(
                                            pluggedDevice: BluetoothDevice?,
                                            registered: Boolean
                                        ) {
                                            super.onAppStatusChanged(pluggedDevice, registered)
                                            // 用于处理App是否注册成功
                                            LogUtils.i(TAG, "onAppStatusChanged pluggedDevice = $pluggedDevice, registered = $registered")
                                            callback.onAppStatusChanged(pluggedDevice, registered)
                                        }

                                        override fun onConnectionStateChanged(
                                            device: BluetoothDevice?,
                                            state: Int
                                        ) {
                                            super.onConnectionStateChanged(device, state)
                                            // 用于处理被动的连接
                                            LogUtils.i(TAG, "onConnectionStateChanged. device = $device, state = $state")
                                            when (state) {
                                                BluetoothProfile.STATE_CONNECTED -> {
                                                    // 暂时忽略被动连接的设备
                                                    // this@HIDBluetoothManager.device = device
                                                }
                                                BluetoothProfile.STATE_CONNECTING -> {

                                                }
                                                BluetoothProfile.STATE_DISCONNECTING -> {

                                                }
                                                BluetoothProfile.STATE_DISCONNECTED -> {
                                                    if (this@HIDBluetoothManager.device == device) {
                                                        this@HIDBluetoothManager.device = null
                                                    }
                                                }
                                            }
                                            callback.onConnectionStateChanged(device, state)
                                        }

                                    }
                                )
                                callback.initResult(result ?: false)
                            }
                        }
                    }
                }

                override fun onServiceDisconnected(profile: Int) {

                }
            }, BluetoothProfile.HID_DEVICE)
        }
    }

    fun connect(device: BluetoothDevice): Boolean {
        LogUtils.i(TAG, "connect device = $device")
        this.device = device
        return hidDevice?.connect(device) ?: false
    }

    fun sendReport(report: ByteArray) {
        scope.launch {
            val res = hidDevice?.sendReport(device, 0, report)
            LogUtils.i(TAG, "sendReport res = $res, report = $report, device = $device, hidDevice = $hidDevice")
        }
    }

    fun disconnect() {
        scope.launch {
            device?.let {
                hidDevice?.disconnect(it)
            }
        }
    }

    fun release() {
        scope.launch {
            hidDevice?.unregisterApp()
            adapter.closeProfileProxy(
                BluetoothProfile.HID_DEVICE,
                hidDevice
            )
            hidDevice = null
            callback.release()
        }
    }

}