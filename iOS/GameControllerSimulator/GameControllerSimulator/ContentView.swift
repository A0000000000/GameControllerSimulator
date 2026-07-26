//
//  ContentView.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/6/29.
//

import SwiftUI
import NetworkKit
import BluetoothKit
import CommonKit


struct ContentView: View {
    
    @State private var viewModel = MainViewModel()
    
    
    var body: some View {
        VStack {
            Button {
                
            } label: {
                Text("测试")
            }

        }
        .padding()
    }
}

#Preview {
    ContentView()
}
