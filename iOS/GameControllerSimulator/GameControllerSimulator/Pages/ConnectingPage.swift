//
//  ConnectingPage.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/27.
//


import SwiftUI
import NetworkKit
import GamepadWidgetKit

struct ConnectingPage: View {
    
    let gattInfo: GATTStatus
    let networkInfo: NetworkInfo
    let currentConnectionType: ConnectionType
    
    let onTransportTypeSelect: (ConnectionType) -> Void
    let onItemClick: (ConnectionType) -> Void
    let onBack: () -> Void
    let onNextPage: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            Text("连接状态")
                .font(.system(size: 30))
                .padding(.top, 10)
                .padding(.bottom, 4)
            ScrollView {
                LazyVStack(spacing: 10) {
                    
                    ConnectionStatusCard(
                        title: "Bluetooth GATT", primaryLabel: "对端名称", primaryValue: gattInfo.name, statusText: gattInfo.isAvailble ? "就绪" : "未就绪", statusColor: gattInfo.isAvailble ? Color.green : Color.yellow
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                    
                    ConnectionStatusCard(
                        title: "TCP", primaryLabel: "对端地址", primaryValue: "\(networkInfo.tcpAddress):\(networkInfo.tcpPort)", statusText: networkInfo.tcpReady ? "就绪" : "未就绪", statusColor: networkInfo.tcpReady ? Color.green : Color.yellow
                    ) {
                        onItemClick(.TCP)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                    ConnectionStatusCard(
                        title: "UDP", primaryLabel: "对端地址", primaryValue: "\(networkInfo.udpAddress):\(networkInfo.udpPort)", statusText: networkInfo.udpReady ? "就绪" : "未就绪", statusColor: networkInfo.udpReady ? Color.green : Color.yellow
                    ) {
                        onItemClick(.UDP)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                }
                .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemBackground))
            
            VStack(spacing: 14) {
                
                // 传输策略选择
                HStack(spacing: 12) {
                    StrategyButton(
                        title: "SPEED FIRST",
                        selected: false,
                        enabled: networkInfo.tcpReady
                    ) {
                        onTransportTypeSelect(.UDP)
                    }
                    
                    StrategyButton(
                        title: "STABLE FIRST",
                        selected: false,
                        enabled: networkInfo.udpReady
                    ) {
                        onTransportTypeSelect(.TCP)
                    }
                }
                
                
                // 页面导航
                HStack(spacing: 12) {
                    
                    Button {
                        onBack()
                    } label: {
                        Text("Back")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(SecondaryButtonStyle())
                    
                    
                    Button {
                        onNextPage()
                    } label: {
                        Text("Next")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(PrimaryButtonStyle())
                }
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 20)
            
        }
        
    }
    
}

