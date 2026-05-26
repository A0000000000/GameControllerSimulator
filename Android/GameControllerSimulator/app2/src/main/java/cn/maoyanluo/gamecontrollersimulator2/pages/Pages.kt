package cn.maoyanluo.gamecontrollersimulator2.pages

import android.annotation.SuppressLint
import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.maoyanluo.gamecontrollersimulator2.mainui.ConnectingPageModel
import cn.maoyanluo.gamecontrollersimulator2.R
import cn.maoyanluo.gamecontrollersimulator2.MainActivity
import cn.maoyanluo.gamecontrollersimulator2.mainui.MainUiIntent
import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadAxis
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadButton
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadEventGenerator
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadTrigger
import cn.maoyanluo.ui_library.CircleTextButton
import cn.maoyanluo.ui_library.Joystick
import cn.maoyanluo.ui_library.SquareTextButton
import cn.maoyanluo.ui_library.findActivity

private fun invertShortAxisValue(value: Int): Short {
    return if (value == Short.MIN_VALUE.toInt()) {
        Short.MAX_VALUE
    } else {
        (-value).toShort()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun ConnectingPage(
    modifier: Modifier,
    uiData: ConnectingPageModel,
    onUiIntent: (intent: MainUiIntent) -> Unit
) {
    val readyColor = colorResource(R.color.connection_status_ready)
    val pendingColor = colorResource(R.color.connection_status_pending)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.connection_state),
            fontSize = 30.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ConnectionStatusCard(
                title = "Bluetooth GATT",
                primaryLabel = "设备名称",
                primaryValue = uiData.deviceName,
                statusText = if (uiData.isGATTAvailable) "就绪" else "未就绪",
                statusColor = if (uiData.isGATTAvailable) readyColor else pendingColor
            )
            ConnectionStatusCard(
                title = "Bluetooth RFCOMM",
                primaryLabel = "设备名称",
                primaryValue = uiData.deviceName,
                statusText = if (uiData.rfcommStatus.isAvailable) "就绪" else "未就绪",
                statusColor = if (uiData.rfcommStatus.isAvailable) readyColor else pendingColor
            ) {
                onUiIntent(MainUiIntent.OnRequestRttIntent(ConnectionType.BLE))
            }

            ConnectionStatusCard(
                title = "TCP",
                primaryLabel = "对端地址",
                primaryValue = uiData.tcpStatus.info,
                statusText = if (uiData.tcpStatus.isAvailable) "就绪" else "未就绪",
                statusColor = if (uiData.tcpStatus.isAvailable) readyColor else pendingColor
            ) {
                onUiIntent(MainUiIntent.OnRequestRttIntent(ConnectionType.TCP))
            }

            ConnectionStatusCard(
                title = "UDP",
                primaryLabel = "对端地址",
                primaryValue = uiData.udpStatus.info,
                statusText = if (uiData.udpStatus.isAvailable) "就绪" else "未就绪",
                statusColor = if (uiData.udpStatus.isAvailable) readyColor else pendingColor
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = { onUiIntent(MainUiIntent.OnSelectConnectTypeIntent(ConnectionType.BLE)) },
                enabled = uiData.rfcommStatus.isAvailable,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (uiData.rfcommStatus.isSelect)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("BLE")
            }

            Button(
                onClick = { onUiIntent(MainUiIntent.OnSelectConnectTypeIntent(ConnectionType.TCP)) },
                enabled = uiData.tcpStatus.isAvailable,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (uiData.tcpStatus.isSelect)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("TCP")
            }

            Button(
                onClick = { onUiIntent(MainUiIntent.OnSelectConnectTypeIntent(ConnectionType.UDP)) },
                enabled = uiData.udpStatus.isAvailable,
                colors = ButtonDefaults.buttonColors(
                    containerColor =
                        if (uiData.udpStatus.isSelect)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("UDP")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onUiIntent(MainUiIntent.OnEnterGamepadIntent) },
            enabled = uiData.isAvailable,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "->")
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    title: String,
    primaryLabel: String,
    primaryValue: String,
    statusText: String,
    statusColor: Color,
    onClick: (() -> Unit)? = null
) {
    val cardBackgroundColor = colorResource(R.color.connection_card_background)
    val primaryTextColor = colorResource(R.color.connection_text_primary)
    val secondaryTextColor = colorResource(R.color.connection_text_secondary)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick?.invoke()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = primaryTextColor, fontSize = 20.sp)
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = primaryLabel, color = secondaryTextColor)
                Text(text = primaryValue, color = primaryTextColor, fontSize = 18.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GamepadPage(modifier: Modifier, generator: GamepadEventGenerator, receiver: (ByteArray) -> Unit) {
    val activity = LocalContext.current.findActivity() as? MainActivity
    var lbPressed by remember { mutableStateOf(false) }
    var rbPressed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        generator.startCollection(receiver)
        val keyEventHandler: (KeyEvent) -> Boolean = { event ->
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (event.repeatCount == 0) {
                                lbPressed = true
                                generator.setButton(GamepadButton.LB, true)
                            }
                            true
                        }
                        KeyEvent.ACTION_UP -> {
                            lbPressed = false
                            generator.setButton(GamepadButton.LB, false)
                            true
                        }
                        else -> false
                    }
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (event.repeatCount == 0) {
                                rbPressed = true
                                generator.setButton(GamepadButton.RB, true)
                            }
                            true
                        }
                        KeyEvent.ACTION_UP -> {
                            rbPressed = false
                            generator.setButton(GamepadButton.RB, false)
                            true
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }
        activity?.hardwareKeyEventHandler = keyEventHandler
        onDispose {
            if (activity?.hardwareKeyEventHandler === keyEventHandler) {
                activity.hardwareKeyEventHandler = null
            }
            lbPressed = false
            rbPressed = false
            generator.setButton(GamepadButton.LB, false)
            generator.setButton(GamepadButton.RB, false)
            generator.stopCollection()
        }
    }
    Column(modifier
        .fillMaxSize()
        .padding(0.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(50.dp, 0.dp, 0.dp, 0.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                LTLBButtons(
                    modifier = Modifier.height(50.dp),
                    fontSize = 20.sp,
                    lbPressed = lbPressed,
                    onTriggerChanged = { value ->
                        generator.setTrigger(GamepadTrigger.LeftTrigger, value.toUByte())
                    },
                    onKeyEvent = { btn, on ->
                        generator.setButton(btn, on)
                    }
                )
            }
            Box(
                modifier = Modifier
                    .padding(0.dp, 0.dp, 50.dp, 0.dp)
                    .weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                RBRTButtons(
                    modifier = Modifier.height(50.dp),
                    fontSize = 20.sp,
                    rbPressed = rbPressed,
                    onTriggerChanged = { value ->
                        generator.setTrigger(GamepadTrigger.RightTrigger, value.toUByte())
                    },
                    onKeyEvent = { btn, on ->
                        generator.setButton(btn, on)
                    }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .padding(50.dp, 0.dp, 0.dp, 0.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Joystick(
                            modifier = Modifier.size(180.dp),
                            onStickPress = {
                                generator.setButton(GamepadButton.LS, true)
                            },
                            onStickRelease = {
                                generator.setButton(GamepadButton.LS, false)
                            },
                            onAxisChanged = { x, y ->
                                generator.setAxis(GamepadAxis.LeftX, x.toShort())
                                generator.setAxis(GamepadAxis.LeftY, invertShortAxisValue(y))
                            },
                            xMinValue = -32768, xMaxValue = 32767, xInitialValue = 0,
                            yMinValue = -32768, yMaxValue = 32767, yInitialValue = 0
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 50.dp, 0.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        DPadButtons(modifier = Modifier.size(180.dp), fontSize = 30.sp) { btn, on ->
                            generator.setButton(btn, on)
                        }
                    }

                }

                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .padding(50.dp, 0.dp, 0.dp, 0.dp)
                            .align(Alignment.BottomStart)
                    ) {
                        Joystick(
                            modifier = Modifier.size(180.dp),
                            onStickPress = {
                                generator.setButton(GamepadButton.RS, true)
                            },
                            onStickRelease = {
                                generator.setButton(GamepadButton.RS, false)
                            },
                            onAxisChanged = { x, y ->
                                generator.setAxis(GamepadAxis.RightX, x.toShort())
                                generator.setAxis(GamepadAxis.RightY, invertShortAxisValue(y))
                            },
                            xMinValue = -32768, xMaxValue = 32767, xInitialValue = 0,
                            yMinValue = -32768, yMaxValue = 32767, yInitialValue = 0
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(0.dp, 0.dp, 50.dp, 0.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        ActionButtons(
                            modifier = Modifier.size(180.dp),
                            fontSize = 30.sp
                        ) { btn, on ->
                            generator.setButton(btn, on)
                        }
                    }

                }
            }
            Box(modifier = Modifier
                .align(Alignment.TopCenter)
                .height(180.dp)
                .width(180.dp)) {
                CircleTextButton(
                    text = "G",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.TopCenter),
                    onDown = {
                        generator.setButton(GamepadButton.Guide, true)
                    },
                    onUp = {
                        generator.setButton(GamepadButton.Guide, false)
                    }
                )
                SquareTextButton(
                    text = "BACK",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterStart),
                    onDown = {
                        generator.setButton(GamepadButton.Back, true)
                    },
                    onUp = {
                        generator.setButton(GamepadButton.Back, false)
                    }
                )
                CircleTextButton(
                    text = "FN",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.BottomCenter),
                    onDown = {
                        generator.setButton(GamepadButton.Function, true)
                    },
                    onUp = {
                        generator.setButton(GamepadButton.Function, false)
                    }
                )
                SquareTextButton(
                    text = "START",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterEnd),
                    onDown = {
                        generator.setButton(GamepadButton.Start, true)
                    },
                    onUp = {
                        generator.setButton(GamepadButton.Start, false)
                    })
            }
        }
    }
}
