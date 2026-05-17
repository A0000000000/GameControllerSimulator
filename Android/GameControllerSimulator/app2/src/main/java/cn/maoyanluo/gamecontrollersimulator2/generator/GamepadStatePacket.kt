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

    fun getButton(button: GamepadButton): Boolean {
        val buttons = readUInt16(current, BUTTON_OFFSET)
        val mask = (1 shl button.ordinal)
        return (buttons and mask) != 0
    }

    fun setAxis(axis: GamepadAxis, value: Short) {
        val offset = AXIS_OFFSET + axis.ordinal * 2
        writeInt16(current, offset, value)
    }

    fun getAxis(axis: GamepadAxis): Short {
        val offset = AXIS_OFFSET + axis.ordinal * 2
        return readInt16(current, offset)
    }

    fun setTrigger(trigger: GamepadTrigger, value: UByte) {
        val offset = TRIGGER_OFFSET + trigger.ordinal
        current[offset] = value.toByte()
    }

    fun getTrigger(trigger: GamepadTrigger): UByte {
        val offset = TRIGGER_OFFSET + trigger.ordinal
        return current[offset].toUByte()
    }

    fun getChanges(): List<GamepadStateChange> {
        val changes = mutableListOf<GamepadStateChange>()

        val currentButtons = readUInt16(current, BUTTON_OFFSET)
        val lastButtons = readUInt16(last, BUTTON_OFFSET)

        val buttonDiff = currentButtons xor lastButtons

        for (i in 0 until 16) {
            val mask = (1 shl i)

            if ((buttonDiff and mask) != 0) {
                val pressed = (currentButtons and mask) != 0

                changes.add(
                    GamepadStateChange(
                        type = GamepadChangeType.Button,
                        button = GamepadButton.entries[i],
                        buttonPressed = pressed
                    )
                )
            }
        }

        GamepadAxis.entries.forEach { axis ->
            val currentValue = getAxisFrom(current, axis)
            val lastValue = getAxisFrom(last, axis)

            if (currentValue != lastValue) {
                changes.add(
                    GamepadStateChange(
                        type = GamepadChangeType.Axis,
                        axis = axis,
                        axisValue = currentValue
                    )
                )
            }
        }

        GamepadTrigger.entries.forEach { trigger ->
            val currentValue = getTriggerFrom(current, trigger)
            val lastValue = getTriggerFrom(last, trigger)

            if (currentValue != lastValue) {
                changes.add(
                    GamepadStateChange(
                        type = GamepadChangeType.Trigger,
                        trigger = trigger,
                        triggerValue = currentValue
                    )
                )
            }
        }

        return changes
    }

    private fun getAxisFrom(data: ByteArray, axis: GamepadAxis): Short {
        val offset = AXIS_OFFSET + axis.ordinal * 2
        return readInt16(data, offset)
    }

    private fun getTriggerFrom(data: ByteArray, trigger: GamepadTrigger): UByte {
        val offset = TRIGGER_OFFSET + trigger.ordinal
        return data[offset].toUByte()
    }

    private fun readUInt16(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readInt16(data: ByteArray, offset: Int): Short {
        return ByteBuffer
            .wrap(data, offset, 2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .short
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