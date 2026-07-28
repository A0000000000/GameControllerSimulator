//
//  SelectPage.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/27.
//

import SwiftUI
import CoreBluetooth
import GamepadWidgetKit
import LogKit

struct SelectPage: View {
    
    public static let tag = "SelectPage"
    
    let peripherals: [CBPeripheral]
    let onPeripheralSelected: (CBPeripheral) -> Void
    let onPageLoad: () -> Void
    
    @State private var hasLoaded = false
    
    var body: some View {
        VStack(spacing: 0) {
            Text("可用设备列表")
                .font(.system(size: 30))
                .padding(.top, 10)
                .padding(.bottom, 4)
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(peripherals, id: \.identifier) { item in
                        PeripheralInfoWidget(deviceName: item.name ?? "未知设备", deviceAddress: item.identifier.uuidString) {
                            Log.d(Self.tag, "select device: \(item.name ?? "nil")")
                            onPeripheralSelected(item)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 4)
                    }
                }
                .frame(maxHeight: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemBackground))
        }
        .onAppear {
            if !hasLoaded {
                hasLoaded = true
                onPageLoad()
            }
        }
    }
    
}
