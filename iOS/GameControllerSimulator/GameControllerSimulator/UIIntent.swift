//
//  UIIntent.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/28.
//

import CoreBluetooth

enum UIIntent {
    case InitAvailableBluetooth
    case PeripheralSelected(CBPeripheral)
}
