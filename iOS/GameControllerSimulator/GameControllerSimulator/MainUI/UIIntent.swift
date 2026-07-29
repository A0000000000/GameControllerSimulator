//
//  UIIntent.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/28.
//

import CoreBluetooth
import NetworkKit

enum UIIntent {
    case InitAvailableBluetooth
    case PeripheralSelected(CBPeripheral)
    case BackSelectPage
    case ConnectionTypeChange(ConnectionType)
    case OnRequestRtt(ConnectionType)
    case OpenGamepadPage
}
