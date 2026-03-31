import XCTest
@testable import IncomingCallKitPlugin

class IncomingCallKitPluginTests: XCTestCase {
    func testPermissionStatusShape() {
        let status = IncomingCallKit.shared.permissionStatus()
        XCTAssertEqual(status["notifications"] as? String, "notApplicable")
        XCTAssertEqual(status["fullScreenIntent"] as? String, "notApplicable")
    }
}
