package cn.maoyanluo.bluetooth_library.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

class CoroutineExecutorWrapper(
    private val scope: CoroutineScope
) : Executor {

    override fun execute(command: Runnable) {
        scope.launch {
            command.run()
        }
    }

}