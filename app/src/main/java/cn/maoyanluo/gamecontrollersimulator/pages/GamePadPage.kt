package cn.maoyanluo.gamecontrollersimulator.pages

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.bluetooth_library.HIDBluetoothCallback
import cn.maoyanluo.bluetooth_library.HIDBluetoothManager
import cn.maoyanluo.bluetooth_library.bean.HIDRegisterData
import cn.maoyanluo.gamecontrollersimulator.MainViewModel
import cn.maoyanluo.gamecontrollersimulator.R
import cn.maoyanluo.hid_library.GameControllerHID
import cn.maoyanluo.hid_library.GameControllerHIDReportGenerator
import cn.maoyanluo.log_library.LogUtils
import cn.maoyanluo.ui_library.ActionButtons
import cn.maoyanluo.ui_library.ActionButtons2
import cn.maoyanluo.ui_library.CircleTextButton
import cn.maoyanluo.ui_library.DPadButtons
import cn.maoyanluo.ui_library.DPadButtons2
import cn.maoyanluo.ui_library.Joystick
import cn.maoyanluo.ui_library.LeftButtonGroup
import cn.maoyanluo.ui_library.RightButtonGroup
import cn.maoyanluo.ui_library.SquareTextButton

const val TAG = "GameControllerPage"

@Composable
fun GameControllerPage(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    var registerResult by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    val hidBluetoothManager = remember {
        HIDBluetoothManager(ctx, object : HIDBluetoothCallback {
            override fun initResult(result: Boolean) {
                LogUtils.i(TAG, "initResult = $result")
            }

            override fun onAppStatusChanged(
                pluggedDevice: BluetoothDevice?,
                registered: Boolean
            ) {
                LogUtils.i(TAG, "onAppStatusChanged = $registered")
                registerResult = registered
            }

            override fun onConnectionStateChanged(
                device: BluetoothDevice?,
                state: Int
            ) {
                if (device == viewModel.selectDevice) {
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

            override fun release() {
                registerResult = false
            }

        })
    }
    val hidName = stringResource(R.string.hid_name)
    val hidDescription = stringResource(R.string.hid_description)
    val hidProvider = stringResource(R.string.hid_provider)
    DisposableEffect(Unit) {
        LogUtils.i(TAG, "init hidBluetoothManager")
        hidBluetoothManager.init(HIDRegisterData(
            GameControllerHID.HID_REPORT_DESCRIPTOR,
            hidName,
            hidDescription,
            hidProvider
        ))
        viewModel.hidBluetoothManager = hidBluetoothManager
        onDispose {
            LogUtils.i(TAG, "dispose hidBluetoothManager")
            hidBluetoothManager.release()
            viewModel.hidBluetoothManager = null
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (registerResult) {
            fun connectTarget() {
                viewModel.selectDevice?.let {
                    hidBluetoothManager.connect(it)
                }
            }
            DisposableEffect(Unit) {
                connectTarget()
                onDispose {
                    viewModel.selectDevice?.let {
                        hidBluetoothManager.disconnect()
                    }
                }
            }
            if (connected) {
                GameControllerInnerLayout(modifier)
            } else {
                Text(text = stringResource(R.string.not_connected), fontSize = 50.sp, modifier = Modifier.clickable {
                    connectTarget()
                })
            }
        } else {
            Text(text = stringResource(R.string.not_register), fontSize = 50.sp)
        }
    }
}

@Composable
fun GameControllerInnerLayout(modifier: Modifier) {
    val viewModel: MainViewModel = viewModel()
    val generator = remember { GameControllerHIDReportGenerator() }
    DisposableEffect(Unit) {
        generator.startCollection {
            viewModel.hidBluetoothManager?.sendReport(it)
        }
        onDispose {
            generator.stopCollection()
        }
    }
    Column(modifier.fillMaxSize().padding(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                LeftButtonGroup(modifier = Modifier.height(50.dp), fontSize = 20.sp, generator::setButton)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                RightButtonGroup(modifier = Modifier.height(50.dp), fontSize = 20.sp, generator::setButton)
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.padding(50.dp, 0.dp, 0.dp, 0.dp).align(Alignment.TopStart)) {
                    Joystick(modifier = Modifier.size(180.dp), onStickPress = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.L3, true)
                    }, onStickRelease = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.L3, false)
                    }, onAxisChanged = { x,y ->
                        generator.setAxis(GameControllerHIDReportGenerator.Axis.X, x)
                        generator.setAxis(GameControllerHIDReportGenerator.Axis.Y, y)
                    })
                }

                Box(modifier = Modifier.padding(0.dp, 0.dp, 50.dp, 0.dp).align(Alignment.BottomEnd)) {
                    var isDpad by remember { mutableStateOf(true) }
                    if (isDpad) {
                        DPadButtons(modifier = Modifier.size(180.dp), fontSize = 30.sp) { btn, on ->
                            if (on) {
                                generator.setDPad(btn)
                            } else {
                                generator.setDPad(GameControllerHIDReportGenerator.DPad.NEUTRAL)
                            }
                        }
                    } else {
                        DPadButtons2(modifier = Modifier.size(180.dp), fontSize = 30.sp, generator::setButton)
                    }
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        SquareTextButton(text = if (isDpad) "H" else "B", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                            isDpad = !isDpad
                        })
                    }
                }

                Box(modifier = Modifier.padding(0.dp, 50.dp, 50.dp, 0.dp).align(Alignment.TopEnd)) {
                    SquareTextButton(text = "Back", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.BACK, true)
                    }, onUp = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.BACK, false)
                    })
                }

            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.padding(50.dp, 0.dp, 0.dp, 0.dp).align(Alignment.BottomStart)) {
                    Joystick(modifier = Modifier.size(180.dp), onStickPress = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.R3, true)
                    }, onStickRelease = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.R3, false)
                    }, onAxisChanged = { x, y ->
                        generator.setAxis(GameControllerHIDReportGenerator.Axis.RX, x)
                        generator.setAxis(GameControllerHIDReportGenerator.Axis.RY, y)
                    })
                }
                Box(modifier = Modifier.padding(0.dp, 0.dp, 50.dp, 0.dp).align(Alignment.TopEnd)) {
                    var isXbox by remember { mutableStateOf(true) }
                    if (isXbox) {
                        ActionButtons(
                            modifier = Modifier.size(180.dp),
                            fontSize = 30.sp
                        ) { btn, on ->
                            generator.setButton(btn, on)
                        }
                    } else {
                        ActionButtons2(
                            modifier = Modifier.size(180.dp),
                            fontSize = 30.sp
                        ) { btn, on ->
                            generator.setButton(btn, on)
                        }
                    }
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        CircleTextButton(text = if (isXbox) "X" else "P", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                            isXbox = !isXbox
                        })
                    }
                }
                Box(modifier = Modifier.padding(50.dp, 50.dp, 0.dp, 0.dp).align(Alignment.TopStart)) {
                    SquareTextButton(text = "Start", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.START, true)
                    }, onUp = {
                        generator.setButton(GameControllerHIDReportGenerator.Button.START, false)
                    })
                }
            }
        }

    }

}