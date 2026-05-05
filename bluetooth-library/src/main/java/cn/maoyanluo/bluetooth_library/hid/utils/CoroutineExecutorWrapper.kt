package cn.maoyanluo.bluetooth_library.hid.utils

import cn.maoyanluo.coroutine_library.CoroutineManager
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class CoroutineExecutorWrapper(
    private val coroutineManager: CoroutineManager
) : Executor {

    override fun execute(command: Runnable) {
        coroutineManager.getDefaultScope().launch {
            command.run()
        }
    }

}