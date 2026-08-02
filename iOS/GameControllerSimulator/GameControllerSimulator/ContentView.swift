//
//  ContentView.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/6/29.
//

import SwiftUI
import LogKit

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
            }
        case UIState.ConnectingPage:
            ConnectingPage(
                gattInfo: viewModel.gattInfo,
                networkInfo: viewModel.networkInfo,
                currentConnectionType: viewModel.currentConnectionType,
                onTransportTypeSelect: { transport in
                    viewModel.onUIIntent(intent: .ConnectionTypeChange(transport))
                },
                onItemClick: { transport in
                    viewModel.onUIIntent(intent: .OnRequestRtt(transport))
                },
                onBack: {
                    viewModel.onUIIntent(intent: .BackSelectPage)
                },
                onNextPage: {
                    viewModel.onUIIntent(intent: .OpenGamepadPage)
                }
            )
        case UIState.GamepadPage:
            GamepadPage()
        }
    }
}

#Preview {
    ContentView()
}
