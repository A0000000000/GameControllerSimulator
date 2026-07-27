//
//  ContentView.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/6/29.
//

import SwiftUI

struct ContentView: View {
    
    @State private var viewModel = MainViewModel()
    
    var body: some View {
        switch (viewModel.currentPage) {
        case PageEnum.SelectPage:
            SelectPage(viewModel: viewModel)
        case PageEnum.ConnectingPage:
            ConnectingPage()
        case PageEnum.GamepadPage:
            GamepadPage()
        }
    }
}

#Preview {
    ContentView()
}
