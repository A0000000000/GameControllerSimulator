//
//  FeedbackReceived.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/8/2.
//

struct FeedbackReceived: Codable {
    
    let largeMotor: Int
    let smallMotor: Int
    let ledNumber: Int
 
    enum CodingKeys: String, CodingKey {
        case largeMotor = "large_motor"
        case smallMotor = "small_motor"
        case ledNumber = "led_number"
    }
    
}
