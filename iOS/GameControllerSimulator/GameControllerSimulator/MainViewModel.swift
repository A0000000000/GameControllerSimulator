//
//  MainViewModel.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import SwiftUI
import CoreBluetooth
import BluetoothKit

@MainActor
@Observable
class MainViewModel {
    
    @ObservationIgnored private var bluetoothManager: BluetoothManager
    @ObservationIgnored private var delegateProxy: MainViewModelDelegateProxy
    
    init() {
        bluetoothManager = BluetoothManager()
        delegateProxy = MainViewModelDelegateProxy()
        delegateProxy.viewModel = self
        bluetoothManager.delegate = delegateProxy
    }
    
}

private class MainViewModelDelegateProxy: BluetoothManagerDelegate {
    
    weak var viewModel: MainViewModel?
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    ) {
        
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscover peripheral: CBPeripheral
    ) {
        
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didConnect peripheral: CBPeripheral
    ) {
        
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverServices peripheral: CBPeripheral,
        error: Error?,
        services: [CBService]
    ) {
        
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverCharacteristics: CBPeripheral,
        characteristic service: CBUUID,
        error: Error?
    ) {
        
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didReceive: CBPeripheral,
        characteristic: CBUUID,
        data: Data,
        error: Error?
    ) {
        
    }
    
}
