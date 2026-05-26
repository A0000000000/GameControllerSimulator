package cn.maoyanluo.ui_library.pages

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        LazyColumn(modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            for (device in data) {
                item {
                    Text(text = device.name ?: device.address ?: "", fontSize = 20.sp, modifier = Modifier.padding(0.dp, 3.dp).clickable {
                        onBluetoothDeviceSelected(device)
                    })
                }
            }
        }
    }
}