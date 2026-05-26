package cn.maoyanluo.ui_library.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.maoyanluo.ui_library.R


@Composable
@SuppressLint("MissingPermission")
fun SelectDevicePages(
    modifier: Modifier = Modifier,
    data: List<BluetoothDevice>,
    onBluetoothDeviceSelected: ((BluetoothDevice) -> Unit),
    onClick: (() -> Unit)? = null) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable {
                onClick?.invoke()
            },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.pair_devices),
            fontSize = 30.sp,
            modifier = Modifier.padding(0.dp, 10.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(data) { device ->
                val deviceName = device.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.unknown_device)
                val deviceAddress = device.address?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.unknown_address)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBluetoothDeviceSelected(device) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.device_list_card_background)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = deviceName,
                            color = colorResource(R.color.device_list_text_primary),
                            fontSize = 20.sp
                        )
                        Text(
                            text = deviceAddress,
                            color = colorResource(R.color.device_list_text_secondary),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
