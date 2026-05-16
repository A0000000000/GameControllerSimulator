package cn.maoyanluo.gamecontrollersimulator2.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.maoyanluo.gamecontrollersimulator2.MainUiState
import cn.maoyanluo.gamecontrollersimulator2.MainViewModel
import cn.maoyanluo.gamecontrollersimulator2.R

@SuppressLint("MissingPermission")
@Composable
fun ConnectingPage(modifier: Modifier) {
    val viewModel: MainViewModel = viewModel()
    val uiState = viewModel.mainUiState

    if (uiState is MainUiState.ConnectingPage) {
        Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(modifier = modifier
                .padding(10.dp, 0.dp)
                .fillMaxWidth()
                .background(color = Color.Gray),
                horizontalAlignment = Alignment.CenterHorizontally) {

                LaunchedEffect(Unit) {
                    viewModel.initConnectionManager(uiState.device)
                }

                Text(text = "类型: 蓝牙")
                Text(text = "名称: ${uiState.device.name}")
                Text(text = "状态: ${if (viewModel.isAvailable) "就绪" else "未就绪"}")
            }

            Button(onClick = {
                viewModel.openGamepadPage()
            }, enabled = viewModel.isAvailable) {
                Text(text = "->")
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.error_page))
        }
    }
}

@Composable
fun GamepadPage(modifier: Modifier) {
    val viewModel: MainViewModel = viewModel()
    val uiState = viewModel.mainUiState
    if (uiState is MainUiState.GamepadPage) {

    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.error_page))
        }
    }
}