package cn.maoyanluo.gamecontrollersimulator2.generator

enum class GamepadButton {
    A,
    B,
    X,
    Y,

    Left,
    Top,
    Right,
    Bottom,

    LB,
    RB,

    LS,
    RS,

    Start,
    Back,

    Guide,
    Function
}

enum class GamepadAxis {
    LeftX,
    LeftY,
    RightX,
    RightY
}

enum class GamepadTrigger {
    LeftTrigger,
    RightTrigger
}

enum class GamepadChangeType {
    Button,
    Axis,
    Trigger
}