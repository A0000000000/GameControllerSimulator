package cn.maoyanluo.hid_library

import cn.maoyanluo.coroutine_library.CoroutineManager
import cn.maoyanluo.game_event_common_library.Axis
import cn.maoyanluo.game_event_common_library.Button
import cn.maoyanluo.game_event_common_library.DPad
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile

class GameControllerHIDReportGenerator(private val coroutineManager: CoroutineManager) {

    companion object {
        private const val MAX_SAME_NOT_SEND_TIME = 60
    }


    private val currentReportStatus = GameControllerHID.RESET_REPORT.clone()

    @Volatile
    private var isStart = false

    private var lastSend: ByteArray = GameControllerHID.RESET_REPORT.clone()
    private var sameCount = 0

    private object Offset {
        const val BTN_LOW = 0
        const val BTN_HIGH = 1
        const val HAT = 2
        const val LX = 3
        const val LY = 4
        const val RX = 5
        const val RY = 6
    }

    fun setAxis(axis: Axis, value: Int) {
        val v = value.coerceIn(0, 255).toByte()
        when (axis) {
            Axis.X -> currentReportStatus[Offset.LX] = v
            Axis.Y -> currentReportStatus[Offset.LY] = v
            Axis.RX -> currentReportStatus[Offset.RX] = v
            Axis.RY -> currentReportStatus[Offset.RY] = v
        }
    }

    fun setButton(button: Button, on: Boolean) {
        val byteOffset = if (button.bitIndex < 8) Offset.BTN_LOW else Offset.BTN_HIGH
        val bitMask = (1 shl (button.bitIndex % 8))

        val currentByte = currentReportStatus[byteOffset].toInt() and 0xFF
        currentReportStatus[byteOffset] = if (on) {
            (currentByte or bitMask).toByte()
        } else {
            (currentByte and bitMask.inv()).toByte()
        }
    }

    fun setDPad(dpad: DPad) {
        currentReportStatus[Offset.HAT] = dpad.hatValue.toByte()
    }

    fun resetReport() {
        System.arraycopy(GameControllerHID.RESET_REPORT, 0, currentReportStatus, 0, currentReportStatus.size)
    }

    fun startCollection(receiver: (ByteArray) -> Unit) {
        if (isStart) return
        isStart = true
        coroutineManager.getDefaultScope().launch {
            while (isStart) {
                val send = currentReportStatus.clone()
                when (lastSend.contentEquals(send)) {
                    true -> {
                        sameCount++
                        if (sameCount == MAX_SAME_NOT_SEND_TIME) {
                            sameCount = 0
                            receiver(send)
                        }
                    }
                    else -> {
                        sameCount = 0
                        receiver(send)
                        lastSend = send
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