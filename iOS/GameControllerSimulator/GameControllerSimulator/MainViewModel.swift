//
//  MainViewModel.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import SwiftUI
import CoreBluetooth
import BluetoothKit
import LogKit

@MainActor
@Observable
class MainViewModel {
    
    public static let tag = "MainViewModel"
    
    @ObservationIgnored private var bluetoothManager: BluetoothManager
    @ObservationIgnored private var delegateProxy: MainViewModelDelegateProxy
    
    var availablePeripherals: [CBPeripheral] = []
    
    var currentPage = PageEnum.SelectPage
    
    init() {
        bluetoothManager = BluetoothManager()
        delegateProxy = MainViewModelDelegateProxy()
        delegateProxy.viewModel = self
        bluetoothManager.delegate = delegateProxy
    }
    
    func initAvailableBluetoothList() {
        bluetoothManager.initCBCentralManager()
    }
    
    fileprivate func showErrorMsg(msg: String) {
        Log.e(Self.tag, msg)
    }
    
    fileprivate func onNewPeripheralDiscover(peripheral: CBPeripheral) {
        availablePeripherals.append(peripheral)
    }
    
}

private class MainViewModelDelegateProxy: BluetoothManagerDelegate {
    
    weak var viewModel: MainViewModel?
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    ) {
        if (state == CBManagerState.poweredOn) {
            manager.scan(serviceUUID: UUIDConstant.shared.GATT_FUN_UUID)
        } else {
            Task { @MainActor in
                viewModel?.showErrorMsg(msg: "蓝牙不可用，当期状态：\(state)。")
            }
        }
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscover peripheral: CBPeripheral
    ) {
        Task { @MainActor in
            viewModel?.onNewPeripheralDiscover(peripheral: peripheral)
        }
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
