package cn.maoyanluo.gamecontrollersimulator2.generator

import cn.maoyanluo.coroutine_library.CoroutineManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class GamepadEventGenerator(private val coroutineManager: CoroutineManager) {

    companion object {
        private const val MAX_SAME_NOT_SEND_TIME = 60
    }

    private val gamepadStatePacket = GamepadStatePacket()

    fun setButton(button: GamepadButton, pressed: Boolean) = gamepadStatePacket.setButton(button, pressed)
    fun setAxis(axis: GamepadAxis, value: Short) = gamepadStatePacket.setAxis(axis, value)
    fun setTrigger(trigger: GamepadTrigger, value: UByte) = gamepadStatePacket.setTrigger(trigger, value)

    @Volatile
    private var isStart = false

    fun startCollection(receiver: (ByteArray) -> Unit) {
        if (isStart) return
        isStart = true
        coroutineManager.getDefaultScope().launch {
            var sameCount = 0
            while (isStart) {
                if (gamepadStatePacket.isChange()) {
                    gamepadStatePacket.copyCurrentToLast()
                    receiver(gamepadStatePacket.current)
                    sameCount = 0
                } else {
                    sameCount++
                    if (sameCount == MAX_SAME_NOT_SEND_TIME) {
                        gamepadStatePacket.copyCurrentToLast()
                        receiver(gamepadStatePacket.current)
                        sameCount = 0
                    }
                }
                delay(16)
            }
        }
    }

    fun stopCollection() {
        isStart = false
    }

}