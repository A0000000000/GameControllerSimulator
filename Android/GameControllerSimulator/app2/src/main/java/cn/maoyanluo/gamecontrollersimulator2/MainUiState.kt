package cn.maoyanluo.gamecontrollersimulator2

import cn.maoyanluo.gamecontrollersimulator2.connect.ConnectionType

data class ConnectStatus(
    val isAvailable: Boolean = false,
    val isSelect: Boolean = false,
    val info: String = "",
)

sealed interface MainUiState {
    data object NoPermissionPage : MainUiState
    data object SelectPage : MainUiState
    data class ConnectingPage(
        val deviceName: String,
        val isGATTAvailable: Boolean = false,
        val isAvailable: Boolean = false,
        val rfcommStatus: ConnectStatus = ConnectStatus(),
        val tcpStatus: ConnectStatus = ConnectStatus(),
        val udpStatus: ConnectStatus = ConnectStatus()
    ) : MainUiState {
        companion object {
            fun map(
                deviceName: String,
                gattAvailable: Boolean,
                sessionAvailable: Boolean,
                selectedType: ConnectionType,
                rfcommInfo: String,
                tcpInfo: String,
                udpInfo: String,
                isBleAvailable: Boolean,
                isTcpAvailable: Boolean,
                isUdpAvailable: Boolean
            ) = ConnectingPage(
                deviceName = deviceName,
                isGATTAvailable = gattAvailable,
                isAvailable = sessionAvailable,
                rfcommStatus = ConnectStatus(
                    isAvailable = isBleAvailable,
                    isSelect = selectedType == ConnectionType.BLE,
                    info = rfcommInfo
                ),
                tcpStatus = ConnectStatus(
                    isAvailable = isTcpAvailable,
                    isSelect = selectedType == ConnectionType.TCP,
                    info = tcpInfo
                ),
                udpStatus = ConnectStatus(
                    isAvailable = isUdpAvailable,
                    isSelect = selectedType == ConnectionType.UDP,
                    info = udpInfo
                )
            )
        }
    }
    data object GamepadPage : MainUiState
}
