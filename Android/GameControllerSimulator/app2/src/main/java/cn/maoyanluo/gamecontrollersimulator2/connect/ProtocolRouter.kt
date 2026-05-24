package cn.maoyanluo.gamecontrollersimulator2.connect

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.gamecontrollersimulator2.bean.BaseEntity
import cn.maoyanluo.gamecontrollersimulator2.constant.EntityType
import cn.maoyanluo.log_library.LogUtils
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch


data class RouterHandler(
    val id: Int,
    val type: Int,
    val handler: (data: BaseEntity<*>, type: ConnectionType) -> Unit
)

class ProtocolRouter(
    private val coroutineManager: CoroutineManager
) {
    companion object {
        const val TAG = "ProtocolRouter"
    }
    private val gson = Gson()
    private val handlers = HashMap<Int, HashMap<Int, RouterHandler>>()

    fun encode(entity: BaseEntity<*>): ByteArray {
        LogUtils.i(TAG, "encode")
        return gson.toJson(entity).toByteArray()
    }

    fun decode(data: ByteArray): BaseEntity<*> {
        LogUtils.i(TAG, "decode")
        val jsonStr = String(data)
        val jsonObject = gson.fromJson(jsonStr, JsonObject::class.java)
        val type = jsonObject.get("type")?.asInt ?: -1
        val typeTokenType = if (type == -1) {
            TypeToken.getParameterized(BaseEntity::class.java, JsonElement::class.java).type
        } else {
            TypeToken.getParameterized(BaseEntity::class.java, EntityType.TYPE_MAPPING[type] ?: JsonElement::class.java).type
        }
        return gson.fromJson(jsonStr, typeTokenType)
    }

    @Synchronized
    fun registerHandler(registerHandlers: List<RouterHandler>) {
        LogUtils.i(TAG, "registerHandler count = ${registerHandlers.size}")
        for (handler in registerHandlers) {
            if (!handlers.contains(handler.id) || handlers[handler.id] == null) {
                handlers[handler.id] = HashMap()
            }
            handlers[handler.id]!![handler.type] = handler
        }
    }

    @Synchronized
    fun unregisterHandler(unregisterHandlers: List<RouterHandler>) {
        LogUtils.i(TAG, "unregisterHandler count = ${unregisterHandlers.size}")
        for (handler in unregisterHandlers) {
            if (handlers.contains(handler.id) && handlers[handler.id] != null) {
                handlers[handler.id]?.remove(handler.type)
                if (handlers[handler.id]?.isEmpty() == true) {
                    handlers.remove(handler.id)
                }
            }
        }
    }

    @Synchronized
    fun clearHandlers(id: Int) {
        LogUtils.i(TAG, "clearHandlers id = $id")
        handlers.remove(id)
    }

    @Synchronized
    fun clearHandlers() {
        LogUtils.i(TAG, "clearHandlers")
        handlers.clear()
    }

    fun dispatcherData(data: ByteArray, type: ConnectionType) {
        LogUtils.i(TAG, "dispatcherData")
        coroutineManager.getIOScope().launch {
            val entity = decode(data)
            handlers[entity.id]?.get(entity.type)?.also {
                LogUtils.i(TAG, "dispatcherData found id = ${entity.id}, type = ${entity.type}")
                it.handler(entity, type)
            } ?: run {
                LogUtils.i(TAG, "dispatcherData not found id = ${entity.id}, type = ${entity.type}")
            }
        }
    }

}