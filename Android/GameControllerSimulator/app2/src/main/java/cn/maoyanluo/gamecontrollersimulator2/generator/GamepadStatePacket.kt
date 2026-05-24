package cn.maoyanluo.gamecontrollersimulator2.generator

import java.nio.ByteBuffer
import java.nio.ByteOrder

class GamepadStatePacket {

    val current = ByteArray(12)
    val last = ByteArray(12)

    init {
        initializeState(current)
        initializeState(last)
    }

    companion object {
        private const val BUTTON_OFFSET = 0
        private const val AXIS_OFFSET = 2
        private const val TRIGGER_OFFSET = 10
    }

    fun reset() {
        initializeState(current)
        initializeState(last)
    }

    fun isChange() = !current.contentEquals(last)
    fun copyCurrentToLast() = current.copyInto(last)

    fun setButton(button: GamepadButton, pressed: Boolean) {
        var buttons = readUInt16(current, BUTTON_OFFSET)
        val mask = (1 shl button.ordinal)
        buttons = if (pressed) {
            buttons or mask
        } else {
            buttons and mask.inv()
        }
        writeUInt16(current, BUTTON_OFFSET, buttons)
    }

    fun setAxis(axis: GamepadAxis, value: Short) {
        val offset = AXIS_OFFSET + axis.ordinal * 2
        writeInt16(current, offset, value)
    }

    fun setTrigger(trigger: GamepadTrigger, value: UByte) {
        val offset = TRIGGER_OFFSET + trigger.ordinal
        current[offset] = value.toByte()
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun writeUInt16(data: ByteArray, offset: Int, value: Int) {
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeInt16(data: ByteArray, offset: Int, value: Short) {
        val buffer = ByteBuffer
            .allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(value)

        data[offset] = buffer.array()[0]
        data[offset + 1] = buffer.array()[1]
    }

    private fun initializeState(data: ByteArray) {
        writeUInt16(data, BUTTON_OFFSET, 0)

        writeInt16(data, 2, 0)
        writeInt16(data, 4, 0)
        writeInt16(data, 6, 0)
        writeInt16(data, 8, 0)

        data[10] = 0
        data[11] = 0
    }
}