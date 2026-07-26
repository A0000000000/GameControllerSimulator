//
//  BluetoothManager.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import Foundation
import CoreBluetooth


public protocol BluetoothManagerDelegate: AnyObject {

    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscover peripheral: CBPeripheral
    )

    func bluetoothManager(
        _ manager: BluetoothManager,
        didReceive data: Data,
        characteristic: CBUUID
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        ready peripheral: CBPeripheral
    )

    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    )
}



public class BluetoothManager: NSObject {

    public static let TAG = "BluetoothManager"
    private var centralManager: CBCentralManager!
    private var peripheral: CBPeripheral?
    private var servicesUUID: CBUUID?
    private var characteristicsUUID: [CBUUID] = []
    public weak var delegate: BluetoothManagerDelegate?
    
    public override init() {
        super.init()
        centralManager = CBCentralManager(
            delegate: self,
            queue: DispatchQueue.main
        )
    }

    public func scan(
        serviceUUID: CBUUID
    ) {
        servicesUUID = serviceUUID
        centralManager.scanForPeripherals(
            withServices: [
                serviceUUID
            ],
            options: nil
        )
    }

    public func stopScan() {
        centralManager.stopScan()
    }

    private func connect(
        _ peripheral: CBPeripheral
    ) {
        self.peripheral = peripheral
        peripheral.delegate = self
        centralManager.connect(peripheral)
    }

    public func discoverCharacteristics(
        uuids: [CBUUID]
    ) {

        characteristicsUUID = uuids
        peripheral?.discoverServices(
            [servicesUUID!]
        )
    }

    public func read(
        characteristicUUID: CBUUID
    ) {
        guard let peripheral else {
            return
        }
        guard let service =
                peripheral.services?
                    .first
        else {
            return
        }
        guard let characteristic =
                service.characteristics?
                    .first(
                        where: {
                            $0.uuid == characteristicUUID
                        }
                    )
        else {
            return
        }
        peripheral.readValue(
            for: characteristic
        )
    }

    public func subscribe(
        characteristicUUID: CBUUID
    ) {
        guard let peripheral else {
            return
        }
        guard let service =
                peripheral.services?
                    .first
        else {
            return
        }
        guard let characteristic =
                service.characteristics?
                    .first(
                        where:{
                            $0.uuid == characteristicUUID
                        }
                    )
        else {
            return
        }
        peripheral.setNotifyValue(
            true,
            for: characteristic
        )
    }
}


extension BluetoothManager: CBCentralManagerDelegate {

    public func centralManagerDidUpdateState(
        _ central: CBCentralManager
    ) {
        delegate?.bluetoothManager(
            self,
            stateChanged: central.state
        )
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String : Any],
        rssi RSSI: NSNumber
    ) {
        print("发现设备:", peripheral.name ?? "")
        central.stopScan()
        self.peripheral = peripheral
        delegate?.bluetoothManager(self, didDiscover: peripheral)
        connect(peripheral)
    }

    public func centralManager(
        _ central: CBCentralManager,
        didConnect peripheral: CBPeripheral
    ) {
        print("连接成功")
        peripheral.delegate = self
        if let uuid = servicesUUID {
            peripheral.discoverServices(
                [uuid]
            )
        }
    }

}

extension BluetoothManager: CBPeripheralDelegate {

    public func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverServices error: Error?
    ) {
        guard let services =
                peripheral.services
        else {
            return
        }
        for service in services {
            print("Service:", service.uuid)
            peripheral.discoverCharacteristics(
                characteristicsUUID,
                for: service
            )
        }
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        for c in service.characteristics ?? [] {
            print("Characteristic:", c.uuid)
        }
        delegate?.bluetoothManager(
                self,
                ready: peripheral
            )
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        guard let data =
                characteristic.value
        else {
            return
        }
        delegate?.bluetoothManager(
            self,
            didReceive: data,
            characteristic: characteristic.uuid
        )
    }

}
