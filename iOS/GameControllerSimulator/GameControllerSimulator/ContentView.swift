//
//  ContentView.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/6/29.
//

import SwiftUI
import NetworkKit
import BluetoothKit
import CommonKit
import CoreBluetooth

class MyDelegate: BluetoothManagerDelegate {
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscover peripheral: CBPeripheral
    ) {
        
    }

    func bluetoothManager(
        _ manager: BluetoothManager,
        ready peripheral: CBPeripheral
    ) {
        print("ready")
        manager.read(characteristicUUID: UUIDConstant.shared.TCP_INFO_UUID)
        manager.read(characteristicUUID: UUIDConstant.shared.UDP_INFO_UUID)
    }

    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didReceive data: Data,
        characteristic: CBUUID
    ) {
        print("uuid = \(characteristic.uuidString), data = \(data), dataStr = \(String(data: data, encoding: .utf8))")
    }

    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    ) {
        
    }
}

struct ContentView: View {
    
    private let bluetoothManager = BluetoothManager()
    private let myDelegate = MyDelegate()
    
    var body: some View {
        VStack {
            Button {
                bluetoothManager.delegate = myDelegate
                bluetoothManager.scan(serviceUUID: UUIDConstant.shared.GATT_FUN_UUID)
            } label: {
                Text("测试")
            }

        }
        .padding()
    }
}

#Preview {
    ContentView()
}
