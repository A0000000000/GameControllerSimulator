//
//  ButtonWidget.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/29.
//

import SwiftUI

public struct StrategyButton: View {
    
    let title: String
    let selected: Bool
    let enabled: Bool
    let action: () -> Void
    
    public init(
        title: String,
        selected: Bool,
        enabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.selected = selected
        self.enabled = enabled
        self.action = action
    }
    
    
    public var body: some View {
        Button {
            if enabled {
                action()
            }
        } label: {
            Text(title)
                .font(.system(size: 14, weight: .semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
        .background(
            RoundedRectangle(cornerRadius: 10)
                .fill(backgroundColor)
        )
        .foregroundStyle(foregroundColor)
        .disabled(!enabled)
    }
    
    
    private var backgroundColor: Color {
        if !enabled {
            return Color.gray.opacity(0.15)
        }
        
        if selected {
            return Color.blue
        }
        
        return Color.gray.opacity(0.15)
    }
    
    
    private var foregroundColor: Color {
        if !enabled {
            return Color.gray
        }
        
        if selected {
            return Color.white
        }
        
        return Color.primary
    }
}

public struct PrimaryButtonStyle: ButtonStyle {
    
    public init() {
        
    }
    
    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .bold))
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.blue)
            )
            .foregroundColor(.white)
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
    }
}

public struct SecondaryButtonStyle: ButtonStyle {
    
    public init() {
        
    }
    
    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 17, weight: .medium))
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(Color.gray.opacity(0.4))
            )
            .foregroundColor(.primary)
            .scaleEffect(configuration.isPressed ? 0.96 : 1)
    }
}
