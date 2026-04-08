# @capgo/capacitor-incoming-call-kit
 <a href="https://capgo.app/"><img src='https://raw.githubusercontent.com/Cap-go/capgo/main/assets/capgo_banner.png' alt='Capgo - Instant updates for capacitor'/></a>

<div align="center">
  <h2><a href="https://capgo.app/?ref=plugin_incoming_call_kit"> ➡️ Get Instant updates for your App with Capgo</a></h2>
  <h2><a href="https://capgo.app/consulting/?ref=plugin_incoming_call_kit"> Missing a feature? We’ll build the plugin for you 💪</a></h2>
</div>

Display a native incoming-call surface in Capacitor with Android full-screen notifications and iOS CallKit.

This plugin was built for teams who want something in the same space as [olarewajuakeemope/capacitor-incoming-call-kit](https://github.com/olarewajuakeemope/capacitor-incoming-call-kit), but with a small API, typed events, clearer platform boundaries, and documentation you can actually ship from.

## Documentation

The most complete doc is available here: https://capgo.app/docs/plugins/incoming-call-kit/

## Compatibility

| Plugin version | Capacitor compatibility | Maintained |
| -------------- | ----------------------- | ---------- |
| v8.\*.\*       | v8.\*.\*                | ✅          |
| v7.\*.\*       | v7.\*.\*                | On demand   |
| v6.\*.\*       | v6.\*.\*                | ❌          |

> **Note:** The major version of this plugin follows the major version of Capacitor. Use the version that matches your Capacitor installation. Only the latest major version is actively maintained.

## Install

```bash
bun add @capgo/capacitor-incoming-call-kit
bunx cap sync
```

## What This Plugin Does

- Shows a native incoming call UI from JavaScript.
- Emits buffered `callAccepted`, `callDeclined`, `callEnded`, and `callTimedOut` events so the action still reaches JS if the bridge was cold.
- Uses Android notifications plus a full-screen activity.
- Uses iOS CallKit for the system incoming-call sheet.
- Keeps the push transport out of scope so you can wire it to your own FCM, PushKit, SIP, or backend flow.

## What This Plugin Does Not Do

- It does not register FCM, APNs, or PushKit for you.
- It does not create or manage the underlying audio/video session.
- It does not replace your VoIP SDK. It only handles the native incoming-call presentation layer.

## Quick Start

```ts
import { IncomingCallKit } from '@capgo/capacitor-incoming-call-kit';

await IncomingCallKit.requestPermissions();
await IncomingCallKit.requestFullScreenIntentPermission();

await IncomingCallKit.addListener('callAccepted', async ({ call }) => {
  console.log('Accepted:', call.callId, call.extra);
  // Join your voice/video session here.
});

await IncomingCallKit.addListener('callDeclined', ({ call }) => {
  console.log('Declined:', call.callId);
});

await IncomingCallKit.addListener('callTimedOut', ({ call }) => {
  console.log('Timed out:', call.callId);
});

await IncomingCallKit.showIncomingCall({
  callId: 'call-42',
  callerName: 'Ada Lovelace',
  handle: '+39 555 010 020',
  appName: 'Capgo Phone',
  hasVideo: true,
  timeoutMs: 45_000,
  extra: {
    roomId: 'room-42',
    callerUserId: 'user_ada',
  },
  android: {
    channelId: 'calls',
    channelName: 'Incoming Calls',
    showFullScreen: true,
  },
  ios: {
    handleType: 'phoneNumber',
  },
});
```

## Event Model

- `incomingCallDisplayed`: native UI was shown successfully.
- `callAccepted`: the user accepted from the native UI.
- `callDeclined`: the user declined before joining.
- `callEnded`: your app or the platform ended the tracked call.
- `callTimedOut`: the call stayed unanswered until `timeoutMs`.

Each event carries the normalized `call` payload and your original `extra` object.

## Platform Notes

### Android

- `requestPermissions()` requests `POST_NOTIFICATIONS` on Android 13+.
- `requestFullScreenIntentPermission()` opens the Android 14+ settings page for full-screen intents.
- `showIncomingCall()` posts a high-priority notification and can raise a full-screen activity while the app is already running.
- The timeout is best-effort and also uses the notification timeout when available.

### iOS

- `showIncomingCall()` reports the call to CallKit.
- CallKit itself does not require a runtime permission prompt, so `requestPermissions()` resolves immediately on iOS.
- Your app is responsible for starting the real call session after `callAccepted`.
- If you need background incoming-call delivery from VoIP pushes, wire your PushKit/APNs flow to call this plugin as soon as your Capacitor bridge is available.

## Choosing An Architecture

- If your app already uses Twilio, Stream, Daily, or a SIP stack, use this plugin as the native ringing layer and keep the media session in your existing SDK.
- If you only need foreground testing or a custom backend event, calling `showIncomingCall()` directly from JS is enough.
- If you need true background delivery, pair this plugin with your own native push integration instead of baking a push provider into the plugin itself.

## Example App

The repository includes [`example-app`](./example-app) with a small control panel for:

- requesting permissions,
- showing a demo incoming call,
- inspecting active calls,
- and watching listener payloads in real time.

## API

<docgen-index>

* [`showIncomingCall(...)`](#showincomingcall)
* [`endCall(...)`](#endcall)
* [`endAllCalls(...)`](#endallcalls)
* [`getActiveCalls()`](#getactivecalls)
* [`checkPermissions()`](#checkpermissions)
* [`requestPermissions()`](#requestpermissions)
* [`requestFullScreenIntentPermission()`](#requestfullscreenintentpermission)
* [`getPluginVersion()`](#getpluginversion)
* [`addListener('incomingCallDisplayed', ...)`](#addlistenerincomingcalldisplayed-)
* [`addListener('callAccepted', ...)`](#addlistenercallaccepted-)
* [`addListener('callDeclined', ...)`](#addlistenercalldeclined-)
* [`addListener('callEnded', ...)`](#addlistenercallended-)
* [`addListener('callTimedOut', ...)`](#addlistenercalltimedout-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

Capacitor API for presenting a native incoming-call surface.

### showIncomingCall(...)

```typescript
showIncomingCall(options: ShowIncomingCallOptions) => Promise<ShowIncomingCallResult>
```

Displays the native incoming call UI.

Android shows a high-priority notification and can raise a full-screen activity.
iOS reports the call to CallKit.

| Param         | Type                                                                        |
| ------------- | --------------------------------------------------------------------------- |
| **`options`** | <code><a href="#showincomingcalloptions">ShowIncomingCallOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#showincomingcallresult">ShowIncomingCallResult</a>&gt;</code>

--------------------


### endCall(...)

```typescript
endCall(options: EndCallOptions) => Promise<ActiveCallsResult>
```

Ends a specific tracked call.

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#endcalloptions">EndCallOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#activecallsresult">ActiveCallsResult</a>&gt;</code>

--------------------


### endAllCalls(...)

```typescript
endAllCalls(options?: EndAllCallsOptions | undefined) => Promise<ActiveCallsResult>
```

Ends every tracked call.

| Param         | Type                                                              |
| ------------- | ----------------------------------------------------------------- |
| **`options`** | <code><a href="#endallcallsoptions">EndAllCallsOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#activecallsresult">ActiveCallsResult</a>&gt;</code>

--------------------


### getActiveCalls()

```typescript
getActiveCalls() => Promise<ActiveCallsResult>
```

Returns the currently tracked calls.

**Returns:** <code>Promise&lt;<a href="#activecallsresult">ActiveCallsResult</a>&gt;</code>

--------------------


### checkPermissions()

```typescript
checkPermissions() => Promise<IncomingCallPermissionStatus>
```

Returns the current permission state for notifications and full-screen intents.

**Returns:** <code>Promise&lt;<a href="#incomingcallpermissionstatus">IncomingCallPermissionStatus</a>&gt;</code>

--------------------


### requestPermissions()

```typescript
requestPermissions() => Promise<IncomingCallPermissionStatus>
```

Requests the notification permission when the platform supports it.

iOS CallKit itself does not require a runtime prompt, so iOS resolves without prompting.

**Returns:** <code>Promise&lt;<a href="#incomingcallpermissionstatus">IncomingCallPermissionStatus</a>&gt;</code>

--------------------


### requestFullScreenIntentPermission()

```typescript
requestFullScreenIntentPermission() => Promise<IncomingCallPermissionStatus>
```

Opens the Android 14+ full-screen intent settings page when available.

On other platforms this resolves with the current permission status.

**Returns:** <code>Promise&lt;<a href="#incomingcallpermissionstatus">IncomingCallPermissionStatus</a>&gt;</code>

--------------------


### getPluginVersion()

```typescript
getPluginVersion() => Promise<PluginVersionResult>
```

Returns the native implementation version marker.

**Returns:** <code>Promise&lt;<a href="#pluginversionresult">PluginVersionResult</a>&gt;</code>

--------------------


### addListener('incomingCallDisplayed', ...)

```typescript
addListener(eventName: 'incomingCallDisplayed', listenerFunc: (event: IncomingCallEvent) => void) => Promise<PluginListenerHandle>
```

Fired after the call has been handed to the native platform UI.

| Param              | Type                                                                                |
| ------------------ | ----------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'incomingCallDisplayed'</code>                                                |
| **`listenerFunc`** | <code>(event: <a href="#incomingcallevent">IncomingCallEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('callAccepted', ...)

```typescript
addListener(eventName: 'callAccepted', listenerFunc: (event: IncomingCallEvent) => void) => Promise<PluginListenerHandle>
```

Fired when the user accepts the call from native UI.

| Param              | Type                                                                                |
| ------------------ | ----------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'callAccepted'</code>                                                         |
| **`listenerFunc`** | <code>(event: <a href="#incomingcallevent">IncomingCallEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('callDeclined', ...)

```typescript
addListener(eventName: 'callDeclined', listenerFunc: (event: IncomingCallEvent) => void) => Promise<PluginListenerHandle>
```

Fired when the user declines the call from native UI.

| Param              | Type                                                                                |
| ------------------ | ----------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'callDeclined'</code>                                                         |
| **`listenerFunc`** | <code>(event: <a href="#incomingcallevent">IncomingCallEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('callEnded', ...)

```typescript
addListener(eventName: 'callEnded', listenerFunc: (event: IncomingCallEvent) => void) => Promise<PluginListenerHandle>
```

Fired when a call ends through the API or a platform action.

| Param              | Type                                                                                |
| ------------------ | ----------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'callEnded'</code>                                                            |
| **`listenerFunc`** | <code>(event: <a href="#incomingcallevent">IncomingCallEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('callTimedOut', ...)

```typescript
addListener(eventName: 'callTimedOut', listenerFunc: (event: IncomingCallEvent) => void) => Promise<PluginListenerHandle>
```

Fired when an unanswered call reaches its configured timeout.

| Param              | Type                                                                                |
| ------------------ | ----------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'callTimedOut'</code>                                                         |
| **`listenerFunc`** | <code>(event: <a href="#incomingcallevent">IncomingCallEvent</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

Removes every native listener registered by the plugin.

--------------------


### Interfaces


#### ShowIncomingCallResult

Result payload for `showIncomingCall()`.

| Prop       | Type                                                              | Description                                 |
| ---------- | ----------------------------------------------------------------- | ------------------------------------------- |
| **`call`** | <code><a href="#incomingcallrecord">IncomingCallRecord</a></code> | The call record that was created or reused. |


#### IncomingCallRecord

Represents a currently tracked call.

| Prop             | Type                                                            | Description                                            |
| ---------------- | --------------------------------------------------------------- | ------------------------------------------------------ |
| **`callId`**     | <code>string</code>                                             | Stable call identifier provided by your app.           |
| **`callerName`** | <code>string</code>                                             | Name shown in the native UI.                           |
| **`handle`**     | <code>string</code>                                             | Secondary handle shown by the platform when available. |
| **`hasVideo`**   | <code>boolean</code>                                            | Whether this call should be treated as a video call.   |
| **`state`**      | <code><a href="#incomingcallstate">IncomingCallState</a></code> | Current platform-reported call state.                  |
| **`platform`**   | <code>'android' \| 'ios' \| 'web'</code>                        | Platform that produced the record.                     |
| **`extra`**      | <code><a href="#record">Record</a>&lt;string, any&gt;</code>    | Arbitrary metadata passed in at call creation time.    |


#### ShowIncomingCallOptions

Common options used to present an incoming call.

| Prop              | Type                                                                              | Description                                                                                               |
| ----------------- | --------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| **`callId`**      | <code>string</code>                                                               | Stable identifier for the call. Reuse the same value when ending the call later.                          |
| **`callerName`**  | <code>string</code>                                                               | Primary name shown to the user.                                                                           |
| **`handle`**      | <code>string</code>                                                               | Optional secondary handle such as a phone number, SIP URI, or user ID.                                    |
| **`appName`**     | <code>string</code>                                                               | Label shown by Android in notifications. iOS uses the app display name configured in the host app bundle. |
| **`hasVideo`**    | <code>boolean</code>                                                              | Whether the incoming session should be marked as video-capable.                                           |
| **`timeoutMs`**   | <code>number</code>                                                               | Best-effort timeout in milliseconds. Defaults to `60000`.                                                 |
| **`acceptText`**  | <code>string</code>                                                               | Custom label for the accept action. Android only.                                                         |
| **`declineText`** | <code>string</code>                                                               | Custom label for the decline action. Android only.                                                        |
| **`extra`**       | <code><a href="#record">Record</a>&lt;string, any&gt;</code>                      | Arbitrary JSON metadata echoed back in all events.                                                        |
| **`android`**     | <code><a href="#androidincomingcalloptions">AndroidIncomingCallOptions</a></code> | Android-specific behavior overrides.                                                                      |
| **`ios`**         | <code><a href="#iosincomingcalloptions">IOSIncomingCallOptions</a></code>         | iOS-specific behavior overrides.                                                                          |


#### AndroidIncomingCallOptions

Android-specific incoming call presentation options.

| Prop                 | Type                 | Description                                                                                |
| -------------------- | -------------------- | ------------------------------------------------------------------------------------------ |
| **`channelId`**      | <code>string</code>  | Notification channel identifier. Defaults to `incoming_call_kit`.                          |
| **`channelName`**    | <code>string</code>  | Notification channel display name. Defaults to `Incoming Calls`.                           |
| **`showFullScreen`** | <code>boolean</code> | Whether Android should request full-screen presentation when possible. Defaults to `true`. |
| **`accentColor`**    | <code>string</code>  | Optional accent color in `#RRGGBB` or `#AARRGGBB` form.                                    |
| **`ringtoneUri`**    | <code>string</code>  | Optional ringtone URI string. Example: `android.resource://com.example.app/raw/ringtone`   |
| **`isHighPriority`** | <code>boolean</code> | Whether to mark the notification as high priority. Defaults to `true`.                     |


#### IOSIncomingCallOptions

iOS-specific incoming call presentation options.

| Prop                     | Type                                                      | Description                                                      |
| ------------------------ | --------------------------------------------------------- | ---------------------------------------------------------------- |
| **`handleType`**         | <code>'generic' \| 'phoneNumber' \| 'emailAddress'</code> | CallKit handle type. Defaults to `generic`.                      |
| **`supportsHolding`**    | <code>boolean</code>                                      | Whether the call should support hold. Defaults to `true`.        |
| **`supportsDTMF`**       | <code>boolean</code>                                      | Whether the call should support DTMF. Defaults to `false`.       |
| **`supportsGrouping`**   | <code>boolean</code>                                      | Whether the call should support grouping. Defaults to `false`.   |
| **`supportsUngrouping`** | <code>boolean</code>                                      | Whether the call should support ungrouping. Defaults to `false`. |


#### ActiveCallsResult

Result payload for `getActiveCalls()`.

| Prop        | Type                              | Description                                       |
| ----------- | --------------------------------- | ------------------------------------------------- |
| **`calls`** | <code>IncomingCallRecord[]</code> | Calls still tracked by the native implementation. |


#### EndCallOptions

Options for ending a single tracked call.

| Prop         | Type                | Description                                                    |
| ------------ | ------------------- | -------------------------------------------------------------- |
| **`callId`** | <code>string</code> | The call identifier originally passed to `showIncomingCall()`. |
| **`reason`** | <code>string</code> | Optional application-defined reason string.                    |


#### EndAllCallsOptions

Options for ending all tracked calls.

| Prop         | Type                | Description                                 |
| ------------ | ------------------- | ------------------------------------------- |
| **`reason`** | <code>string</code> | Optional application-defined reason string. |


#### IncomingCallPermissionStatus

Result payload for permission checks.

| Prop                   | Type                                                                           | Description                                                                                                                         |
| ---------------------- | ------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| **`notifications`**    | <code><a href="#permissionstate">PermissionState</a> \| 'notApplicable'</code> | Notification permission state. iOS CallKit itself does not require runtime notification permission, so iOS returns `notApplicable`. |
| **`fullScreenIntent`** | <code><a href="#permissionstate">PermissionState</a> \| 'notApplicable'</code> | Full-screen intent permission state. iOS returns `notApplicable`. Android 13 and below resolve this as `granted`.                   |


#### PluginVersionResult

Plugin version payload.

| Prop          | Type                | Description                                                 |
| ------------- | ------------------- | ----------------------------------------------------------- |
| **`version`** | <code>string</code> | Version identifier returned by the platform implementation. |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### IncomingCallEvent

Payload delivered by plugin listeners.

| Prop         | Type                                                              | Description                               |
| ------------ | ----------------------------------------------------------------- | ----------------------------------------- |
| **`call`**   | <code><a href="#incomingcallrecord">IncomingCallRecord</a></code> | The call that triggered the event.        |
| **`reason`** | <code>string</code>                                               | Optional reason for the state transition. |
| **`source`** | <code>'api' \| 'user' \| 'system'</code>                          | Origin of the action.                     |


### Type Aliases


#### IncomingCallState

Supported incoming call states.

<code>'ringing' | 'accepted' | 'ended'</code>


#### Record

Construct a type with a set of properties K of type T

<code>{ [P in K]: T; }</code>


#### PermissionState

<code>'prompt' | 'prompt-with-rationale' | 'granted' | 'denied'</code>

</docgen-api>
