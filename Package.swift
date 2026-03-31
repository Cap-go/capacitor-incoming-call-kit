// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapgoCapacitorIncomingCallKit",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapgoCapacitorIncomingCallKit",
            targets: ["IncomingCallKitPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "IncomingCallKitPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/IncomingCallKitPlugin",
            linkerSettings: [
                .linkedFramework("CallKit")
            ]),
        .testTarget(
            name: "IncomingCallKitPluginTests",
            dependencies: ["IncomingCallKitPlugin"],
            path: "ios/Tests/IncomingCallKitPluginTests")
    ]
)
