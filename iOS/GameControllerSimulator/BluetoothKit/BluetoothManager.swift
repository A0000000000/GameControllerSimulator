//
//  BluetoothManager.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import Foundation
import CoreBluetooth
import LogKit

public protocol BluetoothManagerDelegate: AnyObject {

    func bluetoothManager(
        _ manager: BluetoothManager,
        stateChanged state: CBManagerState
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscover peripheral: CBPeripheral
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didConnect peripheral: CBPeripheral
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverServices peripheral: CBPeripheral,
        error: Error?,
        services: [CBService]
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didDiscoverCharacteristics: CBPeripheral,
        characteristic service: CBUUID,
        error: Error?
    )
    
    func bluetoothManager(
        _ manager: BluetoothManager,
        didReceive: CBPeripheral,
        characteristic: CBUUID,
        data: Data,
        error: Error?
    )
    
}

public class BluetoothManager: NSObject {

    public static let tag = "BluetoothManager"
    private let queue = DispatchQueue(label: "ble")
    private var centralManager: CBCentralManager!
    private var peripheral: CBPeripheral?
    public weak var delegate: BluetoothManagerDelegate?
    
    public override init() {
        super.init()
        Log.d(Self.tag, "Create BluetoothManager")
    }
    
    public func initCBCentralManager() {
        centralManager = CBCentralManager(
            delegate: self,
            queue: queue
        )
        Log.d(Self.tag, "Create CBCentralManager")
    }
    
    public func scan(
        serviceUUID: CBUUID
    ) {
        Log.d(Self.tag, "CBCentralManager begin scan service, uuid = \(serviceUUID.uuidString)")
        centralManager.scanForPeripherals(
            withServices: [
                serviceUUID
            ],
            options: nil
        )
    }

    public func stopScan() {
        centralManager.stopScan()
        Log.d(Self.tag, "CBCentralManager stopScan")
    }

    public func connect(
        _ peripheral: CBPeripheral
    ) {
        Log.d(Self.tag, "prepare connect \(peripheral.name ?? "nil")")
        self.peripheral = peripheral
        peripheral.delegate = self
        centralManager.connect(peripheral)
    }
    
    public func discoverServices(uuids: [CBUUID]) {
        peripheral?.discoverServices(uuids)
    }
    
    public func discoverCharacteristics(service: CBService, uuids: [CBUUID]) {
        peripheral?.discoverCharacteristics(uuids, for: service)
    }

    public func read(
        servicesUUID: CBUUID,
        characteristicUUID: CBUUID
    ) {
        Log.d(Self.tag, "read characteristic uuid = \(characteristicUUID.uuidString)")
        guard let peripheral else {
            return
        }
        guard let service =
                peripheral.services?
            .first(where: {
                $0.uuid == servicesUUID
            })
        else {
            Log.w(Self.tag, "Cannot find service. uuid = \(servicesUUID.uuidString)")
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
            Log.w(Self.tag, "Cannot find characteristic. uuid = \(characteristicUUID.uuidString)")
            return
        }
        peripheral.readValue(
            for: characteristic
        )
    }
    
}


extension BluetoothManager: CBCentralManagerDelegate {

    public func centralManagerDidUpdateState(
        _ central: CBCentralManager
    ) {
        Log.d(Self.tag, "centralManagerDidUpdateState state = \(central.state)")
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
        Log.d(Self.tag, "centralManager didDiscover peripheral is \(peripheral.name ?? "nil")")
        delegate?.bluetoothManager(self, didDiscover: peripheral)
    }

    public func centralManager(
        _ central: CBCentralManager,
        didConnect peripheral: CBPeripheral
    ) {
        Log.d(Self.tag, "centralManager didConnect peripheral is \(peripheral.name ?? "nil")")
        self.peripheral = peripheral
        peripheral.delegate = self
        self.delegate?.bluetoothManager(self, didConnect: peripheral)
    }

}

extension BluetoothManager: CBPeripheralDelegate {

    public func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverServices error: Error?
    ) {
        Log.d(Self.tag, "peripheral didDiscoverServices error is \(error?.localizedDescription ?? "no error")")
        guard let services =
                peripheral.services
        else {
            Log.w(Self.tag, "peripheral didDiscoverServices services is nil")
            return
        }
        for service in services {
            Log.d(Self.tag, "service uuid is \(service.uuid.uuidString)")
        }
        self.delegate?.bluetoothManager(self, didDiscoverServices: peripheral, error: error, services: services)
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didDiscoverCharacteristicsFor service: CBService,
        error: Error?
    ) {
        Log.d(Self.tag, "peripheral didDiscoverCharacteristicsFor service = \(service.uuid.uuidString), error = \(error?.localizedDescription ?? "no error")")
        for c in service.characteristics ?? [] {
            Log.d(Self.tag, "Characteristic uuid = \(c.uuid)")
        }
        delegate?.bluetoothManager(self, didDiscoverCharacteristics: peripheral, characteristic: service.uuid, error: error)
    }

    public func peripheral(
        _ peripheral: CBPeripheral,
        didUpdateValueFor characteristic: CBCharacteristic,
        error: Error?
    ) {
        Log.d(Self.tag, "peripheral didUpdateValueFor error = \(error?.localizedDescription ?? "no error")")
        guard let data =
                characteristic.value
        else {
            return
        }
        delegate?.bluetoothManager(self, didReceive: peripheral, characteristic: characteristic.uuid, data: data, error: error)
    }

}
