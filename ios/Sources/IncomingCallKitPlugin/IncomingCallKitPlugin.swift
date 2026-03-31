import CallKit
import Capacitor
import Foundation

enum IncomingCallKitPluginError: LocalizedError {
    case missingField(String)
    case invalidHandleType(String)

    var errorDescription: String? {
        switch self {
        case .missingField(let field):
            return "Missing required field '\(field)'."
        case .invalidHandleType(let value):
            return "Unsupported iOS handleType '\(value)'. Use 'generic', 'phoneNumber', or 'emailAddress'."
        }
    }
}

@objc(IncomingCallKitPlugin)
public class IncomingCallKitPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "IncomingCallKitPlugin"
    public let jsName = "IncomingCallKit"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "showIncomingCall", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "endCall", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "endAllCalls", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getActiveCalls", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestFullScreenIntentPermission", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private let implementation = IncomingCallKit.shared

    override public func load() {
        implementation.attachDispatcher { [weak self] eventName, payload in
            self?.notifyListeners(eventName, data: payload, retainUntilConsumed: true)
        }
    }

    deinit {
        implementation.detachDispatcher()
    }

    @objc func showIncomingCall(_ call: CAPPluginCall) {
        do {
            let request = try makeRequest(from: call)
            implementation.showIncomingCall(request) { result in
                switch result {
                case .success(let entry):
                    call.resolve([
                        "call": entry.asDictionary()
                    ])
                case .failure(let error):
                    call.reject(error.localizedDescription)
                }
            }
        } catch {
            call.reject(error.localizedDescription)
        }
    }

    @objc func endCall(_ call: CAPPluginCall) {
        guard let callId = call.getString("callId"), !callId.isEmpty else {
            call.reject(IncomingCallKitPluginError.missingField("callId").localizedDescription)
            return
        }

        implementation.endCall(callId: callId, reason: call.getString("reason"))
        call.resolve([
            "calls": implementation.activeCalls()
        ])
    }

    @objc func endAllCalls(_ call: CAPPluginCall) {
        implementation.endAllCalls(reason: call.getString("reason"))
        call.resolve([
            "calls": implementation.activeCalls()
        ])
    }

    @objc func getActiveCalls(_ call: CAPPluginCall) {
        call.resolve([
            "calls": implementation.activeCalls()
        ])
    }

    @objc public override func checkPermissions(_ call: CAPPluginCall) {
        call.resolve(implementation.permissionStatus())
    }

    @objc public override func requestPermissions(_ call: CAPPluginCall) {
        call.resolve(implementation.permissionStatus())
    }

    @objc func requestFullScreenIntentPermission(_ call: CAPPluginCall) {
        call.resolve(implementation.permissionStatus())
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve([
            "version": implementation.pluginVersion()
        ])
    }

    private func makeRequest(from call: CAPPluginCall) throws -> IncomingCallRequest {
        guard let callId = call.getString("callId"), !callId.isEmpty else {
            throw IncomingCallKitPluginError.missingField("callId")
        }

        guard let callerName = call.getString("callerName"), !callerName.isEmpty else {
            throw IncomingCallKitPluginError.missingField("callerName")
        }

        let iosObject = call.getObject("ios") ?? [:]
        let handleType = try parseHandleType(iosObject["handleType"] as? String ?? "generic")

        return IncomingCallRequest(
            callId: callId,
            callerName: callerName,
            handle: call.getString("handle"),
            hasVideo: call.getBool("hasVideo") ?? false,
            timeoutMs: max(call.getDouble("timeoutMs") ?? 60_000, 0),
            extra: call.getObject("extra") ?? [:],
            supportsHolding: iosObject["supportsHolding"] as? Bool ?? true,
            supportsDTMF: iosObject["supportsDTMF"] as? Bool ?? false,
            supportsGrouping: iosObject["supportsGrouping"] as? Bool ?? false,
            supportsUngrouping: iosObject["supportsUngrouping"] as? Bool ?? false,
            handleType: handleType
        )
    }

    private func parseHandleType(_ value: String) throws -> CXHandle.HandleType {
        switch value {
        case "generic":
            return .generic
        case "phoneNumber":
            return .phoneNumber
        case "emailAddress":
            return .emailAddress
        default:
            throw IncomingCallKitPluginError.invalidHandleType(value)
        }
    }
}
