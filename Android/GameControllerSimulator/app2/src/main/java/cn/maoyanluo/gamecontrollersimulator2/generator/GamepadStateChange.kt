package cn.maoyanluo.gamecontrollersimulator2.generator

data class GamepadStateChange(
    val type: GamepadChangeType,

    val button: GamepadButton? = null,
    val buttonPressed: Boolean = false,

    val axis: GamepadAxis? = null,
    val axisValue: Short = 0,

    val trigger: GamepadTrigger? = null,
    val triggerValue: UByte = 0u
)