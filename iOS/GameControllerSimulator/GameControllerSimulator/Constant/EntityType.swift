//
//  EntityType.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/8/2.
//

struct EmptyData: Codable {}

enum EntityType: Int {
    
    case TYPE_REQUEST_CLIENT_ID = 1
    case TYPE_REQUEST_CLIENT_ID_RESULT = 2
    case TYPE_UNREGISTER_CLIENT_ID = 3
    case TYPE_UNREGISTER_CLIENT_ID_RESULT = 4
    case TYPE_SEND_GAME_EVENT = 5
    case TYPE_SEND_GAME_EVENT_RESULT = 6
    case TYPE_FEEDBACK_RECEIVED = 7
    case TYPE_FEEDBACK_RECEIVED_RESULT = 8
    case TYPE_QUERY_CLIENT_INFO = 9
    case TYPE_QUERY_CLIENT_INFO_RESULT = 10
    case TYPE_NEW_TYPE_CONNECT = 11
    case TYPE_NEW_TYPE_CONNECT_RESULT = 12
    case TYPE_ECHO = 13
    case TYPE_ECHO_RESULT = 14
    case TYPE_RTT = 15
    case TYPE_RTT_RESULT = 16
    
    public static let mapping: [Int: any Decodable.Type] = [
        TYPE_REQUEST_CLIENT_ID.rawValue:
            BaseEntity<EmptyData>.self,
        
        TYPE_REQUEST_CLIENT_ID_RESULT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_UNREGISTER_CLIENT_ID.rawValue:
            BaseEntity<String>.self,
        
        TYPE_UNREGISTER_CLIENT_ID_RESULT.rawValue:
            BaseEntity<EmptyData>.self,
        
        TYPE_SEND_GAME_EVENT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_SEND_GAME_EVENT_RESULT.rawValue:
            BaseEntity<EmptyData>.self,
        
        TYPE_FEEDBACK_RECEIVED.rawValue:
            BaseEntity<FeedbackReceived>.self,
        
        TYPE_FEEDBACK_RECEIVED_RESULT.rawValue:
            BaseEntity<EmptyData>.self,
        
        TYPE_QUERY_CLIENT_INFO.rawValue:
            BaseEntity<EmptyData>.self,
        
        TYPE_QUERY_CLIENT_INFO_RESULT.rawValue:
            BaseEntity<DeviceInfo>.self,
        
        TYPE_NEW_TYPE_CONNECT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_NEW_TYPE_CONNECT_RESULT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_ECHO.rawValue:
            BaseEntity<String>.self,
        
        TYPE_ECHO_RESULT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_RTT.rawValue:
            BaseEntity<String>.self,
        
        TYPE_RTT_RESULT.rawValue:
            BaseEntity<String>.self
    ]
    
}
