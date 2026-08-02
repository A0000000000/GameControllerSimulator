//
//  TcpSocketClient.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/29.
//

import Network
import Foundation
import LogKit
import CommonKit

public protocol TcpSocketProtocol: AnyObject {
    func onConnected()
    func onDataReady(data: Data)
    func onSendDataError(error: NWError)
    func onReceiveError(error: NWError)
    func onConnectFailed(error: NWError)
    func onDisconnected()
}

public class TcpSocketClient: TransportProtocol {
    
    public static let tag = "TcpSocketClient"
    private let ip: String
    private let port: UInt16
    private var buff = Data()
    private var connection: NWConnection? = nil
    private let queue = DispatchQueue(label: "tcp")
    private weak var callback: TcpSocketProtocol?
    
    public private(set) var isAvailable: Bool = false
    
    public init(ip: String, port: UInt16, callback: TcpSocketProtocol) {
        self.ip = ip
        self.port = port
        self.callback = callback
    }
    
    public func connect() {
        connection = NWConnection(host: NWEndpoint.Host(ip), port: NWEndpoint.Port(rawValue: port)!, using: .tcp)
        connection?.stateUpdateHandler = { [weak self] state in
            switch state {
            case .ready:
                self?.isAvailable = true
                self?.callback?.onConnected()
                self?.onceReceive()
            case .cancelled:
                self?.isAvailable = false
                self?.callback?.onDisconnected()
            case .failed(let error):
                self?.isAvailable = false
                Log.e(Self.tag, "connect error = \(error)")
                self?.callback?.onConnectFailed(error: error)
            default:
                Log.d(Self.tag, "connect state is \(state)")
            }
        }
        connection?.start(queue: queue)
    }
    
    public func sendData(data: Data) {
        queue.async { [weak self] in
            guard let self else { return }
            let length = IntConverter.toBigEndian(data.count)
            var sendData = Data()
            sendData.append(contentsOf: length)
            sendData.append(data)
            connection?.send(
                content: sendData,
                completion: .contentProcessed {error in
                    if let error {
                        Log.e(Self.tag, "send error = \(error)")
                        self.callback?.onSendDataError(error: error)
                    }
                }
            )
        }
    }
    
    public func disConnected() {
        queue.async { [weak self] in
            guard let self else { return }
            connection?.cancel()
            connection = nil
            buff.removeAll()
        }
    }
    
    private func onceReceive() {
        connection?.receive(minimumIncompleteLength: 1, maximumLength: 4096) { [weak self] data, context, isComplete, error in
            guard let self else { return }
            if let data {
                buff.append(data)
                decodeBuff()
            }
            if let error {
                callback?.onReceiveError(error: error)
                return
            }
            if isComplete {
                callback?.onDisconnected()
                return
            }
            onceReceive()
        }
    }
    
    private func decodeBuff() {
        while true {
            guard buff.count >= 4 else { return }
            let length = IntConverter.fromBigEndian([UInt8](buff.prefix(4)))
            guard buff.count >= 4 + length else { return }
            let data = buff.subdata(in: 4 ..< 4 + length)
            buff.removeFirst(4 + length)
            Task {
                self.callback?.onDataReady(data: data)
            }
        }
    }
    
}
