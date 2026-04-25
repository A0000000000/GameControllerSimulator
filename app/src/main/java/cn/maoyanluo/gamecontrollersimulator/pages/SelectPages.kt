package cn.maoyanluo.gamecontrollersimulator.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import cn.maoyanluo.bluetooth_library.BluetoothSelectManager
import cn.maoyanluo.gamecontrollersimulator.MainViewModel
import cn.maoyanluo.gamecontrollersimulator.R


@Composable
fun SelectDevicePages(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val mainViewModel: MainViewModel = viewModel()
    val bluetoothSelectManager = remember { BluetoothSelectManager(ctx) }
    var devicesList by remember {
        mutableStateOf(bluetoothSelectManager.getBondedDevice())
    }
    Column(modifier = modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.pair_devices),
            fontSize = 30.sp,
            modifier = Modifier.padding(0.dp, 10.dp).clickable {
                Toast.makeText(ctx, R.string.start_update, Toast.LENGTH_SHORT).show()
                devicesList = bluetoothSelectManager.getBondedDevice()
                Toast.makeText(ctx, R.string.update_finish, Toast.LENGTH_SHORT).show()
            }
        )
        LazyColumn(modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            for (device in devicesList) {
                item {
                    Text(text = device.name, fontSize = 20.sp, modifier = Modifier.padding(0.dp, 3.dp).clickable {
                        mainViewModel.selectDevice = device
                    })
                }
            }
        }
    }
}