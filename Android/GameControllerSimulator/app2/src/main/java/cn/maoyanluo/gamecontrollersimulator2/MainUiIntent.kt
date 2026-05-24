package cn.maoyanluo.gamecontrollersimulator2

sealed interface MainUiIntent {

    object NoPermissionIntent: MainUiIntent
    object ToSelectPageIntent: MainUiIntent
    data class ToConnectingPageIntent(val data: ConnectionViewData): MainUiIntent {
        data class ConnectionViewData(var deviceName: String)
    }
    object ToGamepadPageIntent: MainUiIntent

}