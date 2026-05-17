package cn.maoyanluo.gamecontrollersimulator2.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.R
import cn.maoyanluo.gamecontrollersimulator2.generator.GamepadEventGenerator

@SuppressLint("MissingPermission")
@Composable
fun ConnectingPage(
    modifier: Modifier,
    device: BluetoothDevice,
    isAvailable: Boolean,
    onReInitConnection: () -> Unit,
    onOpenGamepad: () -> Unit
) {
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
                title = "蓝牙",
                primaryLabel = "设备名称",
                primaryValue = device.name ?: "未知设备",
                statusText = if (isAvailable) "就绪" else "未就绪",
                statusColor = if (isAvailable) Color(0xFF2E7D32) else Color(0xFF8A5A00)
            ) {
                onReInitConnection()
            }
            ConnectionStatusCard(
                title = "TCP",
                primaryLabel = "对端地址",
                primaryValue = "0.0.0.0:1234",
                statusText = "未就绪",
                statusColor = Color(0xFF8A5A00)
            )
            ConnectionStatusCard(
                title = "UDP",
                primaryLabel = "对端地址",
                primaryValue = "0.0.0.0:1234",
                statusText = "未就绪",
                statusColor = Color(0xFF8A5A00)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onOpenGamepad,
            enabled = isAvailable,
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
    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            onClick?.invoke()
        },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6))
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
                Text(text = title, fontSize = 20.sp)
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
                Text(text = primaryLabel, color = Color(0xFF6B7280))
                Text(text = primaryValue, fontSize = 18.sp)
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun GamepadPage(modifier: Modifier, coroutineManager: CoroutineManager, receiver: (ByteArray) -> Unit) {
    val generator = remember { GamepadEventGenerator(coroutineManager) }
    DisposableEffect(Unit) {
        generator.startCollection(receiver)
        onDispose {
            generator.stopCollection()
        }
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {


    }
}
