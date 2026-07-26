//
//  LogUtils.swift
//  GameControllerSimulator
//
//  Created by 猫眼螺 on 2026/7/26.
//
import Foundation
import os.log

public enum Log {
    
    public static var enable: Bool = true
    
    public static func d(
        _ tag: String,
        _ message: String,
        file: String = #file,
        line: Int = #line
    ) {
        log(
            level: .debug,
            tag: tag,
            message: message,
            file: file,
            line: line
        )
    }
    
    public static func i(
        _ tag: String,
        _ message: String,
        file: String = #file,
        line: Int = #line
    ) {
        log(
            level: .info,
            tag: tag,
            message: message,
            file: file,
            line: line
        )
    }
    
    public static func w(
        _ tag: String,
        _ message: String,
        file: String = #file,
        line: Int = #line
    ) {
        log(
            level: .warning,
            tag: tag,
            message: message,
            file: file,
            line: line
        )
    }
    
    public static func e(
        _ tag: String,
        _ message: String,
        file: String = #file,
        line: Int = #line
    ) {
        log(
            level: .error,
            tag: tag,
            message: message,
            file: file,
            line: line
        )
    }
    
    private static func log(
        level: LogLevel,
        tag: String,
        message: String,
        file: String,
        line: Int
    ) {
        
        guard enable else {
            return
        }
        
        let fileName = URL(fileURLWithPath: file)
            .lastPathComponent
        
        let time = DateFormatter.logFormatter
            .string(from: Date())
        
        let output =
        "[\(time)] [\(level.name)] [\(tag)] \(fileName):\(line) - \(message)"
        
        
        switch level {
        case .debug:
            os_log("%{public}@", log: .default, type: .debug, output)
            
        case .info:
            os_log("%{public}@", log: .default, type: .info, output)
            
        case .warning:
            os_log("%{public}@", log: .default, type: .default, output)
            
        case .error:
            os_log("%{public}@", log: .default, type: .error, output)
        }
    }
}

private enum LogLevel {
    
    case debug
    case info
    case warning
    case error
    
    
    var name: String {
        switch self {
        case .debug:
            return "DEBUG"
            
        case .info:
            return "INFO"
            
        case .warning:
            return "WARN"
            
        case .error:
            return "ERROR"
        }
    }
}


private extension DateFormatter {
    
    static let logFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
        return formatter
    }()
    
}
