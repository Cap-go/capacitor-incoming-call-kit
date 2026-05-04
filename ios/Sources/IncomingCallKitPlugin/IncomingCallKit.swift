import CallKit
import Foundation
import AVFoundation

struct IncomingCallRequest {
    let callId: String
    let callerName: String
    let handle: String?
    let hasVideo: Bool
    let timeoutMs: Double
    let extra: [String: Any]
    let supportsHolding: Bool
    let supportsDTMF: Bool
    let supportsGrouping: Bool
    let supportsUngrouping: Bool
    let handleType: CXHandle.HandleType
}

final class IncomingCallEntry {
    let callId: String
    let uuid: UUID
    let callerName: String
    let handle: String?
    let hasVideo: Bool
    let extra: [String: Any]
    let supportsHolding: Bool
    let supportsDTMF: Bool
    let supportsGrouping: Bool
    let supportsUngrouping: Bool
    let handleType: CXHandle.HandleType
    var state = "ringing"
    var timeoutWorkItem: DispatchWorkItem?

    init(request: IncomingCallRequest, uuid: UUID) {
        self.callId = request.callId
        self.uuid = uuid
        self.callerName = request.callerName
        self.handle = request.handle
        self.hasVideo = request.hasVideo
        self.extra = request.extra
        self.supportsHolding = request.supportsHolding
        self.supportsDTMF = request.supportsDTMF
        self.supportsGrouping = request.supportsGrouping
        self.supportsUngrouping = request.supportsUngrouping
        self.handleType = request.handleType
    }

    func asDictionary() -> [String: Any] {
        var dictionary: [String: Any] = [
            "callId": callId,
            "callerName": callerName,
            "hasVideo": hasVideo,
            "state": state,
            "platform": "ios"
        ]

        if let handle, !handle.isEmpty {
            dictionary["handle"] = handle
        }

        if !extra.isEmpty {
            dictionary["extra"] = extra
        }

        return dictionary
    }
}

@objcMembers
public final class IncomingCallKit: NSObject {
    public static let shared = IncomingCallKit()

    private let pendingEventsKey = "CapgoIncomingCallKit.pendingEvents"
    private var provider: CXProvider?
    private var callsById: [String: IncomingCallEntry] = [:]
    private var callIdByUUID: [UUID: String] = [:]

    var eventDispatcher: ((String, [String: Any]) -> Void)?

    override private init() {
        super.init()
        configureProvider()
    }

    func attachDispatcher(_ dispatcher: @escaping (String, [String: Any]) -> Void) {
        eventDispatcher = dispatcher
        drainPendingEvents().forEach { dispatcher($0.eventName, $0.payload) }
    }

    func detachDispatcher() {
        eventDispatcher = nil
    }

    func showIncomingCall(_ request: IncomingCallRequest, completion: @escaping (Result<IncomingCallEntry, Error>) -> Void) {
        DispatchQueue.main.async {
            if let existing = self.callsById[request.callId] {
                completion(.success(existing))
                return
            }

            let entry = IncomingCallEntry(request: request, uuid: UUID())
            let update = CXCallUpdate()
            let handleValue = request.handle?.isEmpty == false ? request.handle! : request.callerName

            update.remoteHandle = CXHandle(type: request.handleType, value: handleValue)
            update.localizedCallerName = request.callerName
            update.hasVideo = request.hasVideo
            update.supportsHolding = request.supportsHolding
            update.supportsDTMF = request.supportsDTMF
            update.supportsGrouping = request.supportsGrouping
            update.supportsUngrouping = request.supportsUngrouping

            self.callsById[request.callId] = entry
            self.callIdByUUID[entry.uuid] = request.callId

            self.provider?.reportNewIncomingCall(with: entry.uuid, update: update) { error in
                if let error {
                    self.removeCall(callId: request.callId)
                    completion(.failure(error))
                    return
                }

                self.scheduleTimeout(for: entry, timeoutMs: request.timeoutMs)
                self.emit(
                    eventName: "incomingCallDisplayed",
                    payload: self.makeEventPayload(for: entry, reason: nil, source: "api")
                )
                completion(.success(entry))
            }
        }
    }

    func endCall(callId: String, reason: String?) {
        DispatchQueue.main.async {
            guard let entry = self.callsById[callId] else {
                return
            }

            self.provider?.reportCall(with: entry.uuid, endedAt: Date(), reason: .remoteEnded)
            self.cancelTimeout(for: entry)
            self.removeCall(callId: callId)
            self.emit(
                eventName: "callEnded",
                payload: self.makeEventPayload(for: entry, reason: reason ?? "ended", source: "api")
            )
        }
    }

    func endAllCalls(reason: String?) {
        let callIds = Array(callsById.keys)
        callIds.forEach { endCall(callId: $0, reason: reason) }
    }

    func activeCalls() -> [[String: Any]] {
        callsById.values
            .sorted { $0.callId < $1.callId }
            .map { $0.asDictionary() }
    }

    func permissionStatus() -> [String: Any] {
        [
            "notifications": "notApplicable",
            "fullScreenIntent": "notApplicable"
        ]
    }

    func pluginVersion() -> String {
        Bundle(for: IncomingCallKitPlugin.self)
            .infoDictionary?["CFBundleShortVersionString"] as? String ?? "ios"
    }

    private func configureProvider() {
        let configuration = CXProviderConfiguration()
        configuration.supportsVideo = true
        configuration.maximumCallGroups = 1
        configuration.maximumCallsPerCallGroup = 1
        configuration.includesCallsInRecents = false
        configuration.supportedHandleTypes = [.generic, .phoneNumber, .emailAddress]

        let provider = CXProvider(configuration: configuration)
        provider.setDelegate(self, queue: nil)
        self.provider = provider
    }

