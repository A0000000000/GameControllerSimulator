package cn.maoyanluo.gamecontrollersimulator2.connect

import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity

interface ConnectionCallback {

    fun onManagerAvailableChange(available: Boolean)
    fun onConnectionAvailable(available: Boolean, type: ConnectionManager.ConnectionType)

    fun getTypeClass(type: Int): Class<*>
    fun onDataReady(data: BaseEntity<*>?)

    fun onSendDataException(
        e: Exception,
        id: Int = -1,
        connectionType: ConnectionManager.ConnectionType
    )
    fun onDataRevException(e: Exception, connectionType: ConnectionManager.ConnectionType)

}