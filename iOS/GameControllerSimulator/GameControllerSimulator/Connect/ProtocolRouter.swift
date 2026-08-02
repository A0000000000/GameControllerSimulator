//
//  ProtocolRouter.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/8/2.
//
import Foundation
import LogKit
import NetworkKit

struct RouteHandler: Equatable {
    let id: Int
    let type: Int
    let handler: (any IBaseEntity, ConnectionType) -> Void
    
    static func == (
        lhs: RouteHandler,
        rhs: RouteHandler
    ) -> Bool {
        return lhs.id == rhs.id &&
        lhs.type == rhs.type
    }
    
}

class ProtocolRouter {
    
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()
    
    private var handlers: [Int:[Int:RouteHandler]] = [:]
    
    public func encode<T: Codable>(entity: BaseEntity<T>) -> Data? {
        return try? encoder.encode(entity)
    }
    
    public func decode(data: Data) -> IBaseEntity? {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String : Any] else { return nil }
        let type = json["type"] as? Int ?? -1
        let entityType: any Decodable.Type = EntityType.mapping[type] ?? BaseEntity<EmptyData>.self
        return try? decoder.decode(entityType, from: data) as? IBaseEntity
    }
    
    public func registerHandler(handlers: [RouteHandler]) {
        for handler in handlers {
            var typeHandlers = self.handlers[handler.id] ?? [:]
            typeHandlers[handler.type] = handler
            self.handlers[handler.id] = typeHandlers
        }
    }

    public func unregisterHandler(handlers: [RouteHandler]) {
        for handler in handlers {
            guard var typeHandlers = self.handlers[handler.id] else { continue }
            typeHandlers.removeValue(forKey: handler.type)
            if typeHandlers.isEmpty {
                self.handlers.removeValue(
                    forKey: handler.id
                )
            } else {
                self.handlers[handler.id] = typeHandlers
            }
        }
    }
    
    public func clearHandlers() {
        handlers.removeAll()
    }
    
    public func dispatcherData(data: Data, type: ConnectionType) {
        guard let parserData = decode(data: data) else { return }
        handlers[parserData.id]?[parserData.type]?.handler(parserData, type)
    }
    
}
