//
//  ContentView.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/6/29.
//

import SwiftUI
import UIKit
import LogKit
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

struct ContentView: View {
    
    public static let tag = "ContentView"
    
    @State private var viewModel = MainViewModel()
    
    var body: some View {
        switch viewModel.currentPage {
        case UIState.SelectPage:
            SelectPage(peripherals: viewModel.availablePeripherals, onPeripheralSelected: { peripheral in
                viewModel.onUIIntent(intent: UIIntent.PeripheralSelected(peripheral))
            }) {
                viewModel.onUIIntent(intent: UIIntent.InitAvailableBluetooth)
            }.onAppear {
                Log.d(Self.tag, "name = \(UIDevice.current.name)")
                Log.d(Self.tag, "systemName = \(UIDevice.current.systemName)")
                Log.d(Self.tag, "model = \(UIDevice.current.model)")
                Log.d(Self.tag, "localizedModel = \(UIDevice.current.localizedModel)")
                Log.d(Self.tag, "systemVersion = \(UIDevice.current.systemVersion)")
                Log.d(Self.tag, "detailedModel = \(UIDevice.current.detailedModel)")
            }
        case UIState.ConnectingPage:
            ConnectingPage(tcpInfo: viewModel.tcpInfo, udpInfo: viewModel.udpInfo, onTransportTypeSelect: { transport in
                
            }, onItemClick: { transport in
                
            }) {
                
            }
        case UIState.GamepadPage:
            GamepadPage()
        }
    }
}

#Preview {
    ContentView()
}
