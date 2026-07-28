//
//  ConnectionStatusCard.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/28.
//

import SwiftUI


public struct ConnectionStatusCard: View {
    let title: String
    let primaryLabel: String
    let primaryValue: String
    let statusText: String
    let statusColor: Color
    let onTap: (() -> Void)?

    @Environment(\.colorScheme) private var colorScheme

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

    public init(
        title: String,
        primaryLabel: String,
        primaryValue: String,
        statusText: String,
        statusColor: Color,
        onTap: (() -> Void)? = nil
    ) {
        self.title = title
        self.primaryLabel = primaryLabel
        self.primaryValue = primaryValue
        self.statusText = statusText
        self.statusColor = statusColor
        self.onTap = onTap
    }

    public var body: some View {
        VStack(spacing: 12) {
            HStack {
                Text(title)
                    .font(.system(size: 20))
                    .foregroundColor(primaryTextColor)

                Spacer()

                Text(statusText)
                    .font(.system(size: 14))
                    .foregroundColor(statusColor)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(
                        Capsule()
                            .fill(statusColor.opacity(0.14))
                    )
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(primaryLabel)
                    .font(.system(size: 14))
                    .foregroundColor(secondaryTextColor)

                Text(primaryValue)
                    .font(.system(size: 18))
                    .foregroundColor(primaryTextColor)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .frame(maxWidth: .infinity)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(cardBackground)
        )
        .contentShape(RoundedRectangle(cornerRadius: 20))
        .onTapGesture {
            onTap?()
        }
    }
}
