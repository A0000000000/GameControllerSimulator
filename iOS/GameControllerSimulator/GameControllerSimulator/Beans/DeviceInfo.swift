//
//  DeviceInfo.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/8/2.
//

import UIKit
import Darwin

extension UIDevice {

    var detailedModel: String {
        var systemInfo = utsname()
        uname(&systemInfo)
        let machineMirror = Mirror(reflecting: systemInfo.machine)
        let identifier = machineMirror.children.reduce("") { identifier, element in
            guard let value = element.value as? Int8, value != 0 else { return identifier }
            return identifier + String(UnicodeScalar(UInt8(value)))
        }
        return identifier
    }

}


struct DeviceInfo: Codable {
    
    var osVersion: String = "\(UIDevice.current.systemName) \(UIDevice.current.systemVersion)"
    var model: String = UIDevice.current.detailedModel
    var device: String = UIDevice.current.name
    var product: String = UIDevice.current.localizedModel
    
    enum CodingKeys: String, CodingKey {
        case osVersion = "os_version"
        case model = "model"
        case device = "device"
        case product = "product"
    }
    
}
