//
//  IntConverter.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/29.
//

import Foundation

public enum IntConverter {

    /// Int 转大端字节序
    public static func toBigEndian(_ value: Int) -> [UInt8] {
        return [
            UInt8((value >> 24) & 0xFF),
            UInt8((value >> 16) & 0xFF),
            UInt8((value >> 8) & 0xFF),
            UInt8(value & 0xFF)
        ]
    }


    /// Int 转小端字节序
    public static func toLittleEndian(_ value: Int) -> [UInt8] {
        return [
            UInt8(value & 0xFF),
            UInt8((value >> 8) & 0xFF),
            UInt8((value >> 16) & 0xFF),
            UInt8((value >> 24) & 0xFF)
        ]
    }


    /// 大端字节序转 Int
    public static func fromBigEndian(_ bytes: [UInt8]) -> Int {
        precondition(bytes.count >= 4, "Byte array size must be at least 4")

        return (Int(bytes[0]) << 24) |
               (Int(bytes[1]) << 16) |
               (Int(bytes[2]) << 8) |
                Int(bytes[3])
    }


    /// 小端字节序转 Int
    public static func fromLittleEndian(_ bytes: [UInt8]) -> Int {
        precondition(bytes.count >= 4, "Byte array size must be at least 4")

        return Int(bytes[0]) |
               (Int(bytes[1]) << 8) |
               (Int(bytes[2]) << 16) |
               (Int(bytes[3]) << 24)
    }
}