    private func emit(eventName: String, payload: [String: Any]) {
        if let eventDispatcher {
            eventDispatcher(eventName, payload)
        } else {
            queuePendingEvent(eventName: eventName, payload: payload)
        }
    }

    private func makeEventPayload(for entry: IncomingCallEntry, reason: String?, source: String) -> [String: Any] {
        var payload: [String: Any] = [
            "call": entry.asDictionary(),
            "source": source
        ]

        if let reason, !reason.isEmpty {
            payload["reason"] = reason
        }

        return payload
    }

    private func makeOutgoingCallEntry(for action: CXStartCallAction) -> IncomingCallEntry {
        let uuidString = action.callUUID.uuidString
        let handle = action.handle.value
        let callerName = handle.isEmpty ? uuidString : handle
        let request = IncomingCallRequest(
            callId: uuidString,
            callerName: callerName,
            handle: handle.isEmpty ? nil : handle,
            hasVideo: action.isVideo,
            timeoutMs: 0,
            extra: [:],
            supportsHolding: false,
            supportsDTMF: false,
            supportsGrouping: false,
            supportsUngrouping: false,
            handleType: action.handle.type
        )
        let entry = IncomingCallEntry(request: request, uuid: action.callUUID)
        entry.state = "accepted"
        return entry
    }

    private func makeUnknownCallEndedPayload(callUUID: UUID) -> [String: Any] {
        [
            "call": [
                "callId": callUUID.uuidString,
                "callerName": "",
                "hasVideo": false,
                "state": "ended",
                "platform": "ios"
            ],
            "source": "user",
            "reason": "ended"
        ]
    }

    private func scheduleTimeout(for entry: IncomingCallEntry, timeoutMs: Double) {
        guard timeoutMs > 0 else {
            return
        }

        let workItem = DispatchWorkItem { [weak self] in
            self?.handleTimeout(callId: entry.callId)
        }

        entry.timeoutWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + (timeoutMs / 1000), execute: workItem)
    }

    private func cancelTimeout(for entry: IncomingCallEntry) {
        entry.timeoutWorkItem?.cancel()
        entry.timeoutWorkItem = nil
    }

    private func handleTimeout(callId: String) {
        guard let entry = callsById[callId], entry.state == "ringing" else {
            return
        }

        provider?.reportCall(with: entry.uuid, endedAt: Date(), reason: .unanswered)
        cancelTimeout(for: entry)
        removeCall(callId: callId)
        emit(
            eventName: "callTimedOut",
            payload: makeEventPayload(for: entry, reason: "timeout", source: "system")
        )
    }

    private func removeCall(callId: String) {
        if let entry = callsById[callId] {
            cancelTimeout(for: entry)
            callIdByUUID.removeValue(forKey: entry.uuid)
        }
        callsById.removeValue(forKey: callId)
    }

    private func queuePendingEvent(eventName: String, payload: [String: Any]) {
        guard JSONSerialization.isValidJSONObject(payload) else {
            return
        }

        let event: [String: Any] = [
            "eventName": eventName,
            "payload": payload
        ]

        guard let data = try? JSONSerialization.data(withJSONObject: event),
              let string = String(data: data, encoding: .utf8) else {
            return
        }

        var pending = UserDefaults.standard.stringArray(forKey: pendingEventsKey) ?? []
        pending.append(string)
        UserDefaults.standard.set(pending, forKey: pendingEventsKey)
    }

    private func drainPendingEvents() -> [(eventName: String, payload: [String: Any])] {
        let pending = UserDefaults.standard.stringArray(forKey: pendingEventsKey) ?? []
        UserDefaults.standard.removeObject(forKey: pendingEventsKey)

        return pending.compactMap { item in
            guard let data = item.data(using: .utf8),
                  let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let eventName = object["eventName"] as? String,
                  let payload = object["payload"] as? [String: Any] else {
                return nil
            }

            return (eventName: eventName, payload: payload)
        }
    }
}

extension IncomingCallKit: CXProviderDelegate {
    public func providerDidReset(_ provider: CXProvider) {
        callsById.values.forEach { cancelTimeout(for: $0) }
        callsById.removeAll()
        callIdByUUID.removeAll()
    }

    public func provider(_ provider: CXProvider, perform action: CXStartCallAction) {
        if callIdByUUID[action.callUUID] == nil {
            let entry = makeOutgoingCallEntry(for: action)
            callsById[entry.callId] = entry
            callIdByUUID[entry.uuid] = entry.callId
        }

        provider.reportOutgoingCall(with: action.callUUID, startedConnectingAt: Date())
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        guard let callId = callIdByUUID[action.callUUID],
              let entry = callsById[callId] else {
            action.fail()
            return
        }

        entry.state = "accepted"
        cancelTimeout(for: entry)
        emit(
            eventName: "callAccepted",
            payload: makeEventPayload(for: entry, reason: "accepted", source: "user")
        )
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        guard let callId = callIdByUUID[action.callUUID],
              let entry = callsById[callId] else {
            emit(eventName: "callEnded", payload: makeUnknownCallEndedPayload(callUUID: action.callUUID))
            action.fulfill()
            return
        }

        let eventName = entry.state == "accepted" ? "callEnded" : "callDeclined"
        let reason = entry.state == "accepted" ? "ended" : "declined"

        removeCall(callId: callId)
        emit(
            eventName: eventName,
            payload: makeEventPayload(for: entry, reason: reason, source: "user")
        )
        action.fulfill()
    }

    public func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        // CallKit activated the audio session
    }

    public func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        // CallKit deactivated the audio session
    }
}
