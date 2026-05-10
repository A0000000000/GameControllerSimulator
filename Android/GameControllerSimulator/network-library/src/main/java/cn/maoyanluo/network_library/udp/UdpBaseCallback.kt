package cn.maoyanluo.network_library.udp

interface UdpBaseCallback {
    fun onStart()
    fun onStop()
    fun onStartException(e: Exception)
    fun onDataReceiveException(e: Exception)
    fun onDataReceive(data: ByteArray, dataSender: ((ByteArray, Int) -> Unit))
    fun onDataSendException(e: Exception, id: Int)
}