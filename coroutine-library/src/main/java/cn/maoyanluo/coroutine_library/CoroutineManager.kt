package cn.maoyanluo.coroutine_library

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class CoroutineManager(private val name: String = CoroutineManager::class.java.simpleName) {

    @Volatile
    private var isInit = false

    private var defaultScope: CoroutineScope? = null
    private var ioScope: CoroutineScope? = null
    private var mainScope: CoroutineScope? = null

    @Synchronized
    fun init() {
        if (isInit) return
        defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("$name-default"))
        ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("$name-io"))
        mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineName("$name-main"))
        isInit = true
    }

    @Synchronized
    fun getDefaultScope(): CoroutineScope {
        return defaultScope ?: throw IllegalStateException("CoroutineManager not init!")
    }

    @Synchronized
    fun getIOScope(): CoroutineScope {
        return ioScope ?: throw IllegalStateException("CoroutineManager not init!")
    }

    @Synchronized
    fun getMainScope(): CoroutineScope {
        return mainScope ?: throw IllegalStateException("CoroutineManager not init!")
    }

    @Synchronized
    fun destroy() {
        if (!isInit) return
        defaultScope?.cancel()
        ioScope?.cancel()
        mainScope?.cancel()
        defaultScope = null
        ioScope = null
        mainScope = null
        isInit = false
    }

}