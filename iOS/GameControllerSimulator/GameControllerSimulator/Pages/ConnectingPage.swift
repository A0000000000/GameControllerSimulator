//
//  ConnectingPage.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/27.
//


import SwiftUI
import GamepadWidgetKit

struct ConnectingPage: View {
    
    let tcpInfo: String
    let udpInfo: String
    
    let onTransportTypeSelect: (Int) -> Void
    let onItemClick: (Int) -> Void
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
                        title: "TCP", primaryLabel: "对端地址", primaryValue: tcpInfo, statusText: "未就绪", statusColor: Color.yellow
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                    ConnectionStatusCard(
                        title: "UDP", primaryLabel: "对端地址", primaryValue: udpInfo, statusText: "未就绪", statusColor: Color.yellow
                    )
                    .padding(.horizontal, 16)
                    .padding(.vertical, 4)
                }
                .frame(maxWidth: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(.systemBackground))
            .safeAreaInset(edge: .bottom, spacing: 0) {
                VStack(spacing: 12) {
                    HStack(spacing: 16) {
                        Button("SPEED FIRST") {
                            
                        }

                        Button("STABLE FIRST") {
                            
                        }
                    }
                    HStack(spacing: 16) {
                        Button("Back") {
                            
                        }

                        Button("Next") {
                            
                        }
                    }
                }
            }
        }
    }
    
}
