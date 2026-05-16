package cn.maoyanluo.gamecontrollersimulator2.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@Composable
fun ConnectingPage(
    modifier: Modifier,
    device: BluetoothDevice,
    isAvailable: Boolean,
    onOpenGamepad: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = modifier
                .padding(10.dp, 0.dp)
                .fillMaxWidth()
                .background(color = Color.Gray),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "类型: 蓝牙")
            Text(text = "名称: ${device.name}")
            Text(text = "状态: ${if (isAvailable) "就绪" else "未就绪"}")
        }

        Button(onClick = onOpenGamepad, enabled = isAvailable) {
            Text(text = "->")
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GamepadPage(modifier: Modifier, device: BluetoothDevice) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "已进入手柄页: ${device.name}")
    }
}
