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
    
    var currentPage = UIState.SelectPage
    
    var availablePeripherals: [CBPeripheral] = []
    
    var tcpInfo = ""
    var udpInfo = ""
    
    init() {
        bluetoothManager = BluetoothManager()
        delegateProxy = MainViewModelDelegateProxy()
        delegateProxy.viewModel = self
        bluetoothManager.delegate = delegateProxy
    }
    
    func onUIIntent(intent: UIIntent) {
        Log.d(Self.tag, "onUIIntent is \(intent)")
        switch intent {
        case UIIntent.InitAvailableBluetooth:
            bluetoothManager.initCBCentralManager()
        case UIIntent.PeripheralSelected(let peripheral):
            currentPage = UIState.ConnectingPage
            bluetoothManager.connect(peripheral)
        }
    }
    
    fileprivate func showErrorMsg(msg: String) {
        Log.e(Self.tag, msg)
    }
    
    fileprivate func onNewPeripheralDiscover(peripheral: CBPeripheral) {
        availablePeripherals.append(peripheral)
    }
    
    fileprivate func setTcpInfo(tcpInfo: String) {
        self.tcpInfo = tcpInfo
    }
    
    fileprivate func setUdpInfo(udpInfo: String) {
        self.udpInfo = udpInfo
    }
    
}

private class MainViewModelDelegateProxy: BluetoothManagerDelegate {
    
    weak var viewModel: MainViewModel?
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    ) {
        if state == CBManagerState.poweredOn {
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
        peripheral.discoverServices([UUIDConstant.shared.GATT_FUN_UUID])
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverServices peripheral: CBPeripheral,
        error: Error?,
        services: [CBService]
    ) {
        if error != nil {
            viewModel?.showErrorMsg(msg: "Discover Service Error = \(error?.localizedDescription ?? "nil")")
            return
        }
        for svc in services {
            if svc.uuid == UUIDConstant.shared.GATT_FUN_UUID {
                peripheral.discoverCharacteristics([UUIDConstant.shared.TCP_INFO_UUID, UUIDConstant.shared.UDP_INFO_UUID], for: svc)
                return
            }
        }
        viewModel?.showErrorMsg(msg: "Service (\(UUIDConstant.shared.GATT_FUN_UUID)) not found.")
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverCharacteristics: CBPeripheral,
        characteristic service: CBUUID,
        error: Error?
    ) {
        if error != nil {
            viewModel?.showErrorMsg(msg: "DiscoverCharacyeristics error: \(error?.localizedDescription ?? "nil")")
            return
        }
        manager.read(servicesUUID: UUIDConstant.shared.GATT_FUN_UUID, characteristicUUID: UUIDConstant.shared.TCP_INFO_UUID)
        manager.read(servicesUUID: UUIDConstant.shared.GATT_FUN_UUID, characteristicUUID: UUIDConstant.shared.UDP_INFO_UUID)
    }
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didReceive: CBPeripheral,
        characteristic: CBUUID,
        data: Data,
        error: Error?
    ) {
        if error != nil {
            viewModel?.showErrorMsg(msg: "Receiver data error: \(error?.localizedDescription ?? "nil")")
            return
        }
        let dataStr = String(data: data, encoding: String.Encoding.utf8)
        switch characteristic {
        case UUIDConstant.shared.TCP_INFO_UUID:
            viewModel?.setTcpInfo(tcpInfo: dataStr ?? "")
        case UUIDConstant.shared.UDP_INFO_UUID:
            viewModel?.setUdpInfo(udpInfo: dataStr ?? "")
        default:
            viewModel?.showErrorMsg(msg: "Unknown data. uuid is \(characteristic.uuidString), data is \(dataStr ?? "nil")")
        }
    }
    
}
