package cn.maoyanluo.bluetooth_library.socket

interface BluetoothServerCallback {

    fun onStartServerSuccess()
    fun onStartServerFailed(e: Exception)
    fun onStopServer()

    fun onStopForeverException(e: Exception)

    fun createNewClientCallback(): ClientCallback
    fun onNewClientConnect(client: BluetoothSocketServer.Client)
    fun onNewClientException(e: Exception)


    interface ClientCallback {
        fun onSendDataException(e: Exception, id: Int = -1)
        fun onDisconnect()
        fun onDataReady(data: ByteArray)
        fun onDataRevException(e: Exception)
    }


}