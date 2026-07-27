//
//  PeripheralInfoWidget.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/27.
//

import SwiftUI


public struct PeripheralInfoWidget: View {
    let deviceName: String
    let deviceAddress: String
    let onTap: (() -> Void)?   // 点击回调，可选

    @Environment(\.colorScheme) private var colorScheme
    
    public init(onTap: (() -> Void)?) {
        self.deviceName = "未知设备"
        self.deviceAddress = "未知地址"
        self.onTap = onTap
    }

    public init(deviceName: String, deviceAddress: String, onTap: (() -> Void)?) {
        self.deviceName = deviceName
        self.deviceAddress = deviceAddress
        self.onTap = onTap
    }
    
    // 根据主题返回颜色
    private var cardBackground: Color {
        colorScheme == .dark
            ? Color(red: 31/255, green: 41/255, blue: 55/255)   // #1F2937
            : Color(red: 243/255, green: 244/255, blue: 246/255) // #F3F4F6
    }

    private var primaryTextColor: Color {
        colorScheme == .dark
            ? Color(red: 249/255, green: 250/255, blue: 251/255) // #F9FAFB
            : Color(red: 17/255, green: 24/255, blue: 39/255)    // #111827
    }

    private var secondaryTextColor: Color {
        colorScheme == .dark
            ? Color(red: 209/255, green: 213/255, blue: 219/255) // #D1D5DB
            : Color(red: 107/255, green: 114/255, blue: 128/255) // #6B7280
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(deviceName)
                .font(.system(size: 20))
                .foregroundColor(primaryTextColor)
            Text(deviceAddress)
                .font(.system(size: 14))
                .foregroundColor(secondaryTextColor)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: 12)
                .fill(cardBackground)
        )
        .onTapGesture {
            onTap?()
        }
    }
}
