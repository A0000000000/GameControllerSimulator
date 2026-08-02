//
//  BaseEntity.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/8/2.
//


protocol IBaseEntity: Codable {
    var type: Int { get }
    var id: Int { get }
}

struct BaseEntity<T: Codable>: IBaseEntity {
    let type: Int
    let id: Int
    let timestamp: Int64
    let data: T?
    
    
    enum CodingKeys: String, CodingKey {
        case type = "type"
        case id = "id"
        case timestamp = "timestamp"
        case data = "data"
    }
}
