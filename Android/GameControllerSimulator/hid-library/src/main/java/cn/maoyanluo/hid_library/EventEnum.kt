package cn.maoyanluo.hid_library


enum class Axis {
    X, Y, RX, RY
}

enum class Button(val bitIndex: Int) {
    A(0), B(1), X(2), Y(3), LB(4), RB(5), L2(6), R2(7),
    BACK(8), START(9), L3(10), R3(11), TOP(12), BOTTOM(13), LEFT(14), RIGHT(15)
}

enum class DPad(val hatValue: Int) {
    TOP(0), RIGHT(2), BOTTOM(4), LEFT(6), NEUTRAL(8)
}