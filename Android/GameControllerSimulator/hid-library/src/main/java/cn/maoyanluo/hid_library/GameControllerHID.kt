package cn.maoyanluo.hid_library

class GameControllerHID {

    companion object {
        fun b(v: Int) = v.toByte()

        val HID_REPORT_DESCRIPTOR = byteArrayOf(
            b(0x05), b(0x01),        // Usage Page (Generic Desktop)
            b(0x09), b(0x05),        // Usage (Game Pad)
            b(0xA1), b(0x01),        // Collection (Application)

            // ---------- 16 个数字按钮 (Byte 0, 1) ----------
            b(0x05), b(0x09),        //   Usage Page (Button)
            b(0x19), b(0x01),        //   Usage Minimum (Button 1)
            b(0x29), b(0x10),        //   Usage Maximum (Button 16)
            b(0x15), b(0x00),        //   Logical Minimum (0)
            b(0x25), b(0x01),        //   Logical Maximum (1)
            b(0x75), b(0x01),        //   Report Size (1 bit)
            b(0x95), b(0x10),        //   Report Count (16)
            b(0x81), b(0x02),        //   Input (Data, Var, Abs)

            // ---------- 方向键 Hat Switch (Byte 2) ----------
            b(0x05), b(0x01),        //   Usage Page (Generic Desktop)
            b(0x09), b(0x39),        //   Usage (Hat Switch)
            b(0x15), b(0x00),        //   Logical Minimum (0)
            b(0x25), b(0x07),        //   Logical Maximum (7)
            b(0x75), b(0x08),        //   Report Size (8 bits)
            b(0x95), b(0x01),        //   Report Count (1)
            b(0x81), b(0x42),        //   Input (Data, Var, Abs, Null)

            // ---------- 左摇杆 X / Y ----------
            b(0x09), b(0x30),        //   Usage (X)
            b(0x09), b(0x31),        //   Usage (Y)
            b(0x16), b(0x00), b(0x00),  //   Logical Minimum (0)
            b(0x26), b(0xFF), b(0x00),  //   Logical Maximum (255)
            b(0x75), b(0x08),        //   Report Size (8 bits)
            b(0x95), b(0x02),        //   Report Count (2)
            b(0x81), b(0x02),        //   Input (Data, Var, Abs)
            // ---------- 右摇杆 X / Y ----------
            b(0x09), b(0x32),        //   Usage (Rx)
            b(0x09), b(0x33),        //   Usage (Ry)
            b(0x16), b(0x00), b(0x00),  //   Logical Minimum (0)
            b(0x26), b(0xFF), b(0x00),  //   Logical Maximum (255)
            b(0x75), b(0x08),        //   Report Size (8 bits)
            b(0x95), b(0x02),        //   Report Count (2)
            b(0x81), b(0x02),        //   Input (Data, Var, Abs)
            b(0xC0)                  // End Collection
        )

        // 重置报文：按钮0，Hat 8，轴128
        val RESET_REPORT = byteArrayOf(
            b(0x00), b(0x00), // Byte 0-1: Buttons
            b(0x08),         // Byte 2: Hat
            b(0x80), b(0x80), // Byte 3-4: LX, LY
            b(0x80), b(0x80),  // Byte 5-6: RX, RY
        )
    }
}