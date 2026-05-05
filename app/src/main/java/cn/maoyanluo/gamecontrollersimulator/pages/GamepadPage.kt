package cn.maoyanluo.gamecontrollersimulator.pages

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.bluetooth_library.hid.bean.HIDRegisterData
import cn.maoyanluo.gamecontrollersimulator.MainViewModel
import cn.maoyanluo.gamecontrollersimulator.R
import cn.maoyanluo.hid_library.GameControllerHID
import cn.maoyanluo.hid_library.GameControllerHIDReportGenerator
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
    val viewModel: MainViewModel = viewModel()
    val hidName = stringResource(R.string.hid_name)
    val hidDescription = stringResource(R.string.hid_description)
    val hidProvider = stringResource(R.string.hid_provider)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object: DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                if (!viewModel.registerResult) {
                    viewModel.initHidBluetoothManager(HIDRegisterData(
                        GameControllerHID.HID_REPORT_DESCRIPTOR,
                        hidName,
                        hidDescription,
                        hidProvider
                    ))
                }
            }

        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (viewModel.registerResult) {
            DisposableEffect(lifecycleOwner) {
                val observer = object: DefaultLifecycleObserver {
                    override fun onResume(owner: LifecycleOwner) {
                        super.onResume(owner)
                        if (!viewModel.connected) {
                            viewModel.connectTargetDevice()
                        }
                    }

                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }
            if (viewModel.connected) {
                GameControllerInnerLayout(modifier)
            } else {
                Text(text = stringResource(R.string.not_connected), fontSize = 50.sp)
            }
        } else {
            Text(text = stringResource(R.string.not_register), fontSize = 50.sp)
        }
    }
}

@Composable
fun GameControllerInnerLayout(modifier: Modifier) {
    val viewModel: MainViewModel = viewModel()
    DisposableEffect(Unit) {
        viewModel.startCollection()
        onDispose {
            viewModel.stopCollection()
        }
    }
    Column(modifier.fillMaxSize().padding(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                LeftButtonGroup(modifier = Modifier.height(50.dp), fontSize = 20.sp, viewModel.generator::setButton)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                RightButtonGroup(modifier = Modifier.height(50.dp), fontSize = 20.sp, viewModel.generator::setButton)
            }
        }
        Row(modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.padding(50.dp, 0.dp, 0.dp, 0.dp).align(Alignment.TopStart)) {
                    Joystick(modifier = Modifier.size(180.dp), onStickPress = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.L3, true)
                    }, onStickRelease = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.L3, false)
                    }, onAxisChanged = { x,y ->
                        viewModel.generator.setAxis(GameControllerHIDReportGenerator.Axis.X, x)
                        viewModel.generator.setAxis(GameControllerHIDReportGenerator.Axis.Y, y)
                    })
                }

                Box(modifier = Modifier.padding(0.dp, 0.dp, 50.dp, 0.dp).align(Alignment.BottomEnd)) {
                    var isDpad by remember { mutableStateOf(true) }
                    if (isDpad) {
                        DPadButtons(modifier = Modifier.size(180.dp), fontSize = 30.sp) { btn, on ->
                            if (on) {
                                viewModel.generator.setDPad(btn)
                            } else {
                                viewModel.generator.setDPad(GameControllerHIDReportGenerator.DPad.NEUTRAL)
                            }
                        }
                    } else {
                        DPadButtons2(modifier = Modifier.size(180.dp), fontSize = 30.sp, viewModel.generator::setButton)
                    }
                    Box(modifier = Modifier.align(Alignment.Center)) {
                        SquareTextButton(text = if (isDpad) "H" else "B", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                            isDpad = !isDpad
                        })
                    }
                }

                Box(modifier = Modifier.padding(0.dp, 50.dp, 50.dp, 0.dp).align(Alignment.TopEnd)) {
                    SquareTextButton(text = "Back", fontSize = 20.sp, modifier = Modifier.size(50.dp), onDown = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.BACK, true)
                    }, onUp = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.BACK, false)
                    })
                }

            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.padding(50.dp, 0.dp, 0.dp, 0.dp).align(Alignment.BottomStart)) {
                    Joystick(modifier = Modifier.size(180.dp), onStickPress = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.R3, true)
                    }, onStickRelease = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.R3, false)
                    }, onAxisChanged = { x, y ->
                        viewModel.generator.setAxis(GameControllerHIDReportGenerator.Axis.RX, x)
                        viewModel.generator.setAxis(GameControllerHIDReportGenerator.Axis.RY, y)
                    })
                }
                Box(modifier = Modifier.padding(0.dp, 0.dp, 50.dp, 0.dp).align(Alignment.TopEnd)) {
                    var isXbox by remember { mutableStateOf(true) }
                    if (isXbox) {
                        ActionButtons(
                            modifier = Modifier.size(180.dp),
                            fontSize = 30.sp
                        ) { btn, on ->
                            viewModel.generator.setButton(btn, on)
                        }
                    } else {
                        ActionButtons2(
                            modifier = Modifier.size(180.dp),
                            fontSize = 30.sp
                        ) { btn, on ->
                            viewModel.generator.setButton(btn, on)
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
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.START, true)
                    }, onUp = {
                        viewModel.generator.setButton(GameControllerHIDReportGenerator.Button.START, false)
                    })
                }
            }
        }

    }

}