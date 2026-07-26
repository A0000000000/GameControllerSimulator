//
//  UUIDConstant.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import Foundation
import CoreBluetooth

class UUIDConstant: NSObject {
    
    static let shared = UUIDConstant()
    
    private override init() {
        super.init()
    }

    let DEFAULT_RFCOMM_UUID = CBUUID(string: "0000180D-0000-1000-8000-00805f9b34fb")
    let GATT_FUN_UUID = CBUUID(string: "09251205-1113-1998-2026-000000000000")
    let GATT_DATA_RFCOMM_UUID = CBUUID(string: "09251205-1113-1998-2026-000000000001")
    let HOST_UUID = CBUUID(string: "09251205-1113-1998-2026-000000000002")
    let TCP_INFO_UUID = CBUUID(string: "09251205-1113-1998-2026-000000000003")
    let UDP_INFO_UUID = CBUUID(string: "09251205-1113-1998-2026-000000000004")
    
}
