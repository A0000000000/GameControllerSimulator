//
//  NetworkInfo.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/29.
//

import SwiftUI


struct GATTStatus {
    var isAvailble: Bool
    var name: String
}

struct NetworkInfo {
    var tcpAddress: String
    var udpAddress: String
    var tcpPort: UInt
    var udpPort: UInt
    var tcpReady: Bool
    var udpReady: Bool
}
